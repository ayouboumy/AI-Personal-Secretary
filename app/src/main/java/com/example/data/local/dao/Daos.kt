package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY createdAtMillis DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAtMillis DESC")
    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdSync(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deadlineMillis IS NOT NULL AND deadlineMillis BETWEEN :startMillis AND :endMillis AND status != 'COMPLETED' ORDER BY deadlineMillis ASC")
    fun getUpcomingDeadlines(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC")
    fun getSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC")
    suspend fun getSubtasksForTaskSync(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: String)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY startTimeMillis ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startTimeMillis BETWEEN :startMillis AND :endMillis ORDER BY startTimeMillis ASC")
    fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CalendarEventEntity)

    @Delete
    suspend fun delete(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CapturedItemDao {
    @Query("SELECT * FROM captured_items ORDER BY createdAtMillis DESC")
    fun getAllItems(): Flow<List<CapturedItemEntity>>

    @Query("SELECT * FROM captured_items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<CapturedItemEntity?>

    @Query("SELECT * FROM captured_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdSync(id: String): CapturedItemEntity?

    @Query("SELECT * FROM captured_items WHERE rawText = :rawText LIMIT 1")
    suspend fun findExistingByText(rawText: String): CapturedItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CapturedItemEntity)

    @Update
    suspend fun update(item: CapturedItemEntity)

    @Delete
    suspend fun delete(item: CapturedItemEntity)

    @Query("DELETE FROM captured_items WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_rules ORDER BY createdAtMillis DESC")
    fun getAllRules(): Flow<List<LearningRuleEntity>>

    @Query("SELECT * FROM learning_rules WHERE isActive = 1")
    suspend fun getActiveRulesSync(): List<LearningRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: LearningRuleEntity)

    @Update
    suspend fun updateRule(rule: LearningRuleEntity)

    @Delete
    suspend fun deleteRule(rule: LearningRuleEntity)

    @Query("SELECT * FROM correction_records ORDER BY timestampMillis DESC")
    fun getAllCorrections(): Flow<List<CorrectionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: CorrectionRecordEntity)

    @Query("DELETE FROM learning_rules")
    suspend fun clearAllRules()

    @Query("DELETE FROM correction_records")
    suspend fun clearAllCorrections()
}

@Dao
interface AssistantDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("SELECT * FROM ai_processing_logs ORDER BY timestampMillis DESC")
    fun getAllLogs(): Flow<List<AIProcessingLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AIProcessingLogEntity)

    @Query("DELETE FROM ai_processing_logs")
    suspend fun clearAllLogs()
}
