package task3;

public class Patient {
    public enum Type { T1, T2, T3 }

    public final long id;
    public final Type orig;   // початковий тип для статистики
    public Type type;         // поточний тип для маршрутизації/пріоритету
    public final double t0;   // час входу в систему

    public Patient(long id, Type type, double t0) {
        this.id = id;
        this.orig = type;
        this.type = type;
        this.t0 = t0;
    }
}