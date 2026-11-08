package com.apexajay.memento.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MementoDao {
    @Query("SELECT * FROM daily_missions WHERE dayKey = :dayKey LIMIT 1")
    fun observeMissionForDay(dayKey: String): Flow<DailyMission?>

    @Query("SELECT * FROM daily_missions WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getMissionForDay(dayKey: String): DailyMission?

    @Upsert
    suspend fun upsertMission(mission: DailyMission)

    @Query("UPDATE daily_missions SET status = 'COMPLETED', completedAt = :completedAt WHERE dayKey = :dayKey")
    suspend fun completeMission(dayKey: String, completedAt: Long)

    @Query("SELECT * FROM focus_sessions WHERE status = 'RUNNING' ORDER BY startedAt DESC LIMIT 1")
    fun observeRunningSession(): Flow<FocusSession?>

    @Query("SELECT * FROM focus_sessions WHERE status = 'AWAITING_REVIEW' ORDER BY startedAt DESC LIMIT 1")
    fun observePendingSessionReview(): Flow<FocusSession?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFocusSession(session: FocusSession): Long

    @Query("UPDATE focus_sessions SET status = 'AWAITING_REVIEW', finishedAt = :finishedAt, actualSeconds = :actualSeconds WHERE id = :sessionId AND status = 'RUNNING'")
    suspend fun finishFocusSession(sessionId: Long, finishedAt: Long, actualSeconds: Int)

    @Query("UPDATE focus_sessions SET status = 'COMPLETE', productive = :productive WHERE id = :sessionId AND status = 'AWAITING_REVIEW'")
    suspend fun reviewFocusSession(sessionId: Long, productive: Boolean)

    @Query("UPDATE focus_sessions SET interruptionCount = interruptionCount + 1 WHERE id = :sessionId AND status = 'RUNNING'")
    suspend fun recordInterruption(sessionId: Long)

    @Query("SELECT * FROM focus_sessions WHERE startedAt >= :sinceMillis ORDER BY startedAt DESC")
    fun observeSessionsSince(sinceMillis: Long): Flow<List<FocusSession>>

    @Query("SELECT * FROM daily_missions WHERE createdAt >= :sinceMillis ORDER BY createdAt DESC")
    fun observeMissionsSince(sinceMillis: Long): Flow<List<DailyMission>>

    @Query("SELECT * FROM daily_reviews WHERE dayKey = :dayKey LIMIT 1")
    fun observeReview(dayKey: String): Flow<DailyReview?>

    @Upsert
    suspend fun upsertReview(review: DailyReview)
}
