package de.dosmike.sponge.spannable;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextStyle;

import java.util.Objects;

public class StyleSpan implements Span {

    private int start;
    private int end;
    private TextStyle style;
    /**
     * Span constructor
     * @param from first character (inclusive)
     * @param to last character (exclusive)
     * @param style this spans style or style compound
     */
    public StyleSpan(int from, int to, TextStyle style) {
        if (to < from) throw new IllegalArgumentException("Length is negative");
        if (style == null) throw new IllegalArgumentException("Style can't be null");
        this.start = from;
        this.end = to;
        this.style = style;
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
        text.style(style);
    }

    public TextStyle getStyle() {
        return style;
    }

    @Override
    public Span copy() {
        return new StyleSpan(start, end, style);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyleSpan styleSpan = (StyleSpan) o;
        return start == styleSpan.start &&
                end == styleSpan.end &&
                style.equals(styleSpan.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, style);
    }
}
