package telran.monitoring.logging;

public interface Logger {
    String defaultValue = "info";

    void log(String level, String message);

}