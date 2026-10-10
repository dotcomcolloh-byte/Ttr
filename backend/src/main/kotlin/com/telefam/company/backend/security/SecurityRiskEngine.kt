package com.telefam.company.backend.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class RiskEvaluation(
    val score: Int,
    val level: String,
    val signals: List<String>,
    val serverVerifiedDeviceHash: String,
    val ipSubnetFingerprint: String,
    val requiresStepUpOtp: Boolean,
    val blockRequest: Boolean
)

/**
 * Zero-Trust Security & Behavioral Anomaly Risk Engine for Telefam.
 * Never trusts client-provided device or IP fingerprints directly; binds them with
 * server-side network telemetry, HMAC pepper, request cadence, and geo-velocity checks.
 */
class SecurityRiskEngine(
    private val serverPepper: String,
    private val sessionMasterKeyHex: String
) {
    private val secureRandom = SecureRandom()

    fun computeServerDeviceHash(
        untrustedClientDeviceFp: String,
        userAgent: String,
        acceptLanguage: String,
        tlsCipherHeader: String
    ): String {
        val raw = "v1|$untrustedClientDeviceFp|${userAgent.take(160)}|${acceptLanguage.take(48)}|$tlsCipherHeader"
        return hmacSha256(raw, serverPepper)
    }

    fun computeIpSubnetFingerprint(remoteIp: String, forwardedFor: String?): String {
        val effectiveIp = forwardedFor?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: remoteIp
        val parts = effectiveIp.split(".")
        val subnetPrefix = if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
        } else {
            effectiveIp.split(":").take(4).joinToString(":")
        }
        return hmacSha256("ip_subnet|$subnetPrefix", serverPepper).take(32)
    }

    fun evaluateRequestRisk(
        untrustedClientDeviceFp: String,
        remoteIp: String,
        forwardedFor: String?,
        userAgent: String,
        acceptLanguage: String,
        tlsCipherHeader: String,
        knownDeviceHashes: Set<String>,
        lastKnownLocation: String?,
        currentLocation: String?,
        recentRequestsLastMinute: Int,
        failedLoginAttempts: Int,
        failedOtpAttempts: Int
    ): RiskEvaluation {
        val signals = mutableListOf<String>()
        var score = 5

        val serverDeviceHash = computeServerDeviceHash(
            untrustedClientDeviceFp = untrustedClientDeviceFp,
            userAgent = userAgent,
            acceptLanguage = acceptLanguage,
            tlsCipherHeader = tlsCipherHeader
        )
        val ipFp = computeIpSubnetFingerprint(remoteIp, forwardedFor)

        if (untrustedClientDeviceFp.length < 16 || untrustedClientDeviceFp.all { it == '0' }) {
            score += 35
            signals.add("MALFORMED_OR_SPOOFED_CLIENT_FINGERPRINT")
        }

        if (userAgent.isBlank() || userAgent.contains("curl", ignoreCase = true) ||
            userAgent.contains("python", ignoreCase = true) || userAgent.contains("headless", ignoreCase = true)
        ) {
            score += 45
            signals.add("AUTOMATED_BOT_SIGNATURE")
        }

        val isNewDevice = knownDeviceHashes.isNotEmpty() && !knownDeviceHashes.contains(serverDeviceHash)
        if (isNewDevice) {
            score += 30
            signals.add("UNRECOGNIZED_DEVICE_HASH")
        }

        val isNewLocation = !lastKnownLocation.isNullOrBlank() &&
            !currentLocation.isNullOrBlank() &&
            !lastKnownLocation.equals(currentLocation, ignoreCase = true)
        if (isNewLocation) {
            score += 25
            signals.add("GEO_LOCATION_SHIFT_DETECTED")
        }

        if (recentRequestsLastMinute > 20) {
            score += 40
            signals.add("HIGH_VELOCITY_API_HAMMERING")
        } else if (recentRequestsLastMinute > 10) {
            score += 15
            signals.add("ELEVATED_REQUEST_CADENCE")
        }

        if (failedLoginAttempts > 0) {
            score += failedLoginAttempts * 12
            signals.add("PRIOR_FAILED_LOGINS_$failedLoginAttempts")
        }

        if (failedOtpAttempts > 0) {
            score += failedOtpAttempts * 15
            signals.add("PRIOR_FAILED_OTPS_$failedOtpAttempts")
        }

        val clampedScore = score.coerceIn(0, 100)
        val level = when {
            clampedScore >= 75 -> "CRITICAL"
            clampedScore >= 50 -> "HIGH"
            clampedScore >= 25 -> "MEDIUM"
            else -> "LOW"
        }

        return RiskEvaluation(
            score = clampedScore,
            level = level,
            signals = signals,
            serverVerifiedDeviceHash = serverDeviceHash,
            ipSubnetFingerprint = ipFp,
            requiresStepUpOtp = isNewDevice || isNewLocation || clampedScore >= 50,
            blockRequest = clampedScore >= 85
        )
    }

    fun encryptSessionPayload(plainJson: String): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(sessionMasterKeyHex.toByteArray(StandardCharsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))
        return encrypted to iv
    }

    fun hmacSha256(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
