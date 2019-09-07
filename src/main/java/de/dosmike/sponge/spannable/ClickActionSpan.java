package de.dosmike.sponge.spannable;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.ClickAction;

public class ClickActionSpan extends ActionSpan<ClickAction> {

    /**
     * Span constructor
     * @param from first character (inclusive)
     * @param to last character (exclusive)
     * @param action this spans click action
     */
    public ClickActionSpan(int from, int to, ClickAction action) {
        super(from, to, action);
    }

    @Override
    public void apply(Text.Builder text) {
        text.onClick(action);
    }

    @Override
    public Span copy() {
        return new ClickActionSpan(start, end, action);
    }
}
