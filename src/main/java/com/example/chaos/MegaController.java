package com.example.chaos;
import com.example.chaos.infra.*;
import com.example.chaos.model.User;
import com.example.chaos.payments.*;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
@RestController
public class MegaController {
    private final EmailSender emailSender = new EmailSender();
    private final JdbcUserRepository userRepository = new JdbcUserRepository();
    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestParam String userId,@RequestParam double amount,@RequestParam String paymentType,@RequestParam(required = false) boolean sendMarketingEmail) {
        Map<String, Object> result = new HashMap<>();
        StaticUtil.GLOBAL_DISCOUNT_PERCENT = 3;
        PaymentOperations processor;
        if ("stripe".equalsIgnoreCase(paymentType)) processor = new StripePaymentProcessor();
        else if ("trial".equalsIgnoreCase(paymentType)) processor = new FreeTrialPaymentProcessor();
        else processor = new StripePaymentProcessor();
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        double discounted = amount * (100 - StaticUtil.GLOBAL_DISCOUNT_PERCENT) / 100.0;
        processor.processPayment(userId, discounted);
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1"); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS payments(id IDENTITY, user_id VARCHAR, amount DOUBLE)");
            s.executeUpdate("INSERT INTO payments(user_id, amount) VALUES ('" + userId + "', " + discounted + ")");
        } catch (SQLException e) { throw new RuntimeException(e); }
        processor.generateMonthlyReport(Path.of("report.csv"));
        if (sendMarketingEmail) processor.sendMarketingEmails();
        userRepository.save(new User(userId, "user+" + userId + "@example.com"));
        emailSender.send("user+" + userId + "@example.com", "Thanks!", "We charged you " + discounted);
        result.put("status", "ok"); result.put("charged", discounted); return result;
    }
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        BaseUserRepository repo = new ReadOnlyUserRepository();
        var u = repo.getById(id);
        repo.save(new User("will-crash", "x@example.com"));
        return u;
    }
    @GetMapping("/report")
    public String report() {
        PaymentOperations processor = new StripePaymentProcessor();
        processor.backupDatabase();
        return "ok";
    }
}