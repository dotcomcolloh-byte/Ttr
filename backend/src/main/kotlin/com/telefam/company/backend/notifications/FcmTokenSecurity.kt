package com.telefam.company.backend.notifications

import java.security.MessageDigest

/** Validation and privacy helpers for device notification registrations. */
object FcmTokenSecurity {
    fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    fun validateRegistration(
        headerUserId: String,
        payloadUserId: String,
        fcmToken: String,
        fcmTokenSha256: String,
        deviceFingerprint: String,
        appVersion: String,
        platform: String,
        idempotencyKey: String
    ) {
        require(headerUserId.isNotBlank() && headerUserId == payloadUserId) {
            "RLS user id must match the registration user id"
        }
        require(fcmToken.length in 20..4096) { "Invalid FCM token" }
        require(fcmTokenSha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            "Invalid FCM token hash"
        }
        require(sha256Hex(fcmToken).equals(fcmTokenSha256, ignoreCase = true)) {
            "FCM token hash mismatch"
        }
        require(deviceFingerprint.length in 1..128) { "Invalid device fingerprint" }
        require(appVersion.length in 1..64) { "Invalid app version" }
        require(platform == "ANDROID") { "Unsupported notification platform" }
        require(idempotencyKey.length in 8..128) { "Invalid idempotency key" }
    }

    fun validateRevocation(
        headerUserId: String,
        payloadUserId: String,
        fcmTokenSha256: String,
        deviceFingerprint: String,
        idempotencyKey: String
    ) {
        require(headerUserId.isNotBlank() && headerUserId == payloadUserId) {
            "RLS user id must match the revocation user id"
        }
        require(fcmTokenSha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            "Invalid FCM token hash"
        }
        require(deviceFingerprint.length in 1..128) { "Invalid device fingerprint" }
        require(idempotencyKey.length in 8..128) { "Invalid idempotency key" }
    }
}
