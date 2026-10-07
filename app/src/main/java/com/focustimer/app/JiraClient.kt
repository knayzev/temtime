package com.focustimer.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64

/** What a refresh produced: either issues, or something to show the user. */
sealed class JiraResult {
    data class Ok(val issues: List<JiraIssue>) : JiraResult()
    data class Failed(val message: String) : JiraResult()
}

/**
 * Reads issues from Jira Cloud with an Atlassian API token over Basic auth. Deliberately small:
 * one GET, no library, matching how the Telegram and ElevenLabs calls are already made here.
 */
object JiraClient {

    private const val MAX_ISSUES = 25

    suspend fun fetch(
        site: String,
        email: String,
        token: String,
        jql: String
    ): JiraResult = withContext(Dispatchers.IO) {
        if (site.isBlank() || email.isBlank() || token.isBlank()) {
            return@withContext JiraResult.Failed("Заполните адрес, почту и токен")
        }

        var connection: HttpURLConnection? = null
        try {
            // The old /rest/api/3/search was retired; this is its replacement.
            val url = URL(
                "https://$site/rest/api/3/search/jql" +
                    "?jql=" + URLEncoder.encode(jql, "UTF-8") +
                    "&fields=summary,status" +
                    "&maxResults=$MAX_ISSUES"
            )
            val auth = Base64.getEncoder().encodeToString("$email:$token".toByteArray(Charsets.UTF_8))
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Basic $auth")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 20000
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                return@withContext JiraResult.Failed(describe(code, detail))
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONObject(body).optJSONArray("issues")
                ?: return@withContext JiraResult.Ok(emptyList())
            val issues = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val fields = o.optJSONObject("fields")
                JiraIssue(
                    key = o.optString("key", ""),
                    summary = fields?.optString("summary", "").orEmpty(),
                    status = fields?.optJSONObject("status")?.optString("name", "").orEmpty()
                )
            }.filter { it.key.isNotBlank() }
            JiraResult.Ok(issues)
        } catch (e: Exception) {
            JiraResult.Failed(e.message ?: "Не удалось связаться с Jira")
        } finally {
            connection?.disconnect()
        }
    }

    /** Jira answers a bad JQL with 400 and a body worth showing, so it is passed through. */
    private fun describe(code: Int, detail: String): String {
        val reason = try {
            val obj = JSONObject(detail)
            val messages = obj.optJSONArray("errorMessages")
            if (messages != null && messages.length() > 0) messages.optString(0) else null
        } catch (_: Exception) {
            null
        }
        return when {
            reason != null -> reason
            code == 401 -> "Неверная почта или токен"
            code == 403 -> "Доступ запрещён"
            code == 404 -> "Адрес сайта не найден"
            else -> "Jira ответила $code"
        }
    }
}
