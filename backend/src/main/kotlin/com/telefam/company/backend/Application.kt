package com.telefam.company.backend

import com.telefam.company.backend.db.TelefamDatabase
import com.telefam.company.backend.routes.registerTelefamRoutes
import com.telefam.company.backend.security.SandboxImageProcessor
import com.telefam.company.backend.security.SecurityRiskEngine
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private data class DatabaseConnection(
    val jdbcUrl: String,
    val username: String?,
    val password: String?
)

private fun databaseConnectionFromEnv(env: io.github.cdimascio.dotenv.Dotenv): DatabaseConnection {
    val rawUrl = env["POSTGRES_JDBC_URL"] ?: env["DATABASE_URL"]
    if (rawUrl.isNullOrBlank()) {
        return DatabaseConnection(
            jdbcUrl = "jdbc:postgresql://db.telefam.company:5432/telefam_production",
            username = env["POSTGRES_USER"],
            password = env["POSTGRES_PASSWORD"]
        )
    }
    if (rawUrl.startsWith("jdbc:", ignoreCase = true)) {
        return DatabaseConnection(rawUrl, env["POSTGRES_USER"], env["POSTGRES_PASSWORD"])
    }
    val uri = URI(rawUrl)
    val userInfo = uri.userInfo?.split(":", limit = 2)
    val username = userInfo?.getOrNull(0)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
    val password = userInfo?.getOrNull(1)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
    val database = uri.path.orEmpty().trimStart('/').ifBlank { "railway" }
    val port = if (uri.port > 0) uri.port else 5432
    return DatabaseConnection(
        jdbcUrl = "jdbc:postgresql://${uri.host}:$port/$database?sslmode=require",
        username = env["POSTGRES_USER"] ?: username,
        password = env["POSTGRES_PASSWORD"] ?: password
    )
}

fun main() {
    val env = dotenv {
        ignoreIfMissing = true
    }
    val host = env["SERVER_HOST"] ?: "0.0.0.0"
    val port = (env["SERVER_PORT"] ?: "8080").toInt()

    embeddedServer(Netty, host = host, port = port) {
        telefamBackendModule(env)
    }.start(wait = true)
}

fun Application.telefamBackendModule(env: io.github.cdimascio.dotenv.Dotenv) {
    val database = databaseConnectionFromEnv(env)
    TelefamDatabase.init(
        jdbcUrl = database.jdbcUrl,
        dbUser = database.username ?: "telefam_core_rw",
        dbPassword = database.password ?: "",
        maxPoolSize = (env["POSTGRES_MAX_POOL_SIZE"] ?: "5").toInt()
    )

    val riskEngine = SecurityRiskEngine(
        serverPepper = env["SERVER_FINGERPRINT_PEPPER"] ?: "telefam_prod_pepper_key",
        sessionMasterKeyHex = env["SESSION_AES_GCM_MASTER_KEY"] ?: "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    )

    val sandboxProcessor = SandboxImageProcessor(
        sandboxDir = File(env["SANDBOX_WORK_DIR"] ?: "/var/telefam/sandbox/quarantine"),
        secureStorageDir = File(env["SECURE_STORAGE_DIR"] ?: "/var/telefam/storage/verified_avatars"),
        maxUploadBytes = (env["MAX_UPLOAD_BYTES"] ?: "5242880").toLong()
    )

    install(XForwardedHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = false
            }
        )
    }
    install(CORS) {
        allowHost("telefam.company", schemes = listOf("https"))
        allowHost("api.telefam.company", schemes = listOf("https"))
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Telefam-Device-Fingerprint")
        allowHeader("X-Telefam-Client-Timestamp")
        allowHeader("X-Telefam-Location")
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("status" to "ERROR", "message" to (cause.message ?: "Invalid request parameter"))
            )
        }
        exception<SecurityException> { call, cause ->
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf("status" to "SECURITY_BLOCK", "message" to (cause.message ?: "Request blocked by security policy"))
            )
        }
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }

    registerTelefamRoutes(riskEngine, sandboxProcessor)
}
