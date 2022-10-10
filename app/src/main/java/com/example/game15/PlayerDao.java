package com.example.game15;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PlayerDao {

    @Query("SELECT _id, name, numberOfSteps, strftime('%d.%m.%Y', gameDate, 'unixepoch') as gameDate FROM player ORDER BY numberOfSteps, gameDate, name")
    Cursor getAll();

    @Query("SELECT * FROM player WHERE _id = :id")
    Player getById(long id);

    @Insert
    void insert(Player player);

    @Update
    void update(Player player);

    @Delete
    void delete(Player player);

    @Query("DELETE FROM player")
    void clear();
}
