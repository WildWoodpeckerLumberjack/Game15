package com.example.game15;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Player.class}, version = 1, exportSchema = false)
//@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract PlayerDao playerDao();
}
