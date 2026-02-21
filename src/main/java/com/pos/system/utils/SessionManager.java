package com.pos.system.utils;

import com.pos.system.models.User;
import com.pos.system.models.Shift;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private Shift currentShift;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentShift(Shift shift) {
        this.currentShift = shift;
    }

    public Shift getCurrentShift() {
        return currentShift;
    }

    public void logout() {
        this.currentUser = null;
        this.currentShift = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
