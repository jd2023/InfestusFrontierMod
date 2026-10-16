package org.jd.infestusfrontier.core.foundation;

/**
 * Why a command changed nothing. {@code key} is a stable dotted name, {@code <owner>.<condition>}, shown to the player
 * through the translation {@code infestusfrontier.refusal.<key>}.
 */
public record Refusal(String key) {
    public Refusal {
        if (key.isBlank()) throw new IllegalArgumentException("Blank refusal key");
    }
}
