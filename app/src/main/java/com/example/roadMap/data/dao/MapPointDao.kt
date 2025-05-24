package com.example.roadMap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.roadMap.data.module.MapPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface MapPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapPoint(mapPoint: MapPoint) : Long

    @Query("SELECT * FROM map_points WHERE userId = :userId")
    fun getMapPointsForUser(userId: String): Flow<List<MapPoint>>

    @Query("SELECT * FROM map_points")
    fun getAllMapPoints(): Flow<List<MapPoint>>

    @Query("DELETE FROM map_points WHERE id = :pointId")
    suspend fun deleteMapPointById(pointId: Long): Int


}