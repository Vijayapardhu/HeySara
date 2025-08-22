package com.mvp.sarah.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_actions")
public class UserAction {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String command;

    public long timestamp;

    public UserAction(String command, long timestamp) {
        this.command = command;
        this.timestamp = timestamp;
    }
}
