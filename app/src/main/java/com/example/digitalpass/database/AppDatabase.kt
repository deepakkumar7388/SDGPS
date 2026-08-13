package com.example.digitalpass.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [UserEntity::class, BatchEntity::class, NotificationEntity::class, CampusEntity::class, DepartmentEntity::class, GatePassEntity::class, VisitorEntity::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun batchDao(): BatchDao
    abstract fun notificationDao(): NotificationDao
    abstract fun campusDao(): CampusDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun gatePassDao(): GatePassDao
    abstract fun interInstitutionalGatePassDao(): InterInstitutionalGatePassDao
    abstract fun visitorDao(): VisitorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digitalpass_database"
                )
                .fallbackToDestructiveMigration()
                // Explicitly enable WAL mode on all Android versions.
                // This prevents older OS versions from locking the entire database
                // when a sync API call performs a bulk insert, allowing the home screen
                // to still read data concurrently without throwing an exception.
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
