package com.imjustdoom.libwork;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final StringBuilder BUILDER = new StringBuilder();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static int LEVEL = 2;
    public static int NOTHING = 0;
    public static int LOGGING = 1;
    public static int LOG_ERR = 2;
    public static int EVERYTHING = 3;

    public static String CLIENT = "Client";
    public static String SERVER = "Server";
    public static String COMMON = "Common";

    public static synchronized void log(String group, String log) {
        if (LEVEL == 0) return;
        BUILDER.setLength(0);
        BUILDER.append("[").append(LocalTime.now().format(FORMATTER)).append("] [").append(group).append("] ").append(log);
        System.out.println(BUILDER);
    }

    public static synchronized void error(String group, String log) {
        if (LEVEL < 2) return;
        BUILDER.setLength(0);
        BUILDER.append("[").append(LocalTime.now().format(FORMATTER)).append("] [").append(group).append("] ").append(log);
        System.err.println(BUILDER);
    }

    public static synchronized void debug(String group, String log) {
        if (LEVEL < 3) return;
        log(group, log);
    }

    public static void setLevel(int level) {
        LEVEL = level;
    }
}
