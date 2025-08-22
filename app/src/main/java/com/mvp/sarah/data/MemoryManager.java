package com.mvp.sarah.data;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemoryManager {
    private static volatile MemoryManager INSTANCE;
    private final UserActionDao userActionDao;
    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private MemoryManager(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.userActionDao = db.userActionDao();
    }

    public static MemoryManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MemoryManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MemoryManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public void logAction(String command) {
        databaseWriteExecutor.execute(() -> {
            UserAction userAction = new UserAction(command, System.currentTimeMillis());
            userActionDao.insert(userAction);
            Log.d("MemoryManager", "Logged action: " + command);
        });
    }

    // This method will be used in the future for prediction.
    // It needs to be called from a background thread.
    public List<UserAction> getActionHistory() {
        return userActionDao.getAllActions();
    }
}
