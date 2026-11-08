package com.apexajay.memento.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyMission::class, FocusSession::class, DailyReview::class],
    version = 1,
    exportSchema = false
)
abstract class MementoDatabase : RoomDatabase() {
    abstract fun mementoDao(): MementoDao

    companion object {
        @Volatile
        private var instance: MementoDatabase? = null

        fun getInstance(context: Context): MementoDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MementoDatabase::class.java,
                    "memento.db"
                ).build().also { instance = it }
            }
        }
    }
}
