package com.telefam.company.app.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Shared KMP-compatible form validation, email-prefix autofill derivation,
 * 13+ age verification, username uniqueness suggestion generator, and SQL-injection rejection.
 */
object TelefamValidators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,12}$")
    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,24}$")
    private val PHONE_DIGITS_REGEX = Regex("^[0-9]{7,15}$")
    private val OTP_REGEX = Regex("^[0-9]{6}$")

    // Suspicious SQLi or script injection tokens rejected before reaching query layer
    private val SQLI_SUSPICIOUS_PATTERN = Regex(
        "(?i)(\\bUNION\\b.*\\bSELECT\\b|\\bDROP\\b.*\\bTABLE\\b|--|;\\s*DELETE|;\\s*UPDATE|/\\*.*\\*/|\\bOR\\b\\s+['\"]?1['\"]?\\s*=\\s*['\"]?1)"
    )

    fun containsSqlInjectionSignature(input: String): Boolean {
        return SQLI_SUSPICIOUS_PATTERN.containsMatchIn(input)
    }

    fun validateEmail(email: String): Boolean {
        val clean = email.trim()
        if (clean.length !in 5..254) return false
        if (containsSqlInjectionSignature(clean)) return false
        return EMAIL_REGEX.matches(clean)
    }

    /**
     * Derives a clean Full Name and a sanitized Username from an email prefix.
     * Example: "amina.wanjiku_24@telefam.company" -> Pair("Amina Wanjiku", "amina_wanjiku_24")
     */
    fun deriveFullNameAndUsernameFromEmail(email: String): Pair<String, String> {
        val prefix = email.trim().substringBefore("@").trim()
        if (prefix.isBlank()) return "" to ""

        // 1. Derive Full Name by splitting on '.', '_', '-', '+' and stripping trailing numbers
        val words = prefix.split(Regex("[._\\-+]+"))
            .map { part -> part.replace(Regex("[0-9]+"), "").trim() }
            .filter { it.isNotBlank() }
            .map { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                }
            }
        val derivedFullName = words.joinToString(" ").take(80).ifBlank {
            prefix.replace(Regex("[^a-zA-Z ]"), "").trim().replaceFirstChar { it.uppercase() }
        }

        // 2. Derive sanitized username (3..24 chars, [a-z0-9_])
        val rawUsername = prefix.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(24)

        val derivedUsername = when {
            rawUsername.length >= 3 -> rawUsername
            rawUsername.isNotEmpty() -> "${rawUsername}_tf"
            else -> ""
        }

        return derivedFullName to derivedUsername
    }

    /**
     * Generates up to 4 unique, valid username suggestions when a desired username already exists.
     */
    fun generateUniqueUsernameSuggestions(
        baseUsername: String,
        fullName: String,
        isUsernameTaken: (String) -> Boolean
    ): List<String> {
        val cleanBase = baseUsername.trim().removePrefix("@").lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(17)
            .ifBlank { "telefam_user" }

        val nameParts = fullName.trim().lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-z0-9]"), "") }
            .filter { it.isNotBlank() }

        val candidates = buildList {
            add("${cleanBase}_tf")
            if (nameParts.size >= 2) {
                add("${nameParts.first()}_${nameParts.last()}".take(22))
                add("${nameParts.first()}_${nameParts.last().take(3)}26".take(22))
            }
            add("${cleanBase}_ke")
            add("${cleanBase}_26")
            add("${cleanBase}_official".take(24))
            add("real_$cleanBase".take(24))
            for (num in listOf(7, 12, 24, 88, 101, 254, 365)) {
                add("${cleanBase}_$num".take(24))
            }
        }.distinct()

        return candidates
            .filter { validateUsername(it) && !isUsernameTaken(it) }
            .take(4)
    }

    fun validatePassword(password: String): Boolean {
        if (password.length < 8 || password.length > 128) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    fun calculatePasswordStrength(password: String): Float {
        if (password.isEmpty()) return 0f
        var points = 0f
        if (password.length >= 8) points += 0.25f
        if (password.length >= 12) points += 0.15f
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) points += 0.2f
        if (password.any { it.isDigit() }) points += 0.2f
        if (password.any { !it.isLetterOrDigit() }) points += 0.2f
        return points.coerceIn(0f, 1f)
    }

    fun validateOtp(otp: String): Boolean {
        return OTP_REGEX.matches(otp.trim())
    }

    fun validateFullName(fullName: String): Boolean {
        val trimmed = fullName.trim()
        if (containsSqlInjectionSignature(trimmed)) return false
        return trimmed.length in 2..120
    }

    fun validateUsername(username: String): Boolean {
        val clean = username.trim().removePrefix("@")
        if (containsSqlInjectionSignature(clean)) return false
        return USERNAME_REGEX.matches(clean)
    }

    fun validatePhoneNumber(phone: String): Boolean {
        val digitsOnly = phone.filter { it.isDigit() }
        return PHONE_DIGITS_REGEX.matches(digitsOnly)
    }

    /**
     * Validates that the YYYY-MM-DD date of birth corresponds to an age of 13 years and above (up to 120 years).
     */
    fun validateAgeAtLeast13(dobIso: String, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val age = calculateAgeYears(dobIso, nowEpochMs) ?: return false
        return age in 13..120
    }

    fun calculateAgeYears(dobIso: String, nowEpochMs: Long = System.currentTimeMillis()): Int? {
        val parts = dobIso.trim().split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null // 1..12
        val day = parts[2].toIntOrNull() ?: return null   // 1..31
        if (year < 1900 || month !in 1..12 || day !in 1..31) return null

        val utc = TimeZone.getTimeZone("UTC")
        val todayCal = Calendar.getInstance(utc).apply { timeInMillis = nowEpochMs }
        val dobCal = Calendar.getInstance(utc).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (dobCal.timeInMillis > todayCal.timeInMillis) return -1

        var age = todayCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val dobMonth = dobCal.get(Calendar.MONTH)
        if (todayMonth < dobMonth ||
            (todayMonth == dobMonth && todayCal.get(Calendar.DAY_OF_MONTH) < dobCal.get(Calendar.DAY_OF_MONTH))
        ) {
            age--
        }
        return age
    }

    fun validateBio(bio: String): Boolean {
        if (containsSqlInjectionSignature(bio)) return false
        return bio.length <= 150
    }
}
