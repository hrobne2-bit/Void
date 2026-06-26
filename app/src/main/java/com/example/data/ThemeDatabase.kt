package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes ORDER BY id DESC")
    fun getAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE isActive = 1 LIMIT 1")
    fun getActiveTheme(): Flow<ThemeEntity?>

    @Query("SELECT * FROM themes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveThemeDirect(): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity): Long

    @Update
    suspend fun updateTheme(theme: ThemeEntity)

    @Delete
    suspend fun deleteTheme(theme: ThemeEntity)

    @Query("UPDATE themes SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE themes SET isActive = 1 WHERE id = :id")
    suspend fun activateThemeById(id: Int)

    @Transaction
    suspend fun setActiveTheme(id: Int) {
        deactivateAll()
        activateThemeById(id)
    }

    @Query("SELECT COUNT(*) FROM themes")
    suspend fun getCount(): Int
}

@Database(entities = [ThemeEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun themeDao(): ThemeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "void_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
