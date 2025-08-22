package com.mvp.sarah.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserActionDao {
    @Insert
    void insert(UserAction userAction);

    @Query("SELECT * FROM user_actions ORDER BY timestamp DESC")
    List<UserAction> getAllActions();

    @Query("DELETE FROM user_actions")
    void clearAll();
}
