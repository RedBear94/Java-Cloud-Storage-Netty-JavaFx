// Для избежания жесткой привязки сети к интерфейсу
public interface Callback {
    // ... - Принимает несколько аргументов типа Object
    void callback(Object... args);
}
