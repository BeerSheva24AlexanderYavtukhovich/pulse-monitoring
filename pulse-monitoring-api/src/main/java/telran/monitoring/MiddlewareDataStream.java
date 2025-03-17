package telran.monitoring;

public interface MiddlewareDataStream<T> {
    void publish(T obj);
}
