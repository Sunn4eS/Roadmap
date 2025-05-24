package com.example.roadMap.data.dataBase

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.room.TypeConverters
import com.example.roadMap.data.dao.MapPointDao
import com.example.roadMap.data.dao.UserDao
import com.example.roadMap.data.database.UriConverters
import com.example.roadMap.data.module.MapPoint
import com.example.roadMap.data.module.User

@Database(entities = [User::class, MapPoint::class], version = 3)
@TypeConverters(UriConverters::class)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}