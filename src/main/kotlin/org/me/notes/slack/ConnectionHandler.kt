package org.me.notes.slack

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import fi.iki.elonen.NanoHTTPD
import kotlin.io.path.Path

class ConnectionHandler(val project: Project, port: Int) : NanoHTTPD("127.0.0.1", port), Disposable {
    companion object {
        val logger = Logger.getInstance(ConnectionHandler::class.java)
    }

    override fun serve(session: IHTTPSession): Response {
        val file = session.parms["≈file"]
        if (file == null) {
            logger.info("file $file was not found")
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "file $file was not found")
        }

        val virtualFile = VfsUtil.findFile(Path(file), false)
        if (virtualFile == null) {
            logger.info("virtual file $file was not found")
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "virtual file $file was not found")
        }

        val document = ReadAction.nonBlocking<Document> { FileDocumentManager.getInstance().getDocument(virtualFile) }
            .executeSynchronously()
        if (document == null) {
            logger.info("document for file $file was not found")
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "document for file $file was not found"
            )
        }

        val offset = session.parms["line"]?.trim(':')?.toInt()?.let { document.getLineStartOffset(it) } ?: 0

        ApplicationManager.getApplication().invokeLater {
            FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, virtualFile, offset), true)
            ProjectUtil.focusProjectWindow(project, true)
        }
        return super.serve(session)
    }

    override fun dispose() {
        closeAllConnections()
    }
}

fun startServer(project: Project) {
    var server: ConnectionHandler? = null
    (3001..3009).forEach {
        val localServer = ConnectionHandler(project, it)
        try {
            localServer.start()
            server = localServer
            return@forEach
        } catch (e: Exception) {
            ConnectionHandler.logger.info("port $it doesn't work: ${e.message}")
        }
    }
    if (server == null) {
        ConnectionHandler.logger.error("unable to start server")
    }
}