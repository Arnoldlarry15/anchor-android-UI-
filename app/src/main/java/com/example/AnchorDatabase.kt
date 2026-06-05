package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val isPendingApproval: Boolean
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isPendingApproval = 1")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isPendingApproval = 0 ORDER BY id DESC LIMIT 5")
    fun getRecentCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isPendingApproval = 0 ORDER BY id DESC")
    fun getAllCompletedTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET isPendingApproval = 0 WHERE id = :taskId")
    suspend fun approveTask(taskId: Int)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Int)
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anchor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TaskRepository(private val taskDao: TaskDao) {
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    val recentCompletedTasks: Flow<List<TaskEntity>> = taskDao.getRecentCompletedTasks()
    val allCompletedTasks: Flow<List<TaskEntity>> = taskDao.getAllCompletedTasks()

    suspend fun insert(task: TaskEntity) = taskDao.insertTask(task)
    suspend fun approveTask(id: Int) = taskDao.approveTask(id)
    suspend fun deleteTask(id: Int) = taskDao.deleteTask(id)
}
