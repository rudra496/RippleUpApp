package com.yft.rippleup.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, RippleEntity::class, PrefEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun rippleDao(): RippleDao
    abstract fun prefDao(): PrefDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: runCatching {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rippleup.db")
                    .fallbackToDestructiveMigration()
                    .build()
            }.getOrElse {
                // Last-resort in-memory DB so the app never hard-crashes on storage failure.
                Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java).build()
            }.also { instance = it }
        }
    }
}
