import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import fi.iki.elonen.NanoHTTPD
import kotlin.io.path.Path

class ConnectionHandler(val project: Project, port: Int) : NanoHTTPD("127.0.0.1", port), Disposable {
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(ConnectionHandler::class.java)
    }

    override fun serve(session: IHTTPSession): Response {
        val file = session.parms["file"]
        if (file == null) logger.error("file was not found")
        val line = session.parms["line"]?.trim(':') ?: ""
        val virtualFile = VfsUtil.findFile(Path(file!!), false)
        if (virtualFile == null) logger.error("virtual file was not found")
        val offset = FileDocumentManager.getInstance().getDocument(virtualFile!!)!!.getLineStartOffset(line.toInt())
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
        }
        catch (e: Exception) {
            ConnectionHandler.thisLogger().info("port $it doesn't work: ${e.message}")
        }
    }
    if (server == null) {
        ConnectionHandler.thisLogger().error("unable to start server")
    }
}