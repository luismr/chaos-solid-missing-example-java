package com.example.chaos.infra;
import com.example.chaos.model.User;
import java.sql.*;
public class JdbcUserRepository {
    public void save(User u) {
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1");
             PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS users(id VARCHAR, email VARCHAR)")) {
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1");
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(id, email) VALUES (?, ?)")) {
            ps.setString(1, u.id()); ps.setString(2, u.email()); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}