package com.example.roadMap.data.dataBase

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.room.TypeConverters
import com.example.roadMap.data.dao.MapPointDao
import com.example.roadMap.data.dao.UserDao
import com.example.roadMap.data.model.MapPoint
import com.example.roadMap.data.model.User
import com.example.roadMap.data.utilities.StringListConverter

@Database(entities = [User::class, MapPoint::class], version = 5)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun mapPointDao(): MapPointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}