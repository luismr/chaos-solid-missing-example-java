package com.example.chaos.infra;
import com.example.chaos.model.User;
public class ReadOnlyUserRepository extends BaseUserRepository {
    @Override public void save(User u) { throw new UnsupportedOperationException("ReadOnly repo cannot save"); }
}