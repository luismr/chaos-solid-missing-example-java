package com.example.chaos.infra;
import com.example.chaos.model.User;
public class BaseUserRepository {
    public User getById(String id) { return new User(id, "fake@" + id + ".example.com"); }
    public void save(User u) {}
}