package com.telefam.company.app.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlinx.coroutines.delay

enum class DetectedEntityType {
    URL,
    EMAIL,
    PHONE,
    OTP
}

data class DetectedMessageEntity(
    val type: DetectedEntityType,
    val startIndex: Int,
    val endIndex: Int,
    val rawText: String,
    val displayText: String,
    val actionValue: String
)

/**
 * Security-hardened entity detector for 1:1 chat messages:
 * - Auto-detects Web URLs (sanitized to HTTPS, blocking unsafe schemes and private/loopback SSRF targets)
 * - Auto-detects Email addresses
 * - Auto-detects Phone numbers in any international or local format and formats them cleanly
 * - Strengthened OTP detection (explicit prefixes, context keywords + numeric/alphanumeric codes, and standalone codes)
 */
object TelefamSmartEntityDetector {

    private val EMAIL_REGEX = Regex(
        """(?<![a-zA-Z0-9._%+\-/])([a-zA-Z0-9._%+\-]{1,64}@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,24})(?![a-zA-Z0-9._%+\-])"""
    )

    private val COMMON_TLDS = setOf(
        "com", "org", "net", "edu", "gov", "io", "co", "app", "dev", "ai", "me", "info", "biz",
        "xyz", "tech", "online", "site", "store", "cloud", "gg", "tv", "us", "uk", "ca", "au",
        "de", "fr", "in", "ke", "ng", "za", "ug", "tz", "rw", "gh", "eg", "br", "jp", "kr", "cn",
        "nl", "es", "it", "ch", "se", "no", "fi", "dk", "pl", "ie", "nz", "sg", "hk", "my", "ph",
        "id", "vn", "th", "ae", "sa", "il", "tr", "mx", "ar", "cl", "pe", "ly", "gl", "to", "sh",
        "fm", "link", "page", "live", "news", "pro", "club", "design", "studio", "world", "today"
    )

    private val URL_REGEX = Regex(
        """(?<![a-zA-Z0-9@._\-])((?:https?://|www\.)[^\s<>"{}|\\^`\[\]]+|(?:[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+([a-zA-Z]{2,16})(?:/[^\s<>"{}|\\^`\[\]]*)?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches international and national phone numbers in various formats:
    // +1 (555) 234-5678, +254 712 345 678, 0044 20 7946 0958, (555) 234-5678, 0712-345-678, 0712345678, etc.
    private val PHONE_REGEX = Regex(
        """(?<!\d)(?:\+\d{1,3}[\s.\-]?|\b00\d{1,3}[\s.\-]?|\(\d{2,4}\)[\s.\-]?)?(?:\(\d{2,4}\)[\s.\-]?|\d{2,4}[\s.\-]?){2,4}\d{2,5}(?!\d)"""
    )

    // Date-like patterns to exclude from phone number detection (e.g. 2026-10-10, 10/11/2026, 12.10.2026)
    private val DATE_LIKE_REGEX = Regex(
        """^\d{4}[\-./]\d{1,2}[\-./]\d{1,2}$|^\d{1,2}[\-./]\d{1,2}[\-./]\d{2,4}$"""
    )

    // Strong OTP context keywords
    private val OTP_CONTEXT_KEYWORD_REGEX = Regex(
        """\b(otp|verification|verify|verified|code|passcode|password|pin|2fa|two[\s\-]?factor|mfa|security|login|log[\s\-]?in|sign[\s\-]?in|signin|auth|authenticate|authentication|token|one[\s\-]?time|confirm|confirmation|secret|valid\s+for|expires\s+in|do\s+not\s+share|never\s+share|don't\s+share|enter\s+this|use\s+this)\b""",
        RegexOption.IGNORE_CASE
    )

    // 1. Explicit prefix OTPs even without other keywords: e.g. G-482910, TF-918273, OTP-123456, #482910, CODE: 482910
    private val EXPLICIT_PREFIX_OTP_REGEX = Regex(
        """(?<![a-zA-Z0-9])(?:(?:G|TF|OTP|PIN|CODE|VERIFY|AUTH)[\s:\-]{1,2}(\d{4,8}|\d{3}[\s\-]\d{3})|#(\d{4,8}))(?![a-zA-Z0-9])""",
        RegexOption.IGNORE_CASE
    )

    // 2. Numeric & grouped OTP candidates (when OTP context keywords exist in message):
    // Matches 4-8 digits (e.g. 482910), 3-3 grouped (482-910 or 482 910), or 4-4 grouped (4829-1029)
    private val CONTEXTUAL_NUMERIC_OTP_REGEX = Regex(
        """(?<![0-9+\-$€£¥.])(\d{3}[\s\-]\d{3}|\d{4}[\s\-]\d{4}|\d{4,8})(?![0-9%])"""
    )

    // 3. Alphanumeric uppercase+digit OTP candidates (5 to 8 chars containing both uppercase letters and at least 2 digits)
    private val CONTEXTUAL_ALPHANUM_OTP_REGEX = Regex(
        """\b(?=[A-Z0-9]{5,8}\b)(?=(?:.*[0-9]){2,})(?=.*[A-Z])[A-Z0-9]{5,8}\b"""
    )

    // 4. Standalone entire-message OTP (when the user receives or sends just the code itself)
    private val STANDALONE_OTP_REGEX = Regex(
        """^\s*(?:[A-Z]{1,4}[\s\-])?(\d{4,8}|\d{3}[\s\-]\d{3}|\d{4}[\s\-]\d{4})\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun detectEntities(text: String): List<DetectedMessageEntity> {
        if (text.isBlank()) return emptyList()

        val occupied = BooleanArray(text.length)
        val results = mutableListOf<DetectedMessageEntity>()

        fun isRangeFree(start: Int, endExclusive: Int): Boolean {
            if (start < 0 || endExclusive > text.length || start >= endExclusive) return false
            for (i in start until endExclusive) {
                if (occupied[i]) return false
            }
            return true
        }

        fun markRange(start: Int, endExclusive: Int) {
            for (i in start until endExclusive) {
                occupied[i] = true
            }
        }

        // 1. Detect EMAIL addresses first (so user@domain.com is not split into a URL domain.com)
        for (match in EMAIL_REGEX.findAll(text)) {
            val raw = match.value
            val start = match.range.first
            val end = match.range.last + 1
            if (isRangeFree(start, end)) {
                markRange(start, end)
                results.add(
                    DetectedMessageEntity(
                        type = DetectedEntityType.EMAIL,
                        startIndex = start,
                        endIndex = end,
                        rawText = raw,
                        displayText = raw,
                        actionValue = raw.trim()
                    )
                )
            }
        }

        // 2. Detect Web URLs and validate/sanitize to secure HTTPS
        for (match in URL_REGEX.findAll(text)) {
            var raw = match.value
            val start = match.range.first
            // Strip trailing sentence punctuation
            while (raw.isNotEmpty() && raw.last() in ".,!?;:)]}'\"") {
                raw = raw.dropLast(1)
            }
            val end = start + raw.length
            if (raw.length < 4 || !isRangeFree(start, end)) continue

            val hasExplicitScheme = raw.startsWith("http://", true) ||
                raw.startsWith("https://", true) ||
                raw.startsWith("www.", true)
            val tld = match.groupValues.getOrNull(2)?.lowercase(Locale.ROOT).orEmpty()
            if (!hasExplicitScheme && tld !in COMMON_TLDS) continue

            val safeUrl = sanitizeAndValidateHttpsUrl(raw) ?: continue
            markRange(start, end)
            results.add(
                DetectedMessageEntity(
                    type = DetectedEntityType.URL,
                    startIndex = start,
                    endIndex = end,
                    rawText = raw,
                    displayText = raw,
                    actionValue = safeUrl
                )
            )
        }

        // 3. Detect Explicit Prefix OTPs (e.g., G-482910, OTP: 123456, #849201) BEFORE phone numbers
        for (match in EXPLICIT_PREFIX_OTP_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isRangeFree(start, end)) continue
            val raw = match.value
            val codePart = match.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: raw
            val cleanOtp = codePart.replace(" ", "").replace("-", "")
            if (cleanOtp.length in 4..8) {
                markRange(start, end)
                results.add(
                    DetectedMessageEntity(
                        type = DetectedEntityType.OTP,
                        startIndex = start,
                        endIndex = end,
                        rawText = raw,
                        displayText = raw,
                        actionValue = cleanOtp
                    )
                )
            }
        }

        // 4. Detect Standalone OTP message (when whole message is a 4-8 digit OTP or 123-456)
        val standaloneMatch = STANDALONE_OTP_REGEX.find(text)
        if (standaloneMatch != null) {
            val trimmed = text.trim()
            val start = text.indexOf(trimmed)
            val end = start + trimmed.length
            if (start >= 0 && isRangeFree(start, end)) {
                val cleanOtp = standaloneMatch.groupValues[1].replace(" ", "").replace("-", "")
                // If it's 4-8 digits (and not a 10+ digit phone number), treat as standalone OTP
                if (cleanOtp.length in 4..8) {
                    markRange(start, end)
                    results.add(
                        DetectedMessageEntity(
                            type = DetectedEntityType.OTP,
                            startIndex = start,
                            endIndex = end,
                            rawText = trimmed,
                            displayText = trimmed,
                            actionValue = cleanOtp
                        )
                    )
                }
            }
        }

        // 5. DetectContextual OTPs when message has OTP keywords (before phone numbers so 7-8 digit OTPs with "OTP"/"code" aren't mistaken for phone numbers)
        val hasOtpContext = OTP_CONTEXT_KEYWORD_REGEX.containsMatchIn(text)
        if (hasOtpContext) {
            for (match in CONTEXTUAL_NUMERIC_OTP_REGEX.findAll(text)) {
                val raw = match.value
                val start = match.range.first
                val end = match.range.last + 1
                if (!isRangeFree(start, end)) continue

                val cleanDigits = raw.filter { it.isDigit() }
                if (cleanDigits.length !in 4..8) continue

                // Avoid treating common 4-digit years (e.g. 2024, 2025, 2026) as OTPs unless immediately near "code", "otp", "pin"
                val asInt = cleanDigits.toIntOrNull()
                if (cleanDigits.length == 4 && asInt != null && asInt in 1990..2035) {
                    val nearbyStart = (start - 20).coerceAtLeast(0)
                    val nearbyWindow = text.substring(nearbyStart, end).lowercase(Locale.ROOT)
                    if (!nearbyWindow.contains("code") && !nearbyWindow.contains("otp") && !nearbyWindow.contains("pin")) {
                        continue
                    }
                }

                // Avoid matching if preceded by '+' (which indicates an international phone prefix)
                if (start > 0 && text[start - 1] == '+') continue

                markRange(start, end)
                results.add(
                    DetectedMessageEntity(
                        type = DetectedEntityType.OTP,
                        startIndex = start,
                        endIndex = end,
                        rawText = raw,
                        displayText = raw,
                        actionValue = cleanDigits
                    )
                )
            }

            for (match in CONTEXTUAL_ALPHANUM_OTP_REGEX.findAll(text)) {
                val raw = match.value
                val start = match.range.first
                val end = match.range.last + 1
                if (!isRangeFree(start, end)) continue
                markRange(start, end)
                results.add(
                    DetectedMessageEntity(
                        type = DetectedEntityType.OTP,
                        startIndex = start,
                        endIndex = end,
                        rawText = raw,
                        displayText = raw,
                        actionValue = raw
                    )
                )
            }
        }

        // 6. Detect and auto-format Phone Numbers (any international or local format)
        for (match in PHONE_REGEX.findAll(text)) {
            var raw = match.value.trim()
            val start = match.range.first + (match.value.indexOf(raw).coerceAtLeast(0))
            val end = start + raw.length
            if (!isRangeFree(start, end)) continue
            if (DATE_LIKE_REGEX.matches(raw)) continue

            val digitsOnly = raw.filter { it.isDigit() }
            val hasPlus = raw.startsWith("+")
            val hasZeroZero = raw.startsWith("00")
            val hasParensOrFormatting = raw.contains("(") || raw.contains(")") || raw.contains("-") || raw.contains(" ")

            // E.164 & national phone numbers have 7 to 15 digits.
            // Require at least 7 digits (and if 7-8 digits without +, require formatting or leading 0 so plain numbers aren't false-positived)
            if (digitsOnly.length !in 7..15) continue
            if (digitsOnly.length in 7..8 && !hasPlus && !hasZeroZero && !hasParensOrFormatting && !raw.startsWith("0")) {
                continue
            }

            val formattedDisplay = formatPhoneNumberUniversal(raw)
            val dialable = toDialablePhoneNumber(raw)
            if (dialable.length < 7) continue

            markRange(start, end)
            results.add(
                DetectedMessageEntity(
                    type = DetectedEntityType.PHONE,
                    startIndex = start,
                    endIndex = end,
                    rawText = raw,
                    displayText = formattedDisplay,
                    actionValue = dialable
                )
            )
        }

        return results.sortedBy { it.startIndex }
    }

    /**
     * Converts any raw phone number into a clean, standardized human-readable format.
     * Supports +CountryCode international numbers, 00-prefixed numbers, 10-digit NANP/local numbers,
     * 07xx/01xx mobile numbers, and falls back to Android PhoneNumberUtils + smart grouping.
     */
    fun formatAnyPhoneNumber(rawPhone: String): String = formatPhoneNumberUniversal(rawPhone)

    fun formatPhoneNumberUniversal(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        val dialable = toDialablePhoneNumber(trimmed)
        val digits = dialable.filter { it.isDigit() }
        if (digits.isEmpty()) return trimmed

        // Normalize 00 international prefix to +
        val normalizedDialable = if (trimmed.startsWith("00") && digits.length >= 9) {
            "+" + digits.drop(2)
        } else {
            dialable
        }

        // First try Android's native PhoneNumberUtils if available at runtime
        val systemFormatted = runCatching {
            val countryIso = Locale.getDefault().country.ifBlank { "US" }
            PhoneNumberUtils.formatNumber(normalizedDialable, countryIso)
        }.getOrNull()

        if (!systemFormatted.isNullOrBlank() &&
            (systemFormatted.contains(" ") || systemFormatted.contains("-") || systemFormatted.contains("("))
        ) {
            return systemFormatted
        }

        // Deterministic universal formatter for international and national numbers
        val cleanDigits = normalizedDialable.filter { it.isDigit() }
        val isInternational = normalizedDialable.startsWith("+")

        return when {
            // +1 (NANP: US/Canada) 11 digits total
            isInternational && cleanDigits.startsWith("1") && cleanDigits.length == 11 -> {
                val area = cleanDigits.substring(1, 4)
                val prefix = cleanDigits.substring(4, 7)
                val line = cleanDigits.substring(7, 11)
                "+1 ($area) $prefix-$line"
            }
            // +254 / +255 / +256 / +234 / +27 / 3-digit country codes with 9 subscriber digits (12 digits total)
            isInternational && cleanDigits.length == 12 -> {
                val cc = cleanDigits.substring(0, 3)
                val p1 = cleanDigits.substring(3, 6)
                val p2 = cleanDigits.substring(6, 9)
                val p3 = cleanDigits.substring(9, 12)
                "+$cc $p1 $p2 $p3"
            }
            // 2-digit country codes (e.g. +44, +49, +33, +91, +27, +61) with 9-10 subscriber digits (11-12 digits total)
            isInternational && cleanDigits.length == 11 -> {
                val cc = cleanDigits.substring(0, 2)
                val p1 = cleanDigits.substring(2, 5)
                val p2 = cleanDigits.substring(5, 8)
                val p3 = cleanDigits.substring(8, 11)
                "+$cc $p1 $p2 $p3"
            }
            // General international number (+CC ...)
            isInternational && cleanDigits.length in 8..15 -> {
                val ccLen = when {
                    cleanDigits.startsWith("1") || cleanDigits.startsWith("7") -> 1
                    cleanDigits.length >= 12 -> 3
                    else -> 2
                }
                val cc = cleanDigits.substring(0, ccLen)
                val rest = cleanDigits.substring(ccLen)
                val groupedRest = rest.chunked(3).joinToString(" ")
                "+$cc $groupedRest"
            }
            // 10-digit national number starting with 0 (e.g. 0712 345 678)
            !isInternational && cleanDigits.length == 10 && cleanDigits.startsWith("0") -> {
                val p1 = cleanDigits.substring(0, 4)
                val p2 = cleanDigits.substring(4, 7)
                val p3 = cleanDigits.substring(7, 10)
                "$p1 $p2 $p3"
            }
            // 10-digit NANP national number (e.g. (555) 234-5678)
            !isInternational && cleanDigits.length == 10 -> {
                val area = cleanDigits.substring(0, 3)
                val prefix = cleanDigits.substring(3, 6)
                val line = cleanDigits.substring(6, 10)
                "($area) $prefix-$line"
            }
            // 11-digit national number starting with 1 (e.g. 1 (555) 234-5678)
            !isInternational && cleanDigits.length == 11 && cleanDigits.startsWith("1") -> {
                val area = cleanDigits.substring(1, 4)
                val prefix = cleanDigits.substring(4, 7)
                val line = cleanDigits.substring(7, 11)
                "+1 ($area) $prefix-$line"
            }
            // 7-digit local number (e.g. 234-5678)
            !isInternational && cleanDigits.length == 7 -> {
                "${cleanDigits.substring(0, 3)}-${cleanDigits.substring(3, 7)}"
            }
            else -> {
                val prefix = if (isInternational) "+" else ""
                prefix + cleanDigits.chunked(3).joinToString(" ")
            }
        }
    }

    fun toDialablePhoneNumber(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        val sb = StringBuilder()
        for (i in trimmed.indices) {
            val c = trimmed[i]
            if (c == '+' && sb.isEmpty()) {
                sb.append('+')
            } else if (c.isDigit()) {
                sb.append(c)
            }
        }
        val result = sb.toString()
        return if (result.startsWith("00") && result.length >= 11) {
            "+" + result.drop(2)
        } else {
            result
        }
    }

    /**
     * Validates a candidate URL and upgrades it to `https://`.
     * Rejects dangerous schemes (`javascript:`, `file:`, `data:`, `content:`, `intent:`, `vbscript:`)
     * and blocks private/loopback/link-local hosts to prevent SSRF or local WebView attacks.
     */
    fun sanitizeAndValidateHttpsUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase(Locale.ROOT)
        val forbiddenSchemes = listOf(
            "javascript:", "data:", "file:", "content:", "intent:", "vbscript:", "about:", "blob:", "ws:", "wss:"
        )
        if (forbiddenSchemes.any { lower.startsWith(it) }) {
            return null
        }

        val withHttps = when {
            lower.startsWith("https://") -> trimmed
            lower.startsWith("http://") -> "https://" + trimmed.substring(7)
            else -> "https://$trimmed"
        }

        return runCatching {
            val uri = Uri.parse(withHttps)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host?.lowercase(Locale.ROOT)?.trim()
            if (scheme != "https" || host.isNullOrBlank() || !host.contains(".")) {
                return null
            }
            if (isBlockedOrLocalHost(host)) {
                return null
            }
            withHttps
        }.getOrNull()
    }

    fun isBlockedOrLocalHost(host: String): Boolean {
        val h = host.lowercase(Locale.ROOT).trim().removeSurrounding("[", "]")
        if (h == "localhost" || h.endsWith(".local") || h.endsWith(".internal") || h == "0.0.0.0" || h == "::1") {
            return true
        }
        // Check IPv4 private/loopback ranges
        val ipv4Parts = h.split(".")
        if (ipv4Parts.size == 4 && ipv4Parts.all { it.toIntOrNull() in 0..255 }) {
            val a = ipv4Parts[0].toInt()
            val b = ipv4Parts[1].toInt()
            if (a == 10 || a == 127 || a == 0) return true
            if (a == 172 && b in 16..31) return true
            if (a == 192 && b == 168) return true
            if (a == 169 && b == 254) return true
        }
        return false
    }
}

/**
 * Renders chat message text with:
 * - Auto-detected clickable HTTPS links (opens secure in-app browser)
 * - Auto-detected clickable emails (opens email client)
 * - Auto-detected & auto-formatted clickable phone numbers (opens Call / Copy / Save to Contacts sheet)
 * - Auto-detected OTP codes with inline highlighting AND a prominent one-tap "Copy OTP" button pill
 */
@Composable
fun SmartAutodetectMessageContent(
    messageId: String,
    textContent: String,
    isOutgoing: Boolean,
    bubbleTextColor: Color,
    bubbleSubtleColor: Color,
    maxLines: Int,
    onBubbleClick: () -> Unit,
    onBubbleLongPress: () -> Unit,
    onOpenSecureBrowser: (String) -> Unit,
    onOpenEmail: (String) -> Unit,
    onOpenPhoneActions: (formattedPhone: String, dialablePhone: String) -> Unit,
    onCopyOtp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entities = remember(textContent) {
        TelefamSmartEntityDetector.detectEntities(textContent)
    }

    val otpEntities = remember(entities) {
        entities.filter { it.type == DetectedEntityType.OTP }
            .distinctBy { it.actionValue }
    }

    val linkAccentColor = if (isOutgoing) {
        Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }

    val otpBadgeBg = if (isOutgoing) {
        Color.White.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    }

    val annotatedText: AnnotatedString = remember(textContent, entities, linkAccentColor, otpBadgeBg) {
        if (entities.isEmpty()) {
            AnnotatedString(textContent)
        } else {
            buildAnnotatedString {
                var cursor = 0
                for (entity in entities) {
                    if (entity.startIndex > cursor) {
                        append(textContent.substring(cursor, entity.startIndex))
                    }
                    val spanStart = this.length
                    // Phone numbers are automatically replaced with their formatted display text!
                    append(entity.displayText)
                    val spanEnd = this.length

                    val style = when (entity.type) {
                        DetectedEntityType.URL,
                        DetectedEntityType.EMAIL,
                        DetectedEntityType.PHONE -> SpanStyle(
                            color = linkAccentColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                        DetectedEntityType.OTP -> SpanStyle(
                            color = linkAccentColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            background = otpBadgeBg,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                    addStyle(style, spanStart, spanEnd)
                    addStringAnnotation(
                        tag = entity.type.name,
                        annotation = "${entity.displayText}|||${entity.actionValue}",
                        start = spanStart,
                        end = spanEnd
                    )
                    cursor = entity.endIndex
                }
                if (cursor < textContent.length) {
                    append(textContent.substring(cursor))
                }
            }
        }
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Column(modifier = modifier) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.5.sp,
                lineHeight = 20.sp
            ),
            color = bubbleTextColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .pointerInput(annotatedText) {
                    detectTapGestures(
                        onLongPress = { onBubbleLongPress() },
                        onTap = { pos ->
                            val layout = textLayoutResult
                            if (layout != null && entities.isNotEmpty()) {
                                val offset = layout.getOffsetForPosition(pos)
                                val hit = annotatedText
                                    .getStringAnnotations(start = offset, end = offset)
                                    .firstOrNull()
                                if (hit != null) {
                                    val parts = hit.item.split("|||", limit = 2)
                                    val displayVal = parts.getOrElse(0) { hit.item }
                                    val actionVal = parts.getOrElse(1) { hit.item }
                                    when (hit.tag) {
                                        DetectedEntityType.URL.name -> onOpenSecureBrowser(actionVal)
                                        DetectedEntityType.EMAIL.name -> onOpenEmail(actionVal)
                                        DetectedEntityType.PHONE.name -> onOpenPhoneActions(displayVal, actionVal)
                                        DetectedEntityType.OTP.name -> onCopyOtp(actionVal)
                                        else -> onBubbleClick()
                                    }
                                    return@detectTapGestures
                                }
                            }
                            onBubbleClick()
                        }
                    )
                }
                .testTag("chat_message_smart_text_$messageId")
        )

        // Strengthened Auto-OTP One-Tap Copy Button Bar inside the bubble
        if (otpEntities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            otpEntities.forEach { otpEntity ->
                var justCopied by remember(messageId, otpEntity.actionValue) { mutableStateOf(false) }

                LaunchedEffect(justCopied) {
                    if (justCopied) {
                        delay(2400L)
                        justCopied = false
                    }
                }

                Surface(
                    onClick = {
                        justCopied = true
                        onCopyOtp(otpEntity.actionValue)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOutgoing) {
                        Color.White.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isOutgoing) {
                            Color.White.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .testTag("copy_otp_button_${messageId}_${otpEntity.actionValue}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = if (justCopied) LucideIconKind.CHECK_SINGLE else LucideIconKind.COPY,
                            tint = if (isOutgoing) Color.White else MaterialTheme.colorScheme.primary,
                            size = 14.dp
                        )
                        Text(
                            text = if (justCopied) {
                                "Copied ${otpEntity.actionValue}"
                            } else {
                                "Copy code ${otpEntity.actionValue}"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet displayed when a user taps an auto-detected phone number in a chat message.
 * Shows the formatted phone number and provides Call, Copy, and Save to Device Contacts actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberActionBottomSheet(
    formattedPhone: String,
    dialablePhone: String,
    suggestedContactName: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("phone_number_action_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.PHONE_CALL,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 20.dp
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedPhone,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (dialablePhone != formattedPhone) {
                        Text(
                            text = dialablePhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Call
            PhoneSheetActionRow(
                icon = LucideIconKind.PHONE_CALL,
                title = "Call $formattedPhone",
                subtitle = "Open phone dialer",
                testTag = "phone_sheet_call_action",
                onClick = {
                    onDismiss()
                    launchDialerSafely(context, dialablePhone)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Copy
            PhoneSheetActionRow(
                icon = LucideIconKind.COPY,
                title = "Copy phone number",
                subtitle = "Copy $formattedPhone to clipboard",
                testTag = "phone_sheet_copy_action",
                onClick = {
                    copyTextToClipboard(context, "Phone Number", formattedPhone)
                    Toast.makeText(context, "Phone number copied", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Save to device contacts
            PhoneSheetActionRow(
                icon = LucideIconKind.USER_PLUS_CONTACT,
                title = "Save to contacts",
                subtitle = "Add $formattedPhone to your device contacts",
                testTag = "phone_sheet_save_contact_action",
                onClick = {
                    onDismiss()
                    savePhoneNumberToDeviceContacts(
                        context = context,
                        formattedPhone = formattedPhone,
                        dialablePhone = dialablePhone,
                        suggestedName = suggestedContactName
                    )
                }
            )
        }
    }
}

@Composable
private fun PhoneSheetActionRow(
    icon: LucideIconKind,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TelefamLucideIcon(
                kind = icon,
                tint = MaterialTheme.colorScheme.primary,
                size = 20.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Hardened Secure In-App Browser Dialog:
 * - Enforces HTTPS-only navigation and blocks private/loopback SSRF or unsafe schemes (`javascript:`, `file:`, `content:`, `intent:`, `data:`)
 * - Disables file access, content access, mixed content, and multi-window popups
 * - Rejects invalid SSL certificates (`handler.cancel()`) and alerts the user
 * - Clears WebView cookies/cache on exit for session privacy
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SecureInAppBrowserDialog(
    initialUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val safeInitialUrl = remember(initialUrl) {
        TelefamSmartEntityDetector.sanitizeAndValidateHttpsUrl(initialUrl)
    }

    var currentUrl by remember { mutableStateOf(safeInitialUrl ?: initialUrl) }
    var pageTitle by remember { mutableStateOf("Secure Browser") }
    var loadProgress by remember { mutableFloatStateOf(0.1f) }
    var isLoading by remember { mutableStateOf(true) }
    var securityErrorText by remember {
        mutableStateOf(
            if (safeInitialUrl == null) {
                "Blocked unsafe or non-HTTPS link for your security."
            } else {
                null
            }
        )
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val hostDisplay = remember(currentUrl) {
        runCatching { Uri.parse(currentUrl).host.orEmpty() }.getOrDefault("").ifBlank { currentUrl }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                webViewRef?.apply {
                    stopLoading()
                    clearHistory()
                    clearCache(true)
                    loadUrl("about:blank")
                    destroy()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize()
                .testTag("secure_in_app_browser_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Security Toolbar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("secure_browser_close_button")
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.CLOSE_X,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    size = 20.dp
                                )
                            }

                            // Verified HTTPS Domain Pill
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TelefamLucideIcon(
                                        kind = if (securityErrorText == null) {
                                            LucideIconKind.LOCK_E2EE
                                        } else {
                                            LucideIconKind.ALERT_TRIANGLE
                                        },
                                        tint = if (securityErrorText == null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        size = 15.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pageTitle.ifBlank { hostDisplay },
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = hostDisplay,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        securityErrorText = null
                                        webViewRef?.reload()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("secure_browser_reload_button")
                                ) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.REFRESH_RETRY,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        size = 18.dp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        copyTextToClipboard(context, "URL", currentUrl)
                                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("secure_browser_copy_url_button")
                                ) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.COPY,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        size = 18.dp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val safe = TelefamSmartEntityDetector.sanitizeAndValidateHttpsUrl(currentUrl)
                                        if (safe != null) {
                                            runCatching {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safe)).apply {
                                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            }.onFailure {
                                                Toast.makeText(context, "No external browser found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("secure_browser_external_button")
                                ) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.EXTERNAL_LINK,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        size = 18.dp
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isLoading) {
                            LinearProgressIndicator(
                                progress = { loadProgress.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // Content Area: Security Warning or Hardened WebView
                val err = securityErrorText
                if (err != null || safeInitialUrl == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.widthIn(max = 420.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.ALERT_TRIANGLE,
                                    tint = MaterialTheme.colorScheme.error,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Connection Blocked for Security",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = err ?: "This link uses an insecure protocol or untrusted certificate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Return to chat", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    @Suppress("DEPRECATION")
                                    allowFileAccessFromFileURLs = false
                                    @Suppress("DEPRECATION")
                                    allowUniversalAccessFromFileURLs = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    javaScriptCanOpenWindowsAutomatically = false
                                    setSupportMultipleWindows(false)
                                    setGeolocationEnabled(false)
                                    mediaPlaybackRequiresUserGesture = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        safeBrowsingEnabled = true
                                    }
                                }
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadProgress = newProgress / 100f
                                        isLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) {
                                            pageTitle = title
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                        }
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                        }
                                        isLoading = false
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val reqUri = request?.url ?: return true
                                        val rawTarget = reqUri.toString()
                                        val validated = TelefamSmartEntityDetector.sanitizeAndValidateHttpsUrl(rawTarget)
                                        return if (validated == null) {
                                            securityErrorText = "Blocked redirect to an untrusted or non-HTTPS address."
                                            true
                                        } else {
                                            if (validated != rawTarget) {
                                                view?.loadUrl(validated)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?
                                    ) {
                                        // Strictly reject invalid TLS/SSL certificates
                                        handler?.cancel()
                                        securityErrorText = "SSL/TLS certificate verification failed for this website."
                                    }
                                }

                                loadUrl(safeInitialUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

fun launchEmailClientSafely(context: Context, emailAddress: String) {
    val cleanEmail = emailAddress.trim().removePrefix("mailto:")
    if (cleanEmail.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(cleanEmail)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }.onFailure {
        copyTextToClipboard(context, "Email Address", cleanEmail)
        Toast.makeText(context, "Email copied: $cleanEmail", Toast.LENGTH_SHORT).show()
    }
}

fun launchDialerSafely(context: Context, dialablePhone: String) {
    val clean = TelefamSmartEntityDetector.toDialablePhoneNumber(dialablePhone)
    if (clean.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(clean)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }.onFailure {
        copyTextToClipboard(context, "Phone Number", clean)
        Toast.makeText(context, "Phone number copied: $clean", Toast.LENGTH_SHORT).show()
    }
}

fun savePhoneNumberToDeviceContacts(
    context: Context,
    formattedPhone: String,
    dialablePhone: String,
    suggestedName: String = ""
) {
    val phoneToSave = formattedPhone.ifBlank { dialablePhone }
    runCatching {
        val insertIntent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, phoneToSave)
            if (dialablePhone.isNotBlank() && dialablePhone != phoneToSave) {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, dialablePhone)
            }
            if (suggestedName.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.NAME, suggestedName)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(insertIntent)
    }.onFailure {
        runCatching {
            val fallbackIntent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, phoneToSave)
                if (suggestedName.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.NAME, suggestedName)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }.onFailure {
            copyTextToClipboard(context, "Phone Number", phoneToSave)
            Toast.makeText(context, "Copied $phoneToSave to save in Contacts", Toast.LENGTH_SHORT).show()
        }
    }
}
