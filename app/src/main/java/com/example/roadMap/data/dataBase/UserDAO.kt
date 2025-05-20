package com.example.roadMap.data.dataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDAO {
    @Insert
    fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username")
    fun getUser(username: String): UserEntity?
}