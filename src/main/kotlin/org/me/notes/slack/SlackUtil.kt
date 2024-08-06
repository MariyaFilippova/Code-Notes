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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.me.notes.NotesStorage
import org.me.notes.editor.NotesHighlightingPassFactory
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val token = ""
private const val botUserId = "D079BF0D4BY"
private val logger = Logger.getInstance(NotesHighlightingPassFactory::class.java)
private val coroutineScope = CoroutineScope(Dispatchers.Default)

private suspend fun postMessage(message: String): String? {
    val client = HttpClient(CIO.create())
    try {
        val response: HttpResponse = client.post("https://slack.com/api/chat.scheduleMessage") {
            headers {
                append("Authorization", "Bearer $token")
            }
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("channel", botUserId)
                        append("text", message)
                        val now = LocalDateTime.now()
                        val date = LocalDateTime.of(now.year, now.month, now.dayOfMonth, 18, 49)
                            .toEpochSecond(ZonedDateTime.now().offset)
                        append("post_at", date.toString())
                    }
                )
            )
        }
        val id = Json.parseToJsonElement(response.bodyAsText()).jsonObject["scheduled_message_id"]
        if (id is JsonPrimitive) return id.content
        if (response.status != HttpStatusCode.OK) {
            logger.error("Unable to send message: ${response.bodyAsText()}")
        } else {
            logger.info("Message scheduled successfully")
        }
    } catch (e: Exception) {
        logger.error("Exception during sending message: ${e.message}")
    } finally {
        client.close()
    }
    return null
}

fun rescheduleSlackMessage(project: Project) {
    coroutineScope.launch {
        deleteScheduledMessage(project)
        scheduleMessage(project)
    }
}

private suspend fun deleteScheduledMessage(project: Project) {
    val id = NotesStorage.getInstance(project).state.messageId ?: return
    val client = HttpClient(CIO.create())
    try {
        val response: HttpResponse = client.post("https://slack-gov.com/api/chat.deleteScheduledMessage") {
            headers {
                append("Authorization", "Bearer $token")
                append("Content-Type", "application/json; charset=UTF-8")
            }
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("channel", botUserId)
                        append("scheduled_message_id", id)
                    }
                )
            )
        }
        print(response.bodyAsText())
        if (response.bodyAsText().contains("\"ok\" : false") || response.status != HttpStatusCode.OK) {
            logger.error("Unable to delete message: ${response.bodyAsText()}")
        } else {
            logger.info("Message deleted successfully")
        }
    } catch (e: Exception) {
        logger.error("Exception during deleting scheduled message: ${e.message}")
    } finally {
        client.close()
    }
}

private suspend fun scheduleMessage(project: Project) {
    val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

    val notes = NotesStorage.getInstance(project).state.notes.entries.flatMap { it.value }.filter {
        it.time.endsWith(today)
    }
    if (notes.isEmpty()) {
        NotesStorage.getInstance(project).state.messageId = null
        return
    }
    val title = "*Your notes for $today in ${project.name}* :eyes:"
    val message = notes.asFlow().map { it.getMessage() }.toList().joinToString("\n")
    NotesStorage.getInstance(project).state.messageId = postMessage("$title\n$message")
}
