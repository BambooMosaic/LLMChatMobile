package com.bamboomosaic.llmchat.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bamboomosaic.llmchat.core.data.db.AppDatabase
import java.io.File

/** 桌面端数据库：~/.llmchat/llmchat.db（bundled SQLite 驱动，与 Android 同一套实体/迁移）。 */
fun desktopDatabase(): AppDatabase {
    val dir = File(System.getProperty("user.home"), ".llmchat").apply { mkdirs() }
    return Room.databaseBuilder<AppDatabase>(
        name = File(dir, "llmchat.db").absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(*AppDatabase.ALL_MIGRATIONS)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
