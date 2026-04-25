package me.justeli.coins.language;

/**
 * @author Eli
 * @since April 24, 2026
 */
public abstract class Entry {
    protected final String raw;
    public Entry(String raw) {
        this.raw = raw;
    }

    @Override
    public String toString() {
        return raw;
    }
}
