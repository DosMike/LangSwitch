package de.dosmike.sponge.spannable;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.ShiftClickAction;

public class ShiftClickActionSpan extends ActionSpan<ShiftClickAction> {

    /**
     * Span constructor
     * @param from first character (inclusive)
     * @param to last character (exclusive)
     * @param action this spans shift-click action
     */
    public ShiftClickActionSpan(int from, int to, ShiftClickAction action) {
        super(from, to, action);
    }

    @Override
    public void apply(Text.Builder text) {
        text.onShiftClick(action);
    }

    @Override
    public Span copy() {
        return new ShiftClickActionSpan(start, end, action);
    }
}
