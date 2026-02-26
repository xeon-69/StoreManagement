package com.pos.system;

public class Launcher {
    public static void main(String[] args) {
        try {
            App.main(args);
        } catch (Throwable t) {
            try {
                java.nio.file.Path logPath = java.nio.file.Paths.get(System.getProperty("user.home"),
                        "StoreManager_startup_error.log");
                java.util.List<String> lines = new java.util.ArrayList<>();
                lines.add("Startup Error at " + java.time.LocalDateTime.now());
                lines.add(t.toString());
                for (StackTraceElement element : t.getStackTrace()) {
                    lines.add("  at " + element.toString());
                }
                if (t.getCause() != null) {
                    lines.add("Caused by: " + t.getCause().toString());
                    for (StackTraceElement element : t.getCause().getStackTrace()) {
                        lines.add("  at " + element.toString());
                    }
                }
                java.nio.file.Files.write(logPath, lines, java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
