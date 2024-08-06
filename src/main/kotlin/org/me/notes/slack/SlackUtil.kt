package org.me.notes.slack

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.me.notes.NotesStorage
import org.me.notes.editor.NotesHighlightingPassFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val token = ""
private const val botUserId = "D079BF0D4BY"
private val logger = Logger.getInstance(NotesHighlightingPassFactory::class.java)
private val coroutineScope = CoroutineScope(Dispatchers.Default)
const val SLACK_ICON = "/icons/slack.svg"

private suspend fun postMessage(message: String) {
    val client = HttpClient(CIO.create())
    try {
        val response: HttpResponse = client.post("https://slack.com/api/chat.postMessage") {
            headers {
                append("Authorization", "Bearer $token")
                append("Content-Type", "application/json")
            }
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("channel", botUserId)
                        append("text", message)
                    }
                )
            )
        }
        if (response.status != HttpStatusCode.OK) {
            logger.error("Unable to send message: ${response.bodyAsText()}")
        }
    } catch (e: Exception) {
        logger.error("Exception during sending message: ${e.message}")
    } finally {
        client.close()
    }
}

fun postNotesIntoSlackBot(project: Project, invokeOnCompletion: () -> Unit) {
    coroutineScope.launch {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val currentTime = LocalDateTime.now().format(formatter)

        val notes = NotesStorage.getInstance(project).state.notes.entries.flatMap { it.value }.filter {
            it.time.endsWith(currentTime)
        }
        if (notes.isNotEmpty()) {
            val title = "*Your notes for $currentTime in ${project.name}* :eyes:"
            val message = notes.asFlow().map { it.getMessage() }.toList().joinToString("\n")
            postMessage("$title\n$message")
        }
    }.invokeOnCompletion {
        invokeOnCompletion()
    }
}
