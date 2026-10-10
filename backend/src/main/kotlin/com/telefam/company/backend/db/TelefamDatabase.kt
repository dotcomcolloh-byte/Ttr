package com.telefam.company.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Parameterized JDBC + HikariCP Data Access Layer for Telefam Production PostgreSQL.
 * Strictly uses bound PreparedStatements and SQL parameter types to prevent SQL injection.
 */
object TelefamDatabase {
    private var dataSource: HikariDataSource? = null

    fun init(jdbcUrl: String, dbUser: String, dbPassword: String, maxPoolSize: Int) {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbUser
            this.password = dbPassword
            this.maximumPoolSize = maxPoolSize
            this.driverClassName = "org.postgresql.Driver"
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            addDataSourceProperty("ApplicationName", "TelefamKtorBackend")
            addDataSourceProperty("reWriteBatchedInserts", "true")
        }
        dataSource = HikariDataSource(config)
    }

    fun <T> withTransaction(block: (Connection) -> T): T {
        val ds = requireNotNull(dataSource) { "Telefam PostgreSQL DataSource not initialized" }
        ds.connection.use { conn ->
            try {
                val result = block(conn)
                conn.commit()
                return result
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            }
        }
    }

    fun invalidateExpiredOtpsAndSessions(conn: Connection, now: Instant) {
        val nowTs = Timestamp.from(now)
        conn.prepareStatement(
            """
            UPDATE telefam_otp_challenges
            SET is_invalidated = TRUE
            WHERE is_invalidated = FALSE AND expires_at <= ?
            """.trimIndent()
        ).use { ps ->
            ps.setTimestamp(1, nowTs)
            ps.executeUpdate()
        }

        conn.prepareStatement(
            """
            UPDATE telefam_encrypted_sessions
            SET is_revoked = TRUE
            WHERE is_revoked = FALSE AND expires_at <= ?
            """.trimIndent()
        ).use { ps ->
            ps.setTimestamp(1, nowTs)
            ps.executeUpdate()
        }
    }

    fun insertForensicAuditLog(
        conn: Connection,
        category: String,
        outcome: String,
        userId: UUID?,
        targetIdentifier: String?,
        ipSubnetFp: String,
        untrustedClientDeviceFp: String,
        serverVerifiedDeviceHash: String,
        geoLocationLabel: String?,
        geoLat: Double?,
        geoLon: Double?,
        riskScore: Int,
        riskSignalsJson: String,
        requestPath: String,
        httpMethod: String,
        userAgent: String?,
        metadataJson: String
    ) {
        val sql = """
            INSERT INTO telefam_forensic_audit_logs (
                event_category, event_outcome, user_id, target_identifier,
                ip_subnet_fingerprint, untrusted_client_device_fp, server_verified_device_hash,
                geo_location_label, geo_latitude, geo_longitude,
                computed_risk_score, risk_signals, request_path, http_method,
                user_agent, forensic_metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb)
        """.trimIndent()
        conn.prepareStatement(sql).use { ps: PreparedStatement ->
            ps.setString(1, category)
            ps.setString(2, outcome)
            ps.setObject(3, userId)
            ps.setString(4, targetIdentifier)
            ps.setString(5, ipSubnetFp)
            ps.setString(6, untrustedClientDeviceFp)
            ps.setString(7, serverVerifiedDeviceHash)
            ps.setString(8, geoLocationLabel)
            if (geoLat != null) ps.setDouble(9, geoLat) else ps.setNull(9, java.sql.Types.DOUBLE)
            if (geoLon != null) ps.setDouble(10, geoLon) else ps.setNull(10, java.sql.Types.DOUBLE)
            ps.setInt(11, riskScore)
            ps.setString(12, riskSignalsJson)
            ps.setString(13, requestPath)
            ps.setString(14, httpMethod)
            ps.setString(15, userAgent)
            ps.setString(16, metadataJson)
            ps.executeUpdate()
        }
    }

    /**
     * Executes a database block inside an explicit RLS-isolated transaction where
     * `app.current_user_id` is bound via parameterized `set_config` so PostgreSQL Row-Level Security
     * policies strictly isolate rows to the authenticated user.
     */
    fun <T> withRlsUserContext(userId: UUID, block: (Connection) -> T): T {
        val ds = checkNotNull(dataSource) { "Database pool is not initialized" }
        ds.connection.use { conn ->
            val oldAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                conn.prepareStatement("SELECT set_config('app.current_user_id', ?, true)").use { ps ->
                    ps.setString(1, userId.toString())
                    ps.executeQuery()
                }
                val result = block(conn)
                conn.commit()
                return result
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = oldAutoCommit
            }
        }
    }

    fun upsertPrivacySettingsWithRls(
        userId: UUID,
        messageRequests: String,
        whoCanCallMe: String,
        whoCanScreenshotChats: String,
        whoCanShareChats: String,
        whoCanCopyMessages: String,
        whoCanDownloadMedia: String,
        chatThemeId: String,
        bubbleColorHex: String,
        readReceipts: String,
        typingIndicators: String,
        onlineIndicators: String,
        syncVersion: Long
    ) {
        withRlsUserContext(userId) { conn ->
            val sql = """
                INSERT INTO telefam_user_privacy_settings (
                    user_id, message_requests, who_can_call_me, who_can_screenshot_chats,
                    who_can_share_chats, who_can_copy_messages, who_can_download_media,
                    chat_theme_id, bubble_color_hex, read_receipts, typing_indicators,
                    online_indicators, sync_version, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (user_id) DO UPDATE SET
                    message_requests = EXCLUDED.message_requests,
                    who_can_call_me = EXCLUDED.who_can_call_me,
                    who_can_screenshot_chats = EXCLUDED.who_can_screenshot_chats,
                    who_can_share_chats = EXCLUDED.who_can_share_chats,
                    who_can_copy_messages = EXCLUDED.who_can_copy_messages,
                    who_can_download_media = EXCLUDED.who_can_download_media,
                    chat_theme_id = EXCLUDED.chat_theme_id,
                    bubble_color_hex = EXCLUDED.bubble_color_hex,
                    read_receipts = EXCLUDED.read_receipts,
                    typing_indicators = EXCLUDED.typing_indicators,
                    online_indicators = EXCLUDED.online_indicators,
                    sync_version = EXCLUDED.sync_version,
                    updated_at = NOW()
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setObject(1, userId)
                ps.setString(2, messageRequests)
                ps.setString(3, whoCanCallMe)
                ps.setString(4, whoCanScreenshotChats)
                ps.setString(5, whoCanShareChats)
                ps.setString(6, whoCanCopyMessages)
                ps.setString(7, whoCanDownloadMedia)
                ps.setString(8, chatThemeId)
                ps.setString(9, bubbleColorHex)
                ps.setString(10, readReceipts)
                ps.setString(11, typingIndicators)
                ps.setString(12, onlineIndicators)
                ps.setLong(13, syncVersion)
                ps.executeUpdate()
            }
        }
    }
}
