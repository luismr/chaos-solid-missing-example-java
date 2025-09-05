package com.example.chaos.payments;
import java.nio.file.Path;
public class FreeTrialPaymentProcessor implements PaymentOperations {
    @Override public void processPayment(String userId, double amount) { throw new UnsupportedOperationException("Free trial cannot process payments"); }
    @Override public void refund(String paymentId) { throw new UnsupportedOperationException("No refunds for free trials"); }
    @Override public void generateMonthlyReport(Path outFile) {}
    @Override public void backupDatabase() {}
    @Override public void sendMarketingEmails() {}
}