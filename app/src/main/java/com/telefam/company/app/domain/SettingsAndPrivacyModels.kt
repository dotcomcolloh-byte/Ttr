package com.telefam.company.app.domain

enum class SettingsSubScreen {
    ROOT,
    // 1. Account & Subscreens
    ACCOUNT,
    ACCOUNT_EDIT_USERNAME,
    ACCOUNT_EDIT_NAME,
    ACCOUNT_EDIT_EMAIL,
    ACCOUNT_EDIT_PHONE,
    ACCOUNT_CHANGE_PASSWORD,
    ACCOUNT_RECOVERY,
    ACCOUNT_DEACTIVATE,
    ACCOUNT_DELETE,
    // 2. Privacy (Account section)
    PRIVACY_ACCOUNT,
    // 3. Security & Permissions
    SECURITY_AND_PERMISSIONS,
    SECURITY_TWO_STEP,
    SECURITY_PASSKEY,
    SECURITY_MANAGE_DEVICES,
    SECURITY_LOGIN_HISTORY,
    SECURITY_ALERTS,
    SECURITY_PERMISSION_DETAIL,
    SECURITY_CONNECTED_APPS,
    // 4. Share Profile (QR Code)
    SHARE_PROFILE,
    // 5. Notifications
    NOTIFICATIONS,
    NOTIFICATIONS_PUSH_DETAIL,
    NOTIFICATIONS_EMAIL_DETAIL,
    // 6. Content Preferences
    CONTENT_PREFERENCES,
    CONTENT_LANGUAGES,
    CONTENT_TOPICS,
    CONTENT_SUGGESTED,
    CONTENT_VIDEO_QUALITY,
    CONTENT_AUTOPLAY,
    CONTENT_FILTERING,
    // 7. Privacy Controls (Privacy and safety section)
    PRIVACY_CONTROLS,
    PRIVACY_RULE_SELECTOR,
    // 8. Blocked Accounts
    BLOCKED_ACCOUNTS,
    // 9. Report a Problem
    REPORT_PROBLEM,
    // 10. Safety Center
    SAFETY_CENTER,
    SAFETY_TOPIC_DETAIL,
    // 11. Help & Support
    HELP_AND_SUPPORT,
    HELP_ARTICLE_DETAIL,
    HELP_CONTACT_SUPPORT,
    HELP_MY_REQUESTS,
    // 12. Terms and Policies
    TERMS_AND_POLICIES,
    LEGAL_DOCUMENT_DETAIL,
    // 13. About
    ABOUT,
    ABOUT_DETAIL,
    // 14. Appearance / Theme
    APPEARANCE_THEME
}

enum class AppThemeOption(
    val wireCode: String,
    val label: String,
    val description: String
) {
    SYSTEM(
        wireCode = "SYSTEM",
        label = "System preference",
        description = "Automatically match your device's light or dark display setting."
    ),
    LIGHT(
        wireCode = "LIGHT",
        label = "Light",
        description = "Use a clean, bright appearance across all screens."
    ),
    DARK(
        wireCode = "DARK",
        label = "Dark",
        description = "Use a low-glare dark appearance designed for comfortable viewing."
    );

    companion object {
        fun fromWire(value: String?): AppThemeOption {
            return entries.firstOrNull { it.wireCode.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

enum class PrivacyControlField(
    val title: String,
    val questionTitle: String,
    val explanation: String,
    val usesOnlyMeOption: Boolean
) {
    WHO_CAN_SEE_VIDEOS(
        title = "Who can see my videos",
        questionTitle = "Who can see your videos?",
        explanation = "Control who can view the videos and posts you publish on your profile.",
        usesOnlyMeOption = true
    ),
    WHO_CAN_SEE_LIKES(
        title = "Who can see my likes",
        questionTitle = "Who can see your liked content?",
        explanation = "Choose who can view the list of posts and videos you have liked.",
        usesOnlyMeOption = true
    ),
    WHO_CAN_SEE_FOLLOW_LIST(
        title = "Who can see my following and followers list",
        questionTitle = "Who can see your following and followers list?",
        explanation = "Decide who is allowed to browse your followers and the accounts you follow.",
        usesOnlyMeOption = true
    ),
    COMMENTS(
        title = "Comments",
        questionTitle = "Who can comment?",
        explanation = "Choose who is allowed to leave comments on your posts and videos.",
        usesOnlyMeOption = false
    ),
    MENTIONS(
        title = "Mentions",
        questionTitle = "Who can mention you?",
        explanation = "Control who can @mention your username in captions, comments, and discussions.",
        usesOnlyMeOption = false
    ),
    TAGS(
        title = "Tags",
        questionTitle = "Who can tag you?",
        explanation = "Choose who can tag your account in photos, videos, and community posts.",
        usesOnlyMeOption = false
    ),
    MESSAGES(
        title = "Messages",
        questionTitle = "Who can send you direct messages?",
        explanation = "Manage who can initiate direct message conversations with your account.",
        usesOnlyMeOption = false
    ),
    DOWNLOADS(
        title = "Downloads",
        questionTitle = "Who can download your content?",
        explanation = "Allow or restrict other people from saving a copy of your shared videos and media.",
        usesOnlyMeOption = false
    ),
    REUSE_REMIX(
        title = "Reuse/remix",
        questionTitle = "Who can reuse or remix your content?",
        explanation = "Choose who can create remixes, duets, or derivative posts with your content.",
        usesOnlyMeOption = false
    )
}

enum class SocialPrivacyOption(val wireCode: String, val label: String, val subtitle: String) {
    EVERYONE("EVERYONE", "Everyone", "Anyone on Telefam can view or interact"),
    FOLLOWERS("FOLLOWERS", "Followers", "Only people who follow your account"),
    FRIENDS("FRIENDS", "Friends", "Followers that you also follow back"),
    NO_ONE("NO_ONE", "No one", "Turned off for all accounts"),
    ONLY_ME("ONLY_ME", "Only me", "Visible exclusively to you");

    companion object {
        fun fromWire(code: String, usesOnlyMe: Boolean): SocialPrivacyOption {
            return entries.firstOrNull { it.wireCode.equals(code, ignoreCase = true) }
                ?: if (code == "NO_ONE" && usesOnlyMe) ONLY_ME
                else if (code == "ONLY_ME" && !usesOnlyMe) NO_ONE
                else EVERYONE
        }

        fun optionsFor(field: PrivacyControlField): List<SocialPrivacyOption> {
            return if (field.usesOnlyMeOption) {
                listOf(EVERYONE, FOLLOWERS, FRIENDS, ONLY_ME)
            } else {
                listOf(EVERYONE, FOLLOWERS, FRIENDS, NO_ONE)
            }
        }
    }
}

enum class AppPermissionType(
    val title: String,
    val summary: String,
    val detailedExplanation: String
) {
    CONTACTS(
        title = "Contacts",
        summary = "Suggest friends you may know",
        detailedExplanation = "Allow access to your contacts so we can suggest friends for you on Telefam. We only use contact information to help you find people you know, and your address book is never shared publicly or sold."
    ),
    CAMERA(
        title = "Camera",
        summary = "Capture profile photos, videos, and scan QR codes",
        detailedExplanation = "Camera access allows you to take profile photos, record video posts, and scan Telefam profile QR codes."
    ),
    MICROPHONE(
        title = "Microphone",
        summary = "Record voice messages, audio calls, and video sound",
        detailedExplanation = "Microphone access is used only when you actively record voice notes, capture video audio, or participate in a call."
    ),
    PHOTOS(
        title = "Photos",
        summary = "Select photos and videos for your profile and posts",
        detailedExplanation = "Photos access allows you to choose specific images or videos from your device when updating your profile photo, sharing posts, or saving QR codes."
    )
}

enum class DeactivationDurationOption(
    val days: Int,
    val label: String,
    val description: String
) {
    ONE_DAY(1, "Automatically re-enable in 24 hours", "Your account will reactivate automatically after 1 day."),
    THREE_DAYS(3, "Automatically re-enable in 3 days", "Your account will reactivate automatically after 3 days."),
    SEVEN_DAYS(7, "Automatically re-enable in 7 days", "Your account will reactivate automatically after 7 days."),
    FOURTEEN_DAYS(14, "Automatically re-enable in 14 days", "Your account will reactivate automatically after 14 days."),
    THIRTY_DAYS(30, "Automatically re-enable in 30 days", "Your account will reactivate automatically after 30 days."),
    MANUAL_LOGIN(0, "Until I sign back in", "Stay deactivated until you sign back into your account.")
}

data class SafetyTopicGuide(
    val id: String,
    val title: String,
    val subtitle: String,
    val overview: String,
    val bulletPoints: List<String>,
    val primaryActionLabel: String,
    val targetSubScreen: SettingsSubScreen,
    val secondaryActionLabel: String = "Report a safety concern",
    val secondarySubScreen: SettingsSubScreen = SettingsSubScreen.REPORT_PROBLEM
)

data class HelpSupportArticle(
    val id: String,
    val categoryTitle: String,
    val title: String,
    val summary: String,
    val steps: List<String>,
    val keywords: List<String>,
    val actionButtonLabel: String = "Open setting",
    val relatedSubScreen: SettingsSubScreen = SettingsSubScreen.ACCOUNT
)

data class LegalPolicyDocument(
    val id: String,
    val title: String,
    val subtitle: String,
    val version: String,
    val effectiveDate: String,
    val sections: List<Pair<String, String>>,
    val relatedActionLabel: String = "Review privacy controls",
    val relatedSubScreen: SettingsSubScreen = SettingsSubScreen.PRIVACY_CONTROLS
)

object TelefamSettingsCatalog {

    val AVAILABLE_CONTENT_LANGUAGES = listOf(
        "English",
        "Swahili",
        "French",
        "Spanish",
        "Arabic"
    )

    val AVAILABLE_CONTENT_TOPICS = listOf(
        "Technology",
        "Community",
        "Education",
        "Music",
        "Sports",
        "Design & Art",
        "Business & Finance",
        "Travel & Nature",
        "Science",
        "Health & Wellness"
    )

    val PROBLEM_REPORT_CATEGORIES = listOf(
        "Account",
        "Login",
        "Video/content",
        "Comments",
        "Messages",
        "Notifications",
        "Payments",
        "Something else"
    )

    val DEACTIVATION_REASONS = listOf(
        "Taking a temporary break",
        "Focusing on work, study, or personal projects",
        "Adjusting privacy or notification preferences",
        "Receiving too many messages or notifications",
        "Other personal reason"
    )

    val DELETION_REASONS = listOf(
        "Created another Telefam account",
        "Privacy preferences",
        "Spending too much time on social apps",
        "Starting fresh with a new profile",
        "Other reason"
    )

    val SAFETY_GUIDES = listOf(
        SafetyTopicGuide(
            id = "protect_account",
            title = "Protect your account",
            subtitle = "Passwords, passkeys, and two-step verification",
            overview = "Keep your Telefam account secure from unauthorized access by combining a strong password with two-step verification and passkey sign-in.",
            bulletPoints = listOf(
                "Create a unique password with at least 12 characters, numbers, and symbols",
                "Turn on 2-step verification using your email address or an authenticator app",
                "Review your active devices regularly and log out any unfamiliar sessions",
                "Never share your verification codes or recovery codes with anyone",
                "Set up a passkey for fast, phishing-resistant sign-in"
            ),
            primaryActionLabel = "Manage 2-step verification",
            targetSubScreen = SettingsSubScreen.SECURITY_TWO_STEP,
            secondaryActionLabel = "Set up a passkey",
            secondarySubScreen = SettingsSubScreen.SECURITY_PASSKEY
        ),
        SafetyTopicGuide(
            id = "privacy_tips",
            title = "Privacy tips",
            subtitle = "Manage account visibility, interactions, and audience controls",
            overview = "You decide who can view your content, send you direct messages, or interact with your profile on Telefam.",
            bulletPoints = listOf(
                "Switch to a Private account so only approved followers can view your posts and videos",
                "Choose who can comment on your posts, mention your handle, or tag you",
                "Turn off Activity status whenever you prefer to browse privately",
                "Control whether others can download or remix your shared videos"
            ),
            primaryActionLabel = "Open privacy controls",
            targetSubScreen = SettingsSubScreen.PRIVACY_CONTROLS,
            secondaryActionLabel = "Manage private account",
            secondarySubScreen = SettingsSubScreen.PRIVACY_ACCOUNT
        ),
        SafetyTopicGuide(
            id = "avoid_scams",
            title = "Avoid scams",
            subtitle = "Recognize phishing attempts and account impersonation",
            overview = "Telefam will never ask for your password, verification code, or recovery codes in a direct message or unsolicited email.",
            bulletPoints = listOf(
                "Verify usernames and mutual connections carefully before responding to urgent requests",
                "Never enter your Telefam password or verification code on unfamiliar websites",
                "Enable passkey sign-in for added protection against phishing",
                "Report suspicious messages or impersonation accounts right away"
            ),
            primaryActionLabel = "Configure passkey sign-in",
            targetSubScreen = SettingsSubScreen.SECURITY_PASSKEY,
            secondaryActionLabel = "Report a suspicious account",
            secondarySubScreen = SettingsSubScreen.REPORT_PROBLEM
        ),
        SafetyTopicGuide(
            id = "blocking_reporting",
            title = "Blocking & reporting",
            subtitle = "Stop unwanted interactions and report policy violations",
            overview = "Blocking immediately stops an account from viewing your profile, sending you messages, or interacting with your posts.",
            bulletPoints = listOf(
                "Accounts are never notified when you block them",
                "Blocking removes follow connections between both accounts immediately",
                "Our Trust & Safety team reviews submitted reports around the clock",
                "You can view or unblock accounts at any time in Blocked accounts"
            ),
            primaryActionLabel = "Manage blocked accounts",
            targetSubScreen = SettingsSubScreen.BLOCKED_ACCOUNTS,
            secondaryActionLabel = "Submit a safety report",
            secondarySubScreen = SettingsSubScreen.REPORT_PROBLEM
        ),
        SafetyTopicGuide(
            id = "online_harassment",
            title = "Online harassment",
            subtitle = "Filter comments, restrict messages, and protect your space",
            overview = "We maintain strict standards against harassment and bullying. You can customize your interaction controls at any time.",
            bulletPoints = listOf(
                "Restrict Comments, Mentions, Tags, and Messages to Friends or No one",
                "Set Content filtering to Less sensitive content in Content preferences",
                "Attach screenshots when submitting a report so our team can take swift action"
            ),
            primaryActionLabel = "Report a safety issue",
            targetSubScreen = SettingsSubScreen.REPORT_PROBLEM,
            secondaryActionLabel = "Adjust interaction controls",
            secondarySubScreen = SettingsSubScreen.PRIVACY_CONTROLS
        ),
        SafetyTopicGuide(
            id = "account_security",
            title = "Account security",
            subtitle = "Monitor sign-in activity, recognized devices, and recovery options",
            overview = "Stay informed about every device signed into your account and keep your recovery contact information up to date.",
            bulletPoints = listOf(
                "Check Login history to review recent sign-ins and locations",
                "Log out unrecognized phones or computers immediately from Manage devices",
                "Save backup recovery codes in Account recovery in case you lose access to your phone"
            ),
            primaryActionLabel = "Review active devices",
            targetSubScreen = SettingsSubScreen.SECURITY_MANAGE_DEVICES,
            secondaryActionLabel = "View login history",
            secondarySubScreen = SettingsSubScreen.SECURITY_LOGIN_HISTORY
        ),
        SafetyTopicGuide(
            id = "community_guidelines",
            title = "Community guidelines",
            subtitle = "Our standards for respectful and authentic connection",
            overview = "Telefam is designed for safe, meaningful communication. Every member helps keep our community welcoming and respectful.",
            bulletPoints = listOf(
                "Treat others with respect—harassment, hate speech, and impersonation are not permitted",
                "Share original content or credit creators appropriately",
                "Report harmful or deceptive behavior using our in-app reporting tools"
            ),
            primaryActionLabel = "Read Community Guidelines",
            targetSubScreen = SettingsSubScreen.TERMS_AND_POLICIES,
            secondaryActionLabel = "Contact Trust & Safety",
            secondarySubScreen = SettingsSubScreen.HELP_CONTACT_SUPPORT
        )
    )

    val HELP_ARTICLES = listOf(
        HelpSupportArticle(
            id = "account_problems",
            categoryTitle = "Account",
            title = "Managing your username, email, deactivation, and 30-day recovery",
            summary = "Learn how to update your profile details, verify a new email address, temporarily deactivate, or recover an account scheduled for deletion.",
            steps = listOf(
                "Your name can be updated once every 7 days, and your unique @username can be updated once every 30 days.",
                "To update your email address, go to Settings and privacy > Account > Email. A 6-digit verification code will be sent to your current email address to confirm the change.",
                "When you deactivate your account, your profile and content are hidden immediately. You can select an automatic reactivation period or sign back in anytime to reactivate.",
                "When you request account deletion, your account enters a 30-day recovery period. Signing in within 30 days cancels the deletion and restores your account."
            ),
            keywords = listOf("account", "username", "name", "email", "phone", "delete", "deactivate", "recover", "30 days"),
            actionButtonLabel = "Open Account settings",
            relatedSubScreen = SettingsSubScreen.ACCOUNT
        ),
        HelpSupportArticle(
            id = "login_problems",
            categoryTitle = "Login & Security",
            title = "Signing in with 2-step verification, passkeys, and backup recovery codes",
            summary = "Learn how to sign in when 2-step verification is active, use a passkey, or regain access with a backup recovery code.",
            steps = listOf(
                "When 2-step verification is enabled, signing in with your password will prompt for a 6-digit verification code from your email or authenticator app.",
                "If you cannot access your verification code, you can enter one of your 8-character backup recovery codes on the sign-in verification screen.",
                "If you have registered a passkey in Security & permissions, tap 'Sign in with a passkey' on the login screen for one-tap sign-in.",
                "After 3 unsuccessful password attempts, password sign-in pauses for 15 minutes to protect your account. You can still sign in using a verification code sent to your email."
            ),
            keywords = listOf("login", "password", "otp", "code", "locked", "cooldown", "2fa", "two-step", "passkey", "recovery"),
            actionButtonLabel = "Open Security & permissions",
            relatedSubScreen = SettingsSubScreen.SECURITY_AND_PERMISSIONS
        ),
        HelpSupportArticle(
            id = "privacy_help",
            categoryTitle = "Privacy",
            title = "Controlling who can view your profile, videos, and interactions",
            summary = "Manage Private account mode and choose who can view your videos, likes, followers, comments, and messages.",
            steps = listOf(
                "Turning on Private account ensures that only approved followers can view your posts, videos, and restricted profile details.",
                "In Privacy controls, you can set separate visibility rules for your videos, liked content, and following/followers lists (Everyone, Followers, Friends, or Only me).",
                "You can also choose who is allowed to comment, mention you, tag you, send direct messages, or download your content."
            ),
            keywords = listOf("privacy", "private", "videos", "likes", "followers", "comments", "mentions", "tags", "downloads"),
            actionButtonLabel = "Open Privacy controls",
            relatedSubScreen = SettingsSubScreen.PRIVACY_CONTROLS
        ),
        HelpSupportArticle(
            id = "notifications_help",
            categoryTitle = "Notifications",
            title = "Managing push notifications, quiet hours, and email alerts",
            summary = "Customize which interactions, direct messages, and account updates send you notifications.",
            steps = listOf(
                "Go to Settings and privacy > Notifications to turn alerts on or off for Likes, Comments, New followers, Mentions, and Direct messages.",
                "Tap Push notifications to pause all notifications temporarily or schedule automatic quiet hours.",
                "Important security alerts—such as new device sign-ins or verification codes—are always delivered to help keep your account safe."
            ),
            keywords = listOf("notifications", "push", "email", "sms", "alerts", "likes", "messages", "quiet"),
            actionButtonLabel = "Open Notifications",
            relatedSubScreen = SettingsSubScreen.NOTIFICATIONS
        ),
        HelpSupportArticle(
            id = "appearance_content_help",
            categoryTitle = "Display & Content",
            title = "Switching between Light, Dark, and System theme and customizing content",
            summary = "Change your app appearance immediately and personalize languages, topics, video quality, and autoplay.",
            steps = listOf(
                "Go to Settings and privacy > Appearance to switch between System preference, Light mode, and Dark mode instantly.",
                "In Content preferences, select the languages and topics you enjoy to tailor your suggested content.",
                "Adjust Video quality (Auto, 1080p HD, or Data saver) and Autoplay behavior to match your network preferences."
            ),
            keywords = listOf("theme", "dark", "light", "system", "appearance", "content", "language", "video", "autoplay"),
            actionButtonLabel = "Open Appearance settings",
            relatedSubScreen = SettingsSubScreen.APPEARANCE_THEME
        ),
        HelpSupportArticle(
            id = "safety_help",
            categoryTitle = "Safety",
            title = "Blocking accounts, reporting problems, and managing active devices",
            summary = "Learn how to block unwanted accounts, submit a report with screenshots, or sign out unfamiliar devices.",
            steps = listOf(
                "Visit Blocked accounts at any time to review or unblock accounts you have restricted.",
                "Use Report a problem to describe an issue, attach a screenshot, and receive a reference number from our support team.",
                "Open Security & permissions > Manage devices to see where your account is signed in and log out any unfamiliar device."
            ),
            keywords = listOf("safety", "block", "unblock", "report", "harassment", "devices", "scam"),
            actionButtonLabel = "Open Safety center",
            relatedSubScreen = SettingsSubScreen.SAFETY_CENTER
        )
    )

    val LEGAL_DOCUMENTS = listOf(
        LegalPolicyDocument(
            id = "terms_of_service",
            title = "Terms of Service",
            subtitle = "Agreement governing your use of Telefam services and features",
            version = "Version 3.2",
            effectiveDate = "Effective Date: October 1, 2026",
            sections = listOf(
                "1. Eligibility & Account Responsibility" to "You must be at least 13 years old to create a Telefam account. You are responsible for maintaining accurate account information and safeguarding your password, passkeys, and verification codes.",
                "2. Your Content & Permissions" to "You retain ownership of the photos, videos, and messages you create and share on Telefam. By posting content, you grant Telefam a non-exclusive, revocable license to host, display, and deliver that content solely in accordance with your selected privacy settings.",
                "3. Acceptable Use & Platform Integrity" to "You agree not to misuse Telefam services, attempt unauthorized access, send automated spam, or interfere with the safety and experience of other members.",
                "4. Account Deactivation & 30-Day Deletion Recovery" to "You may deactivate your account temporarily or request permanent account deletion at any time in Account settings. When you schedule your account for deletion, Telefam provides a 30-day recovery window during which signing back in cancels deletion and restores your account. After 30 days, your account and associated content are permanently removed.",
                "5. Service Updates & Contact" to "We continually improve Telefam features and security controls. When we make material updates to these Terms, we will notify you in the app."
            ),
            relatedActionLabel = "Manage Account settings",
            relatedSubScreen = SettingsSubScreen.ACCOUNT
        ),
        LegalPolicyDocument(
            id = "privacy_policy",
            title = "Privacy Policy",
            subtitle = "How we collect, use, and protect your personal information",
            version = "Version 2.8",
            effectiveDate = "Effective Date: October 1, 2026",
            sections = listOf(
                "1. Information You Provide" to "We collect the account details you provide when registering and setting up your profile, including your name, username, email address, phone number, date of birth, and optional profile photo or bio.",
                "2. Contact Suggestions & Address Book Privacy" to "If you choose to allow access to your contacts, we use that information solely to suggest friends you may know on Telefam. Contact matching is performed securely, and your address book is never shared publicly or sold to third parties.",
                "3. Security & Sign-In Protection" to "To protect your account against unauthorized access, we record sign-in activity, recognized devices, and verification events. You can review your active devices and login history at any time in Security & permissions.",
                "4. Granular Visibility & Audience Controls" to "You control who can view your videos, liked content, and followers list, as well as who can comment, mention, tag, or message you. Changes you make in Privacy controls take effect immediately.",
                "5. Data Retention & Deletion Rights" to "We retain your account information for as long as your account remains active. You may update your information, revoke device permissions, or schedule permanent account deletion at any time."
            ),
            relatedActionLabel = "Review Privacy controls",
            relatedSubScreen = SettingsSubScreen.PRIVACY_CONTROLS
        ),
        LegalPolicyDocument(
            id = "community_guidelines",
            title = "Community Guidelines",
            subtitle = "Standards for safe, respectful, and authentic participation",
            version = "Version 2.5",
            effectiveDate = "Effective Date: September 15, 2026",
            sections = listOf(
                "1. Mutual Respect & Anti-Harassment" to "Telefam is a place for meaningful connection. Harassment, bullying, threats, hate speech, and non-consensual sharing of private information are strictly prohibited.",
                "2. Authentic Identity" to "Represent yourself honestly. Do not impersonate other people, creators, or organizations in a deceptive manner.",
                "3. Community Safety" to "Content that promotes violence, self-harm, illegal activities, or financial fraud is not allowed and will be removed promptly.",
                "4. Reporting & Enforcement" to "If you encounter content or behavior that violates our guidelines, use our in-app reporting tools or block the account. Our Trust & Safety team reviews reports and takes appropriate enforcement action."
            ),
            relatedActionLabel = "Open Safety center",
            relatedSubScreen = SettingsSubScreen.SAFETY_CENTER
        ),
        LegalPolicyDocument(
            id = "copyright_policy",
            title = "Copyright Policy",
            subtitle = "Intellectual property protection and creator rights",
            version = "Version 2.1",
            effectiveDate = "Effective Date: August 20, 2026",
            sections = listOf(
                "1. Respect for Original Creators" to "Only post photos, videos, and audio that you have created yourself or have permission to share. Always credit creators when remixing or collaborating.",
                "2. Content Download & Reuse Controls" to "You can control whether others are allowed to download or remix your posts under Settings and privacy > Privacy controls.",
                "3. Submitting a Copyright Notice" to "If you believe your copyrighted work has been posted on Telefam without authorization, you can submit a formal notice through Help & support so our team can review and remove infringing material."
            ),
            relatedActionLabel = "Contact Support team",
            relatedSubScreen = SettingsSubScreen.HELP_CONTACT_SUPPORT
        ),
        LegalPolicyDocument(
            id = "cookie_policy",
            title = "Cookie & Storage Policy",
            subtitle = "How Telefam uses session tokens and on-device storage",
            version = "Version 1.9",
            effectiveDate = "Effective Date: August 20, 2026",
            sections = listOf(
                "1. Authentication & Session Security" to "Telefam uses secure session credentials on your device to keep you signed in and protect your account without asking for your password every time you open the app.",
                "2. Preferences & Appearance Storage" to "We store your display preferences—such as Light or Dark theme, language selections, and notification settings—so your experience stays consistent even when your connection is interrupted.",
                "3. No Third-Party Advertising Trackers" to "Telefam does not use third-party advertising trackers or sell your browsing activity across other apps and websites."
            ),
            relatedActionLabel = "Manage Connected apps & sessions",
            relatedSubScreen = SettingsSubScreen.SECURITY_CONNECTED_APPS
        )
    )
}
