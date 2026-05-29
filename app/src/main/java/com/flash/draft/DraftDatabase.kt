package com.flash.draft

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sticky_notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val x: Float,
    val y: Float,
    val text: String,
    val colorValue: Long = 0xFFFFF9C4, // Default yellow
    val width: Float = 200f,
    val height: Float = 200f
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM sticky_notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)
}

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class DraftDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: DraftDatabase? = null

        fun getDatabase(context: Context): DraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DraftDatabase::class.java,
                    "draft_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}