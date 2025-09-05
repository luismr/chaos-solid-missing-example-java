package com.example.chaos.payments;
import java.nio.file.Path;
public interface PaymentOperations {
    void processPayment(String userId, double amount);
    void refund(String paymentId);
    void generateMonthlyReport(Path outFile);
    void backupDatabase();
    void sendMarketingEmails();
}