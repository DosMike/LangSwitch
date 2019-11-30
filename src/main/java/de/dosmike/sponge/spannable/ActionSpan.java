package de.dosmike.sponge.spannable;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextAction;

import java.util.Objects;

public abstract class ActionSpan<E extends TextAction> implements Span {

    protected int start;
    protected int end; //intern excluded (end-start) == length requires end exclusive
    protected E action;
    /**
     * @param from inclusive start index
     * @param to exclusive end index
     */
    public ActionSpan(int from, int to, E action) {
        if (to < from) throw new IllegalArgumentException("Length is negative");
        if (action == null) throw new IllegalArgumentException("Action can't be null");
        this.start = from;
        this.end = to;
        this.action = action;
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

    public abstract void apply(Text.Builder text);

    public abstract Span copy();

    public E getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionSpan<?> that = (ActionSpan<?>) o;
        return start == that.start &&
                end == that.end &&
                action.equals(that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, action);
    }
}
