package de.dosmike.sponge.spannable;

import org.spongepowered.api.text.Text;

public interface Span {

    /**
     * @return the first inclusive character this element spans
     */
    int start();
    /**
     * @return the last exclusive character this element spans
     */
    int end();
    /**
     * Set the new inclusive character index for this elements start
     * @param newStart new span start (inclusive)
     */
    void setStart(int newStart);
    /**
     * Set the new inclusive character index for this elements end
     * @param newEnd new span start
     */
    void setEnd(int newEnd);
    /**
     * Expand the size of this span by increasing the end index by length
     * @param length how much characters to expand this span
     */
    void expand(int length);
    /**
     * Reduce moves the span a certain amount of characters to the right.
     * Negative values will move to the left
     * @param amount how far to move this span
     */
    void move(int amount);
    /**
     * Reduce the size of this span by decreasing the end index by length
     * @param length how much characters to reduce this span
     */
    void shrink(int length);
    /**
     * @return the number of characters this element spans
     */
    int length();
    /**
     * Applies this span to a @link{TextBuilder} element.
     * This does not append anything, but is required since
     * @link{Text} has no setter.
     * @param text the text to format with this span
     */
    void apply(Text.Builder text);
    /**
     * Creates a new span with copied parameters but the same (===)
     * data object instance (the same text action or format).
     * @return a copy of this span
     */
    Span copy();

    /**
     * Check if this span contains the character index
     * @param index the first position
     * @return true if this index is inside this span
     */
    default boolean containsIndex(int index) {
        return start() <= index && index < end();
    }
    /**
     * Check if this span is fully contained within the specified range.
     * @param left the first position (inclusive)
     * @param right the last position (inclusive)
     * @return true if this span is contained withing the range
     */
    default boolean containedWithin(int left, int right) {
        return start() >= left && right >= end();
    }
    /**
     * Check if this span is ending within the specified range, but not
     * starting before the specified range starts. Required when adjusting
     * Span boundaries.
     * @param left the first position (exclusive)
     * @param right the last position (inclusive)
     * @return true if this span ends within the range
     */
    default boolean endsWithin(int left, int right) {
        return start() < left && right >= end();
    }
    /**
     * Check if this span is starting within the specified range, but not
     * ending before the specified range ends. Required when adjusting
     * Span boundaries.
     * @param left the first position (inclusive)
     * @param right the last position (exclusive)
     * @return true if this span starts within the range
     */
    default boolean startsWithin(int left, int right) {
        return start() >= left && right < end();
    }
    /**
     * Check if this span is starting before and ending after the specified range.
     * Required when adjusting Span boundaries.
     * @param left the first position (inclusive)
     * @param right the last position (inclusive)
     * @return true if this span contains the range
     */
    default boolean containsRange(int left, int right) {
        return start() <= left && right <= end();
    }
    /**
     * Check if this span is starting and ending at the specified indices.
     * @param left the first position (inclusive)
     * @param right the last position (inclusive)
     * @return true if this span contains the range
     */
    default boolean isRange(int left, int right) {
        return start() == left && right == end();
    }

}
