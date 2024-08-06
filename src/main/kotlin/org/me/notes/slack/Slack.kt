import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.me.notes.NotesStorage
import org.me.notes.editor.NotesHighlightingPassFactory

private const val token = ""
private const val botUserId = "D079BF0D4BY"
private val logger = Logger.getInstance(NotesHighlightingPassFactory::class.java)

private fun postMessage(message: String) {
    val client = HttpClient(CIO.create())
    runBlocking {
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
}

fun postNotesIntoSlackBot(project: Project) {
    val message = NotesStorage.getInstance(project).state.notes.entries.joinToString("\n") { (_, notes) ->
        notes.joinToString("\n") { note ->
            return@joinToString ":heart: *${note.text}*\n" +
                    "<${note.virtualFile.path}|${note.virtualFile.name}> \n" +
                    "```${note.code}```"
        }
    }
    postMessage(message)
}
