package org.me.notes.slack

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ImageLoader
import com.intellij.util.queryParameters
import com.intellij.util.ui.JBImageIcon
import com.slack.api.Slack
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsParameters
import com.sun.net.httpserver.HttpsServer
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
import okhttp3.HttpUrl
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.me.notes.editor.NotesHighlightingPassFactory
import org.me.notes.storage.NotesStorage
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.Exchanger
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLParameters
import javax.swing.Icon

private const val botUserId = "D079BF0D4BY"
private val logger = Logger.getInstance(NotesHighlightingPassFactory::class.java)
private val coroutineScope = CoroutineScope(Dispatchers.Default)
val code = Exchanger<String>()

private suspend fun postMessage(message: String, token: String) {
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
        if (response.status != HttpStatusCode.OK || response.bodyAsText().contains("\"ok\":false")) {
            logger.error("Unable to send message: ${response.bodyAsText()}")
        }
        logger.debug("Message posted successfully: $message")
    } catch (e: Exception) {
        logger.error("Exception during sending message: ${e.message}")
    } finally {
        client.close()
    }
}

fun postNotesIntoSlackBot(project: Project, invokeOnCompletion: () -> Unit) {
    coroutineScope.launch {
        var token = SlackCredentialsHolder.getInstance().getToken()
        if (token == null) {
            token = exchangeCodeForToken()
            if (token == null) {
                logger.error("Unable to exchange code for token")
                return@launch
            }
            SlackCredentialsHolder.getInstance().setToken(token)
        }
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val currentTime = LocalDateTime.now().format(formatter)

        val notes = NotesStorage.getInstance(project).notes.entries
            .flatMap { it.value }
            .filter { it.time.endsWith(currentTime) }
            .sortedBy { it.time }

        if (notes.isEmpty()) return@launch

        val title = "*Your notes for $currentTime in ${project.name}* :eyes:"
        val message = notes.asFlow().map { it.getSlackMessage() }.toList().joinToString("\n")

        postMessage("$title\n$message", token)
    }.invokeOnCompletion {
        invokeOnCompletion()
    }
}

private fun exchangeCodeForToken() : String? {
    val clientId = SlackCredentialsHolder.getInstance().getClientId()
    if (clientId == null) {
        logger.error("Unable to get clientId")
        return null
    }

    val clientSecret = SlackCredentialsHolder.getInstance().getClientSecret()
    if (clientSecret == null) {
        logger.error("Unable to get clientSecret")
        return null
    }

    val redirectLink = "https://localhost:3000/notes/auth"

    val scopeBot = listOf(
        "chat:write",
        "chat:write.public",
        "files:read",
        "files:write",
        "incoming-webhook",
        "pins:write",
        "remote_files:read",
        "remote_files:share",
        "remote_files:write"
    )

    val uri = HttpUrl.Builder()
        .scheme("https")
        .host("slack.com")
        .addPathSegment("oauth")
        .addPathSegment("v2")
        .addPathSegment("authorize")
        .addQueryParameter("scope", scopeBot.joinToString(" "))
        .addQueryParameter("client_id", clientId)
        .addQueryParameter("state", UUID.randomUUID().toString())
        .addQueryParameter("redirect_uri", redirectLink)
        .build()
        .toUrl()
        .toURI()

    val server = startServer()

    try {
        BrowserUtil.browse(uri)
        val code = code.exchange(null)
        val oauthRequest = OAuthV2AccessRequest.builder()
            .redirectUri(redirectLink)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .code(code)
            .build()
        return Slack.getInstance().methods().oauthV2Access(oauthRequest).accessToken
    }
    catch (e: Exception) {
        logger.error("Error during authorization: ${e.message}")
    }
    finally {
        server?.stop(0)
    }
    return null
}

fun startServer(): HttpsServer? {
    val localhostCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName("localhost")
        .duration(10 * 365, TimeUnit.DAYS)
        .build()

    val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()

    val sslContext = serverCertificates.sslContext()
    val httpsServer = HttpsServer.create(InetSocketAddress(3000), 0)

    httpsServer.httpsConfigurator = object : HttpsConfigurator(sslContext) {
        override fun configure(params: HttpsParameters) {
            try {
                val sslEngine: SSLEngine = sslContext.createSSLEngine()
                params.needClientAuth = false
                params.cipherSuites = sslEngine.enabledCipherSuites
                params.protocols = sslEngine.enabledProtocols

                val sslParameters: SSLParameters = sslContext.supportedSSLParameters
                params.setSSLParameters(sslParameters)
            } catch (e: Exception) {
                logger.error("Can't set up SSL: ${e.message}")
            }
        }
    }

    httpsServer.createContext("/notes/auth") {
        code.exchange(it.requestURI.queryParameters["code"].toString())
    }
    httpsServer.executor = null
    httpsServer.start()
    return httpsServer
}

fun createIcon(path: String): Icon {
    val imgURL = NotesStorage::class.java.getResource(path) ?: return AllIcons.General.Information
    val image = ImageLoader.loadFromUrl(imgURL) ?: return AllIcons.General.Information
    return JBImageIcon(ImageLoader.scaleImage(image, 18, 18))
}