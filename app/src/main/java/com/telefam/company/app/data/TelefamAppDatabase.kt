package com.telefam.company.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ProfileEntity::class,
        MediaFingerprintEntity::class,
        TrustedDeviceEntity::class,
        OtpChallengeEntity::class,
        EncryptedSessionEntity::class,
        RateLimitBucketEntity::class,
        ForensicAuditLogEntity::class,
        PrivacySettingsEntity::class,
        FollowEdgeEntity::class,
        ContactHashEntity::class,
        UserBlockEntity::class,
        UserReportEntity::class,
        UserPostEntity::class,
        UserSubscriptionEntity::class,
        SocialSyncQueueEntity::class,
        AccountSettingsAndSecurityEntity::class,
        ConnectedAppSessionEntity::class,
        ProblemSupportTicketEntity::class,
        QuickLoginAccountEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class TelefamAppDatabase : RoomDatabase() {
    abstract fun telefamDao(): TelefamDao

    companion object {
        @Volatile
        private var INSTANCE: TelefamAppDatabase? = null

        fun getInstance(context: Context): TelefamAppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TelefamAppDatabase::class.java,
                    "telefam_company_production.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
