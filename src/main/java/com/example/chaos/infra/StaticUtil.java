package com.example.chaos.infra;
import java.time.LocalDateTime;
public final class StaticUtil {
    public static int GLOBAL_DISCOUNT_PERCENT = 0;
    public static void log(String msg) { System.out.println(LocalDateTime.now() + " [LOG] " + msg); }
    private StaticUtil() {}
}