package org.jd.infestusfrontier.core.foundation;

/** Result of a command: the new value, or a refusal that names the unmet condition. A refusal changed nothing. */
public sealed interface Outcome<T> {
    record Done<T>(T value) implements Outcome<T> {}

    record Refused<T>(Refusal refusal) implements Outcome<T> {}

    static <T> Outcome<T> done(T value) {
        return new Done<>(value);
    }

    static <T> Outcome<T> refused(Refusal refusal) {
        return new Refused<>(refusal);
    }

    default boolean isDone() {
        return this instanceof Done<T>;
    }
}
