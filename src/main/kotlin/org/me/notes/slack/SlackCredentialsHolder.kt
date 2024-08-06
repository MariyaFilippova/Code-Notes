package org.me.notes.slack

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val myToken = "token_notes"
private const val myClientId = "clientId"
private const val myClientSecret = "clientSecret"

@Service(Service.Level.APP)
class SlackCredentialsHolder {
    companion object {
        fun getInstance() = ApplicationManager.getApplication().getService(SlackCredentialsHolder::class.java)
        private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(SlackCredentialsHolder::class.java)
    }

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName("NotesSlack", key))
    }

    fun getToken(): String? {
        return getCredentials(myToken)
    }

    fun getClientId(): String? {
        return getCredentials(myClientId)
    }

    fun getClientSecret(): String? {
        return getCredentials(myClientSecret)
    }

    fun setClientSecrets() {
        val file = SlackCredentialsHolder::class.java.getResource("/secret.json")
        if (file == null) {
            logger.error("Can't get secret file")
            return
        }
        try {
            val json = Json.parseToJsonElement(file.readText()) as? JsonObject
            if (json == null) {
                logger.error("Can't parse json")
            }
            val clientId = json?.get("clientId")?.jsonPrimitive?.content
            val clientSecret = json?.get("clientSecret")?.jsonPrimitive?.content
            if (clientId == null || clientSecret == null) {
                logger.error("Can't get client id or client secret")
            }
            setClientId(clientId!!)
            setClientSecret(clientSecret!!)
        }
        catch (e: Exception) {
            logger.error("Can't get client id or client secret: ${e.message}")
        }
    }

    private fun setClientId(clientId: String) {
        val credentials = Credentials("clientId-slack-notes", clientId)
        val attributes = createCredentialAttributes(myClientId)
        PasswordSafe.instance.set(attributes, credentials)
    }

    private fun setClientSecret(clientSecret: String) {
        val credentials = Credentials("clientSecret-slack-notes", clientSecret)
        val attributes = createCredentialAttributes(myClientSecret)
        PasswordSafe.instance.set(attributes, credentials)
    }

    fun setToken(token: String) {
        val credentials = Credentials("token-slack-notes", token)
        val attributes = createCredentialAttributes(myToken)
        PasswordSafe.instance.set(attributes, credentials)
    }

    private fun getCredentials(key: String): String? {
        val attributes = createCredentialAttributes(key)
        val credentials = PasswordSafe.instance.get(attributes)

        PasswordSafe.instance.set(attributes, credentials);
        return credentials?.getPasswordAsString() ?: System.getenv(key)
    }
}