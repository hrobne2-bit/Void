package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ThemeRepository(private val themeDao: ThemeDao) {

    val allThemes: Flow<List<ThemeEntity>> = themeDao.getAllThemes()
    val activeTheme: Flow<ThemeEntity?> = themeDao.getActiveTheme()

    suspend fun getActiveThemeDirect(): ThemeEntity? = themeDao.getActiveThemeDirect()

    suspend fun insertTheme(theme: ThemeEntity): Long = themeDao.insertTheme(theme)

    suspend fun updateTheme(theme: ThemeEntity) = themeDao.updateTheme(theme)

    suspend fun deleteTheme(theme: ThemeEntity) = themeDao.deleteTheme(theme)

    suspend fun setActiveTheme(id: Int) = themeDao.setActiveTheme(id)

    suspend fun checkAndSeedDefaults() {
        if (themeDao.getCount() == 0) {
            themeDao.insertTheme(ThemeEntity.createDefault())
            themeDao.insertTheme(ThemeEntity.createNord())
            themeDao.insertTheme(ThemeEntity.createNebula())
        }
    }
}
