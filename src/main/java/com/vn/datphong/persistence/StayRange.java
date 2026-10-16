package com.vn.datphong.persistence;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** Finite hotel stay, including check-in and excluding check-out. */
public record StayRange(LocalDate from, LocalDate to) implements Serializable {
    public StayRange {
        Objects.requireNonNull(from, "check-in");
        Objects.requireNonNull(to, "check-out");
        if (!from.isBefore(to)) throw new IllegalArgumentException("check-out must follow check-in");
    }

    public static StayRange parse(String value) {
        if (value == null) return null;
        if (!value.startsWith("[") || !value.endsWith(")")) throw new IllegalArgumentException("Expected a finite [from,to) date range");
        String[] bounds = value.substring(1, value.length() - 1).split(",", -1);
        if (bounds.length != 2) throw new IllegalArgumentException("Invalid stay range");
        return new StayRange(LocalDate.parse(bounds[0]), LocalDate.parse(bounds[1]));
    }

    @Override public String toString() { return "[" + from + "," + to + ")"; }
}
