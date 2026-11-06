package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AssistantDao
import com.example.data.local.dao.CalendarDao
import com.example.data.local.dao.CapturedItemDao
import com.example.data.local.dao.LearningDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AIProcessingLogEntity
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.local.entity.CapturedItemEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.CorrectionRecordEntity
import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        CalendarEventEntity::class,
        CapturedItemEntity::class,
        LearningRuleEntity::class,
        CorrectionRecordEntity::class,
        ChatMessageEntity::class,
        AIProcessingLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SecretaryDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun calendarDao(): CalendarDao
    abstract fun capturedItemDao(): CapturedItemDao
    abstract fun learningDao(): LearningDao
    abstract fun assistantDao(): AssistantDao

    companion object {
        @Volatile
        private var INSTANCE: SecretaryDatabase? = null

        fun getDatabase(context: Context): SecretaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecretaryDatabase::class.java,
                    "secretary_database.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
