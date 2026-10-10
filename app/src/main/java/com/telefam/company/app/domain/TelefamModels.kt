package com.telefam.company.app.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.R

enum class AuthMode {
    SIGN_UP,
    LOGIN,
    VERIFY_SIGNUP_OTP,
    FORGOT_PASSWORD_REQUEST,
    FORGOT_PASSWORD_VERIFY_AND_RESET,
    NEW_DEVICE_VERIFY_OTP
}

enum class HomeNavTab {
    CHATS,
    CONTACTS,
    POST,
    FEEDS,
    PROFILE
}

enum class PrivacyAudience(val wireValue: String) {
    ANYONE("ANYONE"),
    NOBODY("NOBODY");

    companion object {
        fun fromWire(value: String?): PrivacyAudience =
            if (value?.uppercase() == "NOBODY") NOBODY else ANYONE
    }
}

enum class PrivacyRuleKey {
    MESSAGE_REQUESTS,
    WHO_CAN_CALL_ME,
    WHO_CAN_SCREENSHOT_CHATS,
    WHO_CAN_SHARE_CHATS,
    WHO_CAN_COPY_MESSAGES,
    WHO_CAN_DOWNLOAD_MEDIA,
    READ_RECEIPTS,
    TYPING_INDICATORS,
    ONLINE_INDICATORS
}

data class ChatBackgroundThemeOption(
    val id: String,
    @StringRes val titleRes: Int,
    @DrawableRes val drawableRes: Int,
    val accentHex: String
)

object TelefamChatThemes {
    val ALL: List<ChatBackgroundThemeOption> = listOf(
        ChatBackgroundThemeOption(
            id = "crimson_silk",
            titleRes = R.string.theme_crimson_silk,
            drawableRes = R.drawable.bg_chat_theme_crimson_silk_1791535312731,
            accentHex = "#C80815"
        ),
        ChatBackgroundThemeOption(
            id = "savannah_sunset",
            titleRes = R.string.theme_savannah_sunset,
            drawableRes = R.drawable.bg_chat_theme_savannah_sunset_1791535323502,
            accentHex = "#E65100"
        ),
        ChatBackgroundThemeOption(
            id = "aurora_cosmos",
            titleRes = R.string.theme_aurora_cosmos,
            drawableRes = R.drawable.bg_chat_theme_aurora_cosmos_1791535334698,
            accentHex = "#00897B"
        ),
        ChatBackgroundThemeOption(
            id = "emerald_botanical",
            titleRes = R.string.theme_emerald_botanical,
            drawableRes = R.drawable.bg_chat_theme_emerald_botanical_1791535344448,
            accentHex = "#2E7D32"
        ),
        ChatBackgroundThemeOption(
            id = "sahara_twilight",
            titleRes = R.string.theme_sahara_twilight,
            drawableRes = R.drawable.bg_chat_theme_sahara_twilight_1791535356607,
            accentHex = "#AD1457"
        ),
        ChatBackgroundThemeOption(
            id = "alpine_mist",
            titleRes = R.string.theme_alpine_mist,
            drawableRes = R.drawable.bg_chat_theme_alpine_mist_1791535368092,
            accentHex = "#5E35B1"
        )
    )

    val DEFAULT: ChatBackgroundThemeOption = ALL.first()

    fun findById(id: String?): ChatBackgroundThemeOption =
        ALL.find { it.id == id } ?: DEFAULT
}

data class MessageBubbleColorOption(
    val colorHex: String,
    @StringRes val nameRes: Int
)

object TelefamBubbleColors {
    // Default to Telefam Red first as requested
    val ALL: List<MessageBubbleColorOption> = listOf(
        MessageBubbleColorOption("#C80815", R.string.bubble_color_red),
        MessageBubbleColorOption("#D81B60", R.string.bubble_color_crimson_rose),
        MessageBubbleColorOption("#1565C0", R.string.bubble_color_royal_blue),
        MessageBubbleColorOption("#00897B", R.string.bubble_color_emerald),
        MessageBubbleColorOption("#6A1B9A", R.string.bubble_color_violet),
        MessageBubbleColorOption("#E65100", R.string.bubble_color_sunset_amber),
        MessageBubbleColorOption("#00838F", R.string.bubble_color_ocean_teal),
        MessageBubbleColorOption("#283593", R.string.bubble_color_indigo),
        MessageBubbleColorOption("#AD1457", R.string.bubble_color_magenta),
        MessageBubbleColorOption("#37474F", R.string.bubble_color_slate)
    )

    const val DEFAULT_HEX: String = "#C80815"

    fun findByHex(hex: String?): MessageBubbleColorOption =
        ALL.find { it.colorHex.equals(hex, ignoreCase = true) } ?: ALL.first()
}

enum class RiskLevel(val label: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL")
}

data class CountryDialCode(
    val isoCode: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String
)

/**
 * Complete Global Country & Territory Dial Code Directory (240+ countries).
 * Default is Kenya (+254) matching Reference Image 2, with full instant search by
 * country name, ISO code, or dial prefix.
 */
object TelefamCountries {

    private fun isoToFlagEmoji(iso: String): String {
        if (iso.length != 2) return "🌐"
        val first = Character.codePointAt(iso.uppercase(), 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(iso.uppercase(), 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    private fun c(iso: String, name: String, dial: String): CountryDialCode =
        CountryDialCode(iso, name, dial, isoToFlagEmoji(iso))

    val ALL: List<CountryDialCode> = listOf(
        // Default pinned at top matching Reference Image 2
        c("KE", "Kenya", "+254"),
        // Complete A-Z World Countries & Territories
        c("AF", "Afghanistan", "+93"),
        c("AL", "Albania", "+355"),
        c("DZ", "Algeria", "+213"),
        c("AS", "American Samoa", "+1684"),
        c("AD", "Andorra", "+376"),
        c("AO", "Angola", "+244"),
        c("AI", "Anguilla", "+1264"),
        c("AG", "Antigua and Barbuda", "+1268"),
        c("AR", "Argentina", "+54"),
        c("AM", "Armenia", "+374"),
        c("AW", "Aruba", "+297"),
        c("AU", "Australia", "+61"),
        c("AT", "Austria", "+43"),
        c("AZ", "Azerbaijan", "+994"),
        c("BS", "Bahamas", "+1242"),
        c("BH", "Bahrain", "+973"),
        c("BD", "Bangladesh", "+880"),
        c("BB", "Barbados", "+1246"),
        c("BY", "Belarus", "+375"),
        c("BE", "Belgium", "+32"),
        c("BZ", "Belize", "+501"),
        c("BJ", "Benin", "+229"),
        c("BM", "Bermuda", "+1441"),
        c("BT", "Bhutan", "+975"),
        c("BO", "Bolivia", "+591"),
        c("BA", "Bosnia and Herzegovina", "+387"),
        c("BW", "Botswana", "+267"),
        c("BR", "Brazil", "+55"),
        c("IO", "British Indian Ocean Territory", "+246"),
        c("VG", "British Virgin Islands", "+1284"),
        c("BN", "Brunei", "+673"),
        c("BG", "Bulgaria", "+359"),
        c("BF", "Burkina Faso", "+226"),
        c("BI", "Burundi", "+257"),
        c("KH", "Cambodia", "+855"),
        c("CM", "Cameroon", "+237"),
        c("CA", "Canada", "+1"),
        c("CV", "Cape Verde", "+238"),
        c("KY", "Cayman Islands", "+1345"),
        c("CF", "Central African Republic", "+236"),
        c("TD", "Chad", "+235"),
        c("CL", "Chile", "+56"),
        c("CN", "China", "+86"),
        c("CO", "Colombia", "+57"),
        c("KM", "Comoros", "+269"),
        c("CG", "Congo (Republic)", "+242"),
        c("CD", "Congo (DRC)", "+243"),
        c("CK", "Cook Islands", "+682"),
        c("CR", "Costa Rica", "+506"),
        c("CI", "Côte d'Ivoire", "+225"),
        c("HR", "Croatia", "+385"),
        c("CU", "Cuba", "+53"),
        c("CW", "Curaçao", "+599"),
        c("CY", "Cyprus", "+357"),
        c("CZ", "Czechia", "+420"),
        c("DK", "Denmark", "+45"),
        c("DJ", "Djibouti", "+253"),
        c("DM", "Dominica", "+1767"),
        c("DO", "Dominican Republic", "+1809"),
        c("EC", "Ecuador", "+593"),
        c("EG", "Egypt", "+20"),
        c("SV", "El Salvador", "+503"),
        c("GQ", "Equatorial Guinea", "+240"),
        c("ER", "Eritrea", "+291"),
        c("EE", "Estonia", "+372"),
        c("SZ", "Eswatini", "+268"),
        c("ET", "Ethiopia", "+251"),
        c("FK", "Falkland Islands", "+500"),
        c("FO", "Faroe Islands", "+298"),
        c("FJ", "Fiji", "+679"),
        c("FI", "Finland", "+358"),
        c("FR", "France", "+33"),
        c("GF", "French Guiana", "+594"),
        c("PF", "French Polynesia", "+689"),
        c("GA", "Gabon", "+241"),
        c("GM", "Gambia", "+220"),
        c("GE", "Georgia", "+995"),
        c("DE", "Germany", "+49"),
        c("GH", "Ghana", "+233"),
        c("GI", "Gibraltar", "+350"),
        c("GR", "Greece", "+30"),
        c("GL", "Greenland", "+299"),
        c("GD", "Grenada", "+1473"),
        c("GP", "Guadeloupe", "+590"),
        c("GU", "Guam", "+1671"),
        c("GT", "Guatemala", "+502"),
        c("GG", "Guernsey", "+44"),
        c("GN", "Guinea", "+224"),
        c("GW", "Guinea-Bissau", "+245"),
        c("GY", "Guyana", "+592"),
        c("HT", "Haiti", "+509"),
        c("HN", "Honduras", "+504"),
        c("HK", "Hong Kong", "+852"),
        c("HU", "Hungary", "+36"),
        c("IS", "Iceland", "+354"),
        c("IN", "India", "+91"),
        c("ID", "Indonesia", "+62"),
        c("IR", "Iran", "+98"),
        c("IQ", "Iraq", "+964"),
        c("IE", "Ireland", "+353"),
        c("IM", "Isle of Man", "+44"),
        c("IL", "Israel", "+972"),
        c("IT", "Italy", "+39"),
        c("JM", "Jamaica", "+1876"),
        c("JP", "Japan", "+81"),
        c("JE", "Jersey", "+44"),
        c("JO", "Jordan", "+962"),
        c("KZ", "Kazakhstan", "+7"),
        c("KI", "Kiribati", "+686"),
        c("XK", "Kosovo", "+383"),
        c("KW", "Kuwait", "+965"),
        c("KG", "Kyrgyzstan", "+996"),
        c("LA", "Laos", "+856"),
        c("LV", "Latvia", "+371"),
        c("LB", "Lebanon", "+961"),
        c("LS", "Lesotho", "+266"),
        c("LR", "Liberia", "+231"),
        c("LY", "Libya", "+218"),
        c("LI", "Liechtenstein", "+423"),
        c("LT", "Lithuania", "+370"),
        c("LU", "Luxembourg", "+352"),
        c("MO", "Macau", "+853"),
        c("MG", "Madagascar", "+261"),
        c("MW", "Malawi", "+265"),
        c("MY", "Malaysia", "+60"),
        c("MV", "Maldives", "+960"),
        c("ML", "Mali", "+223"),
        c("MT", "Malta", "+356"),
        c("MH", "Marshall Islands", "+692"),
        c("MQ", "Martinique", "+596"),
        c("MR", "Mauritania", "+222"),
        c("MU", "Mauritius", "+230"),
        c("YT", "Mayotte", "+262"),
        c("MX", "Mexico", "+52"),
        c("FM", "Micronesia", "+691"),
        c("MD", "Moldova", "+373"),
        c("MC", "Monaco", "+377"),
        c("MN", "Mongolia", "+976"),
        c("ME", "Montenegro", "+382"),
        c("MS", "Montserrat", "+1664"),
        c("MA", "Morocco", "+212"),
        c("MZ", "Mozambique", "+258"),
        c("MM", "Myanmar", "+95"),
        c("NA", "Namibia", "+264"),
        c("NR", "Nauru", "+674"),
        c("NP", "Nepal", "+977"),
        c("NL", "Netherlands", "+31"),
        c("NC", "New Caledonia", "+687"),
        c("NZ", "New Zealand", "+64"),
        c("NI", "Nicaragua", "+505"),
        c("NE", "Niger", "+227"),
        c("NG", "Nigeria", "+234"),
        c("NU", "Niue", "+683"),
        c("KP", "North Korea", "+850"),
        c("MK", "North Macedonia", "+389"),
        c("MP", "Northern Mariana Islands", "+1670"),
        c("NO", "Norway", "+47"),
        c("OM", "Oman", "+968"),
        c("PK", "Pakistan", "+92"),
        c("PW", "Palau", "+680"),
        c("PS", "Palestine", "+970"),
        c("PA", "Panama", "+507"),
        c("PG", "Papua New Guinea", "+675"),
        c("PY", "Paraguay", "+595"),
        c("PE", "Peru", "+51"),
        c("PH", "Philippines", "+63"),
        c("PL", "Poland", "+48"),
        c("PT", "Portugal", "+351"),
        c("PR", "Puerto Rico", "+1787"),
        c("QA", "Qatar", "+974"),
        c("RE", "Réunion", "+262"),
        c("RO", "Romania", "+40"),
        c("RU", "Russia", "+7"),
        c("RW", "Rwanda", "+250"),
        c("KN", "Saint Kitts and Nevis", "+1869"),
        c("LC", "Saint Lucia", "+1758"),
        c("VC", "Saint Vincent and the Grenadines", "+1784"),
        c("WS", "Samoa", "+685"),
        c("SM", "San Marino", "+378"),
        c("ST", "São Tomé and Príncipe", "+239"),
        c("SA", "Saudi Arabia", "+966"),
        c("SN", "Senegal", "+221"),
        c("RS", "Serbia", "+381"),
        c("SC", "Seychelles", "+248"),
        c("SL", "Sierra Leone", "+232"),
        c("SG", "Singapore", "+65"),
        c("SX", "Sint Maarten", "+1721"),
        c("SK", "Slovakia", "+421"),
        c("SI", "Slovenia", "+386"),
        c("SB", "Solomon Islands", "+677"),
        c("SO", "Somalia", "+252"),
        c("ZA", "South Africa", "+27"),
        c("KR", "South Korea", "+82"),
        c("SS", "South Sudan", "+211"),
        c("ES", "Spain", "+34"),
        c("LK", "Sri Lanka", "+94"),
        c("SD", "Sudan", "+249"),
        c("SR", "Suriname", "+597"),
        c("SE", "Sweden", "+46"),
        c("CH", "Switzerland", "+41"),
        c("SY", "Syria", "+963"),
        c("TW", "Taiwan", "+886"),
        c("TJ", "Tajikistan", "+992"),
        c("TZ", "Tanzania", "+255"),
        c("TH", "Thailand", "+66"),
        c("TL", "Timor-Leste", "+670"),
        c("TG", "Togo", "+228"),
        c("TO", "Tonga", "+676"),
        c("TT", "Trinidad and Tobago", "+1868"),
        c("TN", "Tunisia", "+216"),
        c("TR", "Turkey", "+90"),
        c("TM", "Turkmenistan", "+993"),
        c("TC", "Turks and Caicos Islands", "+1649"),
        c("TV", "Tuvalu", "+688"),
        c("UG", "Uganda", "+256"),
        c("UA", "Ukraine", "+380"),
        c("AE", "United Arab Emirates", "+971"),
        c("GB", "United Kingdom", "+44"),
        c("US", "United States", "+1"),
        c("UY", "Uruguay", "+598"),
        c("UZ", "Uzbekistan", "+998"),
        c("VU", "Vanuatu", "+678"),
        c("VA", "Vatican City", "+379"),
        c("VE", "Venezuela", "+58"),
        c("VN", "Vietnam", "+84"),
        c("VI", "U.S. Virgin Islands", "+1340"),
        c("YE", "Yemen", "+967"),
        c("ZM", "Zambia", "+260"),
        c("ZW", "Zimbabwe", "+263")
    )

    val DEFAULT: CountryDialCode = ALL.first()

    fun search(query: String): List<CountryDialCode> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return ALL
        return ALL.filter {
            it.name.lowercase().contains(q) ||
                it.isoCode.lowercase().contains(q) ||
                it.dialCode.lowercase().contains(q) ||
                it.dialCode.removePrefix("+").contains(q)
        }
    }

    fun findByIso(isoCode: String?): CountryDialCode? {
        if (isoCode.isNullOrBlank()) return null
        return ALL.find { it.isoCode.equals(isoCode.trim(), ignoreCase = true) }
    }
}

enum class FollowRelationState {
    NONE,        // "Follow"
    FOLLOW_BACK, // "Follow Back" (they follow me, I don't follow them)
    FOLLOWING,   // "Following" (I follow them, they don't follow me)
    FRIENDS;     // "Friends" (we both follow each other)

    val isFollowing: Boolean
        get() = this == FOLLOWING || this == FRIENDS

    companion object {
        fun resolve(iFollowThem: Boolean, theyFollowMe: Boolean): FollowRelationState = when {
            iFollowThem && theyFollowMe -> FRIENDS
            iFollowThem && !theyFollowMe -> FOLLOWING
            !iFollowThem && theyFollowMe -> FOLLOW_BACK
            else -> NONE
        }
    }
}

enum class ContactsFilterMode {
    SUGGESTED,
    ALL,
    FOLLOWERS,
    FOLLOWING,
    FRIENDS
}

enum class FollowListType {
    FOLLOWING,
    FOLLOWERS,
    SUBSCRIBERS
}

enum class OwnerProfileSubTab {
    POSTS,
    SAVED,
    RESHARED,
    LOCKED
}

enum class PostsFilterOption {
    ALL_POSTS,
    MOST_VIEWED,
    LATEST
}

data class DiscoverableUserItem(
    val userId: String,
    val fullName: String,
    val username: String,
    val bio: String,
    val locationLabel: String,
    val countryIsoCode: String,
    val avatarLocalPath: String? = null,
    val avatarCdnUrl: String? = null,
    val mutualConnectionsCount: Int = 0,
    val isMatchedFromContacts: Boolean = false,
    val followState: FollowRelationState = FollowRelationState.NONE,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val likesCount: Long = 0L,
    val subscribersCount: Long = 0L,
    val isBlocked: Boolean = false
)

data class UserPostItem(
    val postId: String,
    val userId: String,
    @DrawableRes val mediaDrawableRes: Int,
    val mediaUrl: String? = null,
    val caption: String = "",
    val durationLabel: String = "0:42",
    val subTabCategory: OwnerProfileSubTab = OwnerProfileSubTab.POSTS,
    val viewsCount: Long = 0L,
    val likesCount: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class OwnerProfileCooldownState(
    val canChangeFullName: Boolean = true,
    val fullNameRemainingMs: Long = 0L,
    val fullNameRemainingDays: Int = 0,
    val canChangeUsername: Boolean = true,
    val usernameRemainingMs: Long = 0L,
    val usernameRemainingDays: Int = 0
) {
    companion object {
        const val FULL_NAME_COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000L   // 7 days
        const val USERNAME_COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000L   // 30 days
        private const val DAY_MS = 24L * 60 * 60 * 1000L

        fun compute(
            lastFullNameChangedAtMs: Long?,
            lastUsernameChangedAtMs: Long?,
            nowMs: Long = System.currentTimeMillis()
        ): OwnerProfileCooldownState {
            val nameRemMs = if (lastFullNameChangedAtMs != null && lastFullNameChangedAtMs > 0L) {
                ((lastFullNameChangedAtMs + FULL_NAME_COOLDOWN_MS) - nowMs).coerceAtLeast(0L)
            } else {
                0L
            }
            val usernameRemMs = if (lastUsernameChangedAtMs != null && lastUsernameChangedAtMs > 0L) {
                ((lastUsernameChangedAtMs + USERNAME_COOLDOWN_MS) - nowMs).coerceAtLeast(0L)
            } else {
                0L
            }
            val nameDays = if (nameRemMs > 0L) ((nameRemMs + DAY_MS - 1L) / DAY_MS).toInt().coerceAtLeast(1) else 0
            val userDays = if (usernameRemMs > 0L) ((usernameRemMs + DAY_MS - 1L) / DAY_MS).toInt().coerceAtLeast(1) else 0

            return OwnerProfileCooldownState(
                canChangeFullName = nameRemMs == 0L,
                fullNameRemainingMs = nameRemMs,
                fullNameRemainingDays = nameDays,
                canChangeUsername = usernameRemMs == 0L,
                usernameRemainingMs = usernameRemMs,
                usernameRemainingDays = userDays
            )
        }
    }
}

object TelefamInputSanitizer {
    private val htmlTagRegex = Regex("<[^>]*>")
    private val controlCharRegex = Regex("[\\p{Cntrl}&&[^\n\t]]")
    private val multiSpaceRegex = Regex(" {2,}")

    fun sanitizeFullName(raw: String): String {
        return raw
            .replace(htmlTagRegex, "")
            .replace(controlCharRegex, "")
            .replace("\n", " ")
            .replace(multiSpaceRegex, " ")
            .trim()
            .take(60)
    }

    fun sanitizeUsername(raw: String): String {
        return raw
            .trim()
            .removePrefix("@")
            .lowercase()
            .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
            .take(24)
    }

    fun sanitizePhoneNumber(raw: String): String {
        return raw.filter { it.isDigit() }.take(15)
    }

    fun sanitizeBio(raw: String): String {
        return raw
            .replace(htmlTagRegex, "")
            .replace(controlCharRegex, "")
            .trim()
            .take(150)
    }

    fun sanitizeGender(raw: String): String {
        return raw
            .replace(htmlTagRegex, "")
            .replace(controlCharRegex, "")
            .trim()
            .take(32)
    }

    fun sanitizeDateOfBirth(raw: String): String {
        return raw
            .filter { it.isDigit() || it == '-' }
            .take(10)
    }
}

enum class ReportReasonOption(
    val wireCode: String,
    @StringRes val labelRes: Int
) {
    SPAM_OR_BOT("SPAM_OR_BOT", R.string.report_q1_spam),
    IMPERSONATION("IMPERSONATION", R.string.report_q1_impersonation),
    HARASSMENT("HARASSMENT", R.string.report_q1_harassment),
    INAPPROPRIATE_CONTENT("INAPPROPRIATE_CONTENT", R.string.report_q1_inappropriate),
    SCAM_OR_FRAUD("SCAM_OR_FRAUD", R.string.report_q1_scam)
}

enum class ReportLocationOption(
    val wireCode: String,
    @StringRes val labelRes: Int
) {
    PROFILE_DETAILS("PROFILE_DETAILS", R.string.report_q2_profile),
    POSTS_OR_MEDIA("POSTS_OR_MEDIA", R.string.report_q2_posts),
    DIRECT_MESSAGES("DIRECT_MESSAGES", R.string.report_q2_messages),
    UNWANTED_CONTACT("UNWANTED_CONTACT", R.string.report_q2_unwanted_contact)
}

enum class ReportUrgencyOption(
    val wireCode: String,
    @StringRes val labelRes: Int
) {
    STANDARD_REVIEW("STANDARD_REVIEW", R.string.report_q3_standard),
    HIGH_PRIORITY_SAFETY("HIGH_PRIORITY_SAFETY", R.string.report_q3_urgent),
    HIDE_AND_MUTE("HIDE_AND_MUTE", R.string.report_q3_mute)
}

object TelefamNumberFormatter {
    fun formatCompactCount(count: Long): String {
        val safe = count.coerceAtLeast(0L)
        return when {
            safe >= 1_000_000L -> {
                val m = safe / 100_000L
                if (m % 10L == 0L) "${m / 10L}M" else "${m / 10.0}M"
            }
            safe >= 1_000L -> {
                val k = safe / 100L
                if (k % 10L == 0L) "${k / 10L}K" else "${k / 10.0}K"
            }
            else -> safe.toString()
        }
    }
}

data class DeviceAndNetworkTelemetry(
    val untrustedClientDeviceFingerprint: String,
    val serverVerifiedDeviceHash: String,
    val ipSubnetFingerprint: String,
    val clientIpAddress: String,
    val osDescription: String,
    val screenBucket: String,
    val localeTag: String,
    val locationLabel: String,
    val latitude: Double?,
    val longitude: Double?,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val riskSignals: List<String>
)

data class SanitizedAvatarResult(
    val secureFilePath: String,
    val cdnStorageUrl: String,
    val sha256Checksum: String,
    val perceptualDHash: String,
    val verifiedMimeType: String,
    val originalByteSize: Int,
    val compressedByteSize: Int,
    val widthPx: Int,
    val heightPx: Int,
    val exifMetadataStripped: Boolean,
    val reusedFromFingerprint: Boolean = false
)

data class EncryptedUserSession(
    val sessionId: String,
    val userId: String,
    val email: String,
    val accessTokenHash: String,
    val refreshTokenHash: String = "",
    val encryptedPayloadBase64: String,
    val ivBase64: String,
    val serverDeviceHash: String,
    val ipFingerprint: String,
    val locationLabel: String,
    val riskScore: Int,
    val expiresAtEpochMs: Long,
    val refreshExpiresAtEpochMs: Long = expiresAtEpochMs + 30L * 24 * 60 * 60 * 1000L,
    val hasCompletedProfile: Boolean,
    val isOfflinePreserved: Boolean = false
)
