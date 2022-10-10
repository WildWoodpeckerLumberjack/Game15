package com.example.game15;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Player {

/*    public Player() {
    }*/

    public Player(String name, int numberOfSteps, long gameDate) {
        this._id = 0;
        this.name = name;
        this.numberOfSteps = numberOfSteps;
        this.gameDate = gameDate;
    }

    @PrimaryKey(autoGenerate = true)
    public long _id;
    public String name;
    public int numberOfSteps;
    public long gameDate;
}
