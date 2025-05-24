package com.example.roadMap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadMap.data.module.User

@Dao
interface UserDao {
    @Insert
    fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username")
    fun getUser(username: String): User?
}