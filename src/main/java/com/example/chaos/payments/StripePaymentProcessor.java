package com.example.chaos.payments;
import com.example.chaos.infra.StaticUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
public class StripePaymentProcessor implements PaymentOperations {
    @Override public void processPayment(String userId, double amount) { StaticUtil.log("Stripe charge " + userId + " $" + amount); }
    @Override public void refund(String paymentId) { StaticUtil.log("Stripe refund " + paymentId); }
    @Override public void generateMonthlyReport(Path outFile) {
        try { Files.writeString(outFile, "csv,goes,here\\n"); } catch (IOException e) { throw new RuntimeException(e); }
    }
    @Override public void backupDatabase() { StaticUtil.log("Pretend DB backup…"); }
    @Override public void sendMarketingEmails() { StaticUtil.log("Email blast to everyone!"); }
}