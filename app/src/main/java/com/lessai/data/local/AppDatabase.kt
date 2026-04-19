package com.lessai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val fullText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "segments")
data class SegmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val index: Int,
    val originalText: String,
    val rewrittenText: String?,
    val status: String,
    val isApplied: Boolean = false
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity)
    
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    suspend fun getAllSessions(): List<SessionEntity>
    
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionEntity?
    
    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)
    
    @Insert
    suspend fun insertSegments(segments: List<SegmentEntity>)
    
    @Query("SELECT * FROM segments WHERE sessionId = :sessionId ORDER BY `index`")
    suspend fun getSegments(sessionId: String): List<SegmentEntity>
    
    @Update
    suspend fun updateSegment(segment: SegmentEntity)
    
    @Query("UPDATE segments SET rewrittenText = :text, status = :status WHERE id = :segmentId")
    suspend fun updateRewrite(segmentId: String, text: String, status: String)
}

@Database(entities = [SessionEntity::class, SegmentEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}