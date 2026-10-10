-- =============================================================================
-- TELEFAM COMPANY PRODUCTION POSTGRESQL SCHEMA (telefam.company)
-- Strict parameterized SQL enforcement, zero-trust device & IP telemetry,
-- brute-force cooldown locks, OTP rate limits, and forensic audit logs.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS telefam_users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    google_sub_id VARCHAR(128) UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    login_locked_until TIMESTAMPTZ,
    failed_otp_attempts INT NOT NULL DEFAULT 0,
    otp_locked_until TIMESTAMPTZ,
    last_known_country VARCHAR(8),
    last_known_latitude DOUBLE PRECISION,
    last_known_longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_users_email ON telefam_users(email);

-- 2. USER PROFILES TABLE
CREATE TABLE IF NOT EXISTS telefam_profiles (
    user_id UUID PRIMARY KEY REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(32) NOT NULL UNIQUE,
    phone_country_code VARCHAR(8) NOT NULL,
    phone_number VARCHAR(24) NOT NULL,
    gender VARCHAR(32) NOT NULL,
    date_of_birth DATE NOT NULL,
    bio VARCHAR(150) NOT NULL DEFAULT '',
    avatar_storage_key TEXT,
    avatar_sha256_checksum VARCHAR(64),
    avatar_perceptual_dhash VARCHAR(16),
    avatar_mime_type VARCHAR(32),
    avatar_byte_size INT,
    sandbox_verified_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_profiles_username ON telefam_profiles(username);

-- 2B. PHOTO FINGERPRINTS VAULT FOR DEDUPLICATION & FUTURE REUSE
CREATE TABLE IF NOT EXISTS telefam_media_fingerprints (
    sha256_checksum VARCHAR(64) PRIMARY KEY,
    perceptual_dhash VARCHAR(16) NOT NULL,
    storage_key TEXT NOT NULL,
    local_secure_path TEXT,
    verified_mime_type VARCHAR(32) NOT NULL,
    compressed_byte_size INT NOT NULL,
    width_px INT NOT NULL DEFAULT 512,
    height_px INT NOT NULL DEFAULT 512,
    reuse_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_reused_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_media_dhash ON telefam_media_fingerprints(perceptual_dhash);

-- 3. TRUSTED DEVICES & UNTRUSTED CLIENT TELEMETRY BINDINGS
-- Note: Client-supplied fingerprint is never trusted alone; the backend computes
-- a server-bound composite hash combining TLS JA3/ALPN, IP ASN/subnet, and HMAC token.
CREATE TABLE IF NOT EXISTS telefam_devices (
    device_record_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    client_reported_fingerprint VARCHAR(128) NOT NULL,
    server_derived_device_hash VARCHAR(128) NOT NULL,
    ip_subnet_fingerprint VARCHAR(64) NOT NULL,
    user_agent TEXT NOT NULL,
    os_version VARCHAR(64) NOT NULL,
    screen_bucket VARCHAR(32) NOT NULL,
    is_trusted BOOLEAN NOT NULL DEFAULT FALSE,
    last_latitude DOUBLE PRECISION,
    last_longitude DOUBLE PRECISION,
    last_location_label VARCHAR(128),
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, server_derived_device_hash)
);

-- 4. ONE-TIME PASSWORDS (OTP) WITH EXPIRATION & INVALIDATION
CREATE TABLE IF NOT EXISTS telefam_otp_challenges (
    challenge_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(32) NOT NULL, -- SIGNUP, FORGOT_PASSWORD, NEW_DEVICE_LOGIN, STEP_UP_AUTH
    otp_hmac_digest VARCHAR(128) NOT NULL,
    ip_fingerprint VARCHAR(64) NOT NULL,
    server_device_hash VARCHAR(128) NOT NULL,
    verify_attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    is_invalidated BOOLEAN NOT NULL DEFAULT FALSE,
    consumed_at TIMESTAMPTZ,
    resend_available_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_otp_lookup ON telefam_otp_challenges(email, purpose, is_invalidated);

-- 5. ENCRYPTED SESSIONS TABLE
CREATE TABLE IF NOT EXISTS telefam_encrypted_sessions (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    access_token_sha256 VARCHAR(64) NOT NULL UNIQUE,
    refresh_token_sha256 VARCHAR(64) NOT NULL UNIQUE,
    encrypted_session_payload BYTEA NOT NULL, -- AES-256-GCM encrypted session state
    encryption_iv BYTEA NOT NULL,
    server_device_hash VARCHAR(128) NOT NULL,
    ip_fingerprint VARCHAR(64) NOT NULL,
    risk_score INT NOT NULL DEFAULT 0,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. IP & USER RATE LIMIT WINDOWS (ANTI-HAMMERING & BRUTE FORCE)
CREATE TABLE IF NOT EXISTS telefam_rate_limits (
    bucket_key VARCHAR(160) PRIMARY KEY, -- e.g. ip_fp:action or email:action
    action_type VARCHAR(32) NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    window_started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cooldown_until TIMESTAMPTZ,
    last_request_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. FORENSIC SECURITY AUDIT LOGS & REAL-TIME ANOMALY TELEMETRY
CREATE TABLE IF NOT EXISTS telefam_forensic_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    event_category VARCHAR(48) NOT NULL, -- AUTH_LOGIN, AUTH_SIGNUP, OTP_SEND, OTP_VERIFY, NEW_DEVICE_ALERT, MEDIA_SANDBOX, RATE_LIMIT_BLOCK, ANOMALY_DETECTED
    event_outcome VARCHAR(24) NOT NULL,  -- SUCCESS, FAILURE, BLOCKED_COOLDOWN, STEP_UP_REQUIRED
    user_id UUID REFERENCES telefam_users(user_id) ON DELETE SET NULL,
    target_identifier VARCHAR(254),
    client_ip_address INET,
    ip_subnet_fingerprint VARCHAR(64) NOT NULL,
    untrusted_client_device_fp VARCHAR(128) NOT NULL,
    server_verified_device_hash VARCHAR(128) NOT NULL,
    geo_location_label VARCHAR(128),
    geo_latitude DOUBLE PRECISION,
    geo_longitude DOUBLE PRECISION,
    computed_risk_score INT NOT NULL,
    risk_signals JSONB NOT NULL DEFAULT '[]'::jsonb,
    request_path VARCHAR(160) NOT NULL,
    http_method VARCHAR(12) NOT NULL,
    user_agent TEXT,
    forensic_metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_forensic_audit_time ON telefam_forensic_audit_logs(event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_forensic_audit_user ON telefam_forensic_audit_logs(user_id, event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_forensic_audit_ip ON telefam_forensic_audit_logs(ip_subnet_fingerprint, event_timestamp DESC);

-- 8. USER PRIVACY, CHAT BACKGROUND THEME & MESSAGE BUBBLE COLOUR SETTINGS
CREATE TABLE IF NOT EXISTS telefam_user_privacy_settings (
    user_id UUID PRIMARY KEY REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    message_requests VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (message_requests IN ('ANYONE', 'NOBODY')),
    who_can_call_me VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (who_can_call_me IN ('ANYONE', 'NOBODY')),
    who_can_screenshot_chats VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (who_can_screenshot_chats IN ('ANYONE', 'NOBODY')),
    who_can_share_chats VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (who_can_share_chats IN ('ANYONE', 'NOBODY')),
    who_can_copy_messages VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (who_can_copy_messages IN ('ANYONE', 'NOBODY')),
    who_can_download_media VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (who_can_download_media IN ('ANYONE', 'NOBODY')),
    chat_theme_id VARCHAR(64) NOT NULL DEFAULT 'crimson_silk',
    bubble_color_hex VARCHAR(16) NOT NULL DEFAULT '#C80815',
    read_receipts VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (read_receipts IN ('ANYONE', 'NOBODY')),
    typing_indicators VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (typing_indicators IN ('ANYONE', 'NOBODY')),
    online_indicators VARCHAR(16) NOT NULL DEFAULT 'ANYONE' CHECK (online_indicators IN ('ANYONE', 'NOBODY')),
    sync_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- 9. STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS) ENFORCEMENT
-- =============================================================================
ALTER TABLE telefam_user_privacy_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_privacy_settings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_privacy_select_own ON telefam_user_privacy_settings;
CREATE POLICY rls_privacy_select_own ON telefam_user_privacy_settings
    FOR SELECT
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY IF EXISTS rls_privacy_insert_own ON telefam_user_privacy_settings;
CREATE POLICY rls_privacy_insert_own ON telefam_user_privacy_settings
    FOR INSERT
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY IF EXISTS rls_privacy_update_own ON telefam_user_privacy_settings;
CREATE POLICY rls_privacy_update_own ON telefam_user_privacy_settings
    FOR UPDATE
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY IF EXISTS rls_privacy_delete_own ON telefam_user_privacy_settings;
CREATE POLICY rls_privacy_delete_own ON telefam_user_privacy_settings
    FOR DELETE
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

ALTER TABLE telefam_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_profiles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_profiles_owner_modify ON telefam_profiles;
CREATE POLICY rls_profiles_owner_modify ON telefam_profiles
    FOR ALL
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

ALTER TABLE telefam_encrypted_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_encrypted_sessions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_sessions_owner_isolation ON telefam_encrypted_sessions;
CREATE POLICY rls_sessions_owner_isolation ON telefam_encrypted_sessions
    FOR ALL
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- =============================================================================
-- 10. SOCIAL GRAPH: FOLLOW / FOLLOW BACK / FRIENDS WITH IDEMPOTENCY & INTERNAL RISK SCORE
-- =============================================================================
CREATE TABLE IF NOT EXISTS telefam_user_follows (
    follower_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    internal_risk_score INT NOT NULL DEFAULT 0, -- Internal-only security telemetry; never returned to client UI
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_user_id, target_user_id),
    CHECK (follower_user_id <> target_user_id)
);

CREATE INDEX IF NOT EXISTS idx_telefam_follows_follower ON telefam_user_follows(follower_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_telefam_follows_target ON telefam_user_follows(target_user_id, created_at DESC);

ALTER TABLE telefam_user_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_follows FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_follows_read_all ON telefam_user_follows;
CREATE POLICY rls_follows_read_all ON telefam_user_follows
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS rls_follows_write_own ON telefam_user_follows;
CREATE POLICY rls_follows_write_own ON telefam_user_follows
    FOR ALL
    USING (follower_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (follower_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- =============================================================================
-- 11. CONTACTS DISCOVERY SHA-256 HASHES (PHONE & EMAIL MATCHING)
-- =============================================================================
CREATE TABLE IF NOT EXISTS telefam_contact_hashes (
    owner_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    contact_hash_sha256 VARCHAR(64) NOT NULL,
    hash_type VARCHAR(16) NOT NULL CHECK (hash_type IN ('PHONE', 'EMAIL')),
    matched_user_id UUID REFERENCES telefam_users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (owner_user_id, contact_hash_sha256)
);

CREATE INDEX IF NOT EXISTS idx_telefam_contact_hashes_lookup ON telefam_contact_hashes(contact_hash_sha256);

ALTER TABLE telefam_contact_hashes ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_contact_hashes FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_contact_hashes_owner_only ON telefam_contact_hashes;
CREATE POLICY rls_contact_hashes_owner_only ON telefam_contact_hashes
    FOR ALL
    USING (owner_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (owner_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- =============================================================================
-- 12. USER BLOCKS & MULTI-QUESTION SAFETY REPORTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS telefam_user_blocks (
    blocker_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    blocked_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (blocker_user_id, blocked_user_id),
    CHECK (blocker_user_id <> blocked_user_id)
);

ALTER TABLE telefam_user_blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_blocks FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_blocks_owner_only ON telefam_user_blocks;
CREATE POLICY rls_blocks_owner_only ON telefam_user_blocks
    FOR ALL
    USING (blocker_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (blocker_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

CREATE TABLE IF NOT EXISTS telefam_user_reports (
    report_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    reported_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    reason_code VARCHAR(48) NOT NULL,
    location_code VARCHAR(48) NOT NULL,
    urgency_code VARCHAR(48) NOT NULL,
    additional_notes VARCHAR(300) NOT NULL DEFAULT '',
    internal_risk_score INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE telefam_user_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_reports FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_reports_insert_own ON telefam_user_reports;
CREATE POLICY rls_reports_insert_own ON telefam_user_reports
    FOR ALL
    USING (reporter_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (reporter_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- =============================================================================
-- 13. USER POSTS WITH VIEWS & LIKES COUNTERS
-- =============================================================================
CREATE TABLE IF NOT EXISTS telefam_user_posts (
    post_id VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    media_theme_key VARCHAR(64) NOT NULL,
    caption VARCHAR(500) NOT NULL DEFAULT '',
    duration_label VARCHAR(16) NOT NULL DEFAULT '0:42',
    post_category VARCHAR(24) NOT NULL DEFAULT 'POSTS' CHECK (post_category IN ('POSTS', 'SAVED', 'RESHARED', 'LOCKED')),
    views_count BIGINT NOT NULL DEFAULT 0,
    likes_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_posts_user ON telefam_user_posts(user_id, created_at DESC);

ALTER TABLE telefam_user_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_posts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_posts_read_all ON telefam_user_posts;
CREATE POLICY rls_posts_read_all ON telefam_user_posts
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS rls_posts_owner_write ON telefam_user_posts;
CREATE POLICY rls_posts_owner_write ON telefam_user_posts
    FOR ALL
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- =============================================================================
-- 14. OWNER PROFILE COOLDOWN TIMESTAMPS (7-DAY NAME / 30-DAY USERNAME) & SUBSCRIBERS
-- =============================================================================
ALTER TABLE telefam_profiles
    ADD COLUMN IF NOT EXISTS last_full_name_changed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_username_changed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS subscribers_count BIGINT NOT NULL DEFAULT 0;

DROP POLICY IF EXISTS rls_profiles_public_read ON telefam_profiles;
CREATE POLICY rls_profiles_public_read ON telefam_profiles
    FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS telefam_user_subscriptions (
    subscriber_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    creator_user_id UUID NOT NULL REFERENCES telefam_users(user_id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (subscriber_user_id, creator_user_id),
    CHECK (subscriber_user_id <> creator_user_id)
);

CREATE INDEX IF NOT EXISTS idx_telefam_subscriptions_creator ON telefam_user_subscriptions(creator_user_id, created_at DESC);

ALTER TABLE telefam_user_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_subscriptions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_subscriptions_read_all ON telefam_user_subscriptions;
CREATE POLICY rls_subscriptions_read_all ON telefam_user_subscriptions
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS rls_subscriptions_write_own ON telefam_user_subscriptions;
CREATE POLICY rls_subscriptions_write_own ON telefam_user_subscriptions
    FOR ALL
    USING (subscriber_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (subscriber_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);


