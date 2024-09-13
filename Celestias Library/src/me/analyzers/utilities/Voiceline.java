package me.analyzers.utilities;

public class Voiceline {
    public final String[] lines;

    public Voiceline(String... lines) {
        this.lines = lines;
    }

    public String get(int i) {
        return lines[i];
    }

    @Override
    public String toString() {
        return (String) MathUtils.getRandom(lines);
    }
}
