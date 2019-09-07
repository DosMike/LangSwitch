package de.dosmike.sponge.spannable;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.HoverAction;

public class HoverActionSpan extends ActionSpan<HoverAction> {

    /**
     * Span constructor
     * @param from first character (inclusive)
     * @param to last character (exclusive)
     * @param action this spans hover action
     */
    public HoverActionSpan(int from, int to, HoverAction action) {
        super(from, to, action);
    }

    @Override
    public void apply(Text.Builder text) {
        text.onHover(action);
    }

    @Override
    public Span copy() {
        return new HoverActionSpan(start, end, action);
    }
}
