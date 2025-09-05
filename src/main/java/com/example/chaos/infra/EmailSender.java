package com.example.chaos.infra;
public class EmailSender {
    public void send(String to, String subject, String body) {
        StaticUtil.log("Email to " + to + " | " + subject + " | " + body);
    }
}