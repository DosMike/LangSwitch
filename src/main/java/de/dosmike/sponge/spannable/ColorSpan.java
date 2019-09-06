package de.dosmike.sponge.spannable;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;

import java.util.Objects;

public class ColorSpan implements Span {

    private int start;
    private int end;
    private TextColor color;
    /**
     * Span constructor
     * @param from first character (inclusive)
     * @param to last character (exclusive)
     * @param color this spans color
     */
    public ColorSpan(int from, int to, TextColor color) {
        if (to < from) throw new IllegalArgumentException("Length is negative");
        if (color == null) throw new IllegalArgumentException("Color can't be null");
        this.start = from;
        this.end = to;
        this.color = color;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int end() {
        return end;
    }

    @Override
    public void setStart(int newStart) {
        start = newStart;
    }

    @Override
    public void setEnd(int newEnd) {
        end = newEnd;
    }

    @Override
    public void expand(int length) {
        end += length;
    }

    @Override
    public void move(int amount) {
        start += amount;
        end += amount;
    }

    @Override
    public void shrink(int length) {
        end -= length;
    }

    @Override
    public int length() {
        return end-start;
    }

    @Override
    public void apply(Text.Builder text) {
        text.color(color);
    }

    public TextColor getColor() {
        return color;
    }

    @Override
    public Span copy() {
        return new ColorSpan(start, end, color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorSpan colorSpan = (ColorSpan) o;
        return start == colorSpan.start &&
                end == colorSpan.end &&
                color.equals(colorSpan.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, color);
    }
}
