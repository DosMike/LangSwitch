package de.dosmike.sponge.langswitch;

import de.dosmike.sponge.languageservice.API.Localized;
import de.dosmike.sponge.spannable.Spannable;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.*;
import java.util.Map.Entry;

public class LocalizedText implements Localized<Text> {
    private Lang lang;
	private String path;
	private TextColor contextColor = TextColors.RESET;
	private TextStyle contextStyle = TextStyles.RESET;

    private Map<String, Object> replacements = new HashMap<>();
	/** Calls toString on replacements when resolving
	 * returns the same Localized for easy chaining. */
    @Override
    public LocalizedText replace(String charSequence, Object replacement) {
		replacements.put(charSequence, replacement);
		return this;
	}

	/**
	 * Takes the raw translation string, inserts registered replacements
	 * and returns the result as text
	 */
	private Text getLocal(String string, Locale locale) {
//		Spannable raw = Spannable.from(TextSerializers.formattingCode('\u00a7').deserialize(string));
//		Spannable raw = Spannable.from(string);
		Spannable raw = Spannable.parseSerialized(string, '\u00a7');
		Set<String> unusedPlaceholders = new HashSet<>(replacements.keySet());
		for (Entry<String, Object> entry : replacements.entrySet()) {
			Text replacement;
			if (raw.toString().contains(entry.getKey())) {
				unusedPlaceholders.remove(entry.getKey()); //placeholder is used

				if (entry.getValue() instanceof Localized) {
					Object loc = ((Localized) entry.getValue()).orLiteral(locale);
					replacement = (loc instanceof Text) ? (Text)loc : Text.of(loc);
				} else if (entry.getValue() instanceof Text) {
					replacement = (Text) entry.getValue();
				} else {
					replacement = Text.of(entry.getValue());
				}
				raw = raw.replace(entry.getKey(), replacement);
			}
		}
		if (!unusedPlaceholders.isEmpty() && LangSwitch.verbose)
			LangSwitch.l("Localisation %s does not use the following placeholder: %s", path, StringUtils.join(unusedPlaceholders, ", "));
		return raw.toText(contextColor,contextStyle);
	}
	
	LocalizedText(String path) {
		lang=null;
		this.path=path;
	}
	public LocalizedText(Lang yourLang, String path) {
		lang=yourLang;
		this.path=path;
	}
	@Override
    public Optional<Text> resolve(CommandSource src) {
		if (lang==null)return Optional.empty();
		if (src instanceof Player) return resolve((Player)src);
		Optional<String> optional = lang.query(path, lang.def, null);
        return optional.map(s->getLocal(s,lang.def));
    }
    @Override
    public Optional<Text> resolve(Player player) {
		return resolve(player.getUniqueId());
	}
    @Override
    public Optional<Text> resolve(GameProfile player) {
		return resolve(player.getUniqueId());
	}
    @Override
    public Optional<Text> resolve(UUID playerID) {
		if (lang==null)return Optional.empty();
		Locale loc = LangSwitch.playerLang.get(playerID);
		return resolve(loc);
	}
    @Override
    public Optional<Text> resolve(Locale language) {
		Optional<String> optional = lang.query(path, language, lang.def);
        return optional.map(s->getLocal(s,language));
    }

    @Override
    public Text orLiteral(CommandSource src) {
        if (lang==null) return getLocal(path, null);
        if (src instanceof Player) return orLiteral((Player)src);
        String template = lang.query(path, lang.def, null, true).orElse(path);
        return getLocal(template, lang.def);
    }
    @Override
    public Text orLiteral(Player player) {
        return orLiteral(player.getUniqueId());
    }
    @Override
    public Text orLiteral(GameProfile player) {
        return orLiteral(player.getUniqueId());
    }
    @Override
    public Text orLiteral(UUID playerID) {
        if (lang==null) return getLocal(path, null);
        return orLiteral(LangSwitch.playerLang.get(playerID));
    }
    @Override
    public Text orLiteral(Locale locale) {
		if (lang==null) return getLocal(path, null);
        return getLocal(lang.query(path, locale, lang.def, true).orElse(path), locale);
    }

    /**
     * Text actually sucks hard and TextColor.NONE / TextStyle.NONE
     * do not END styles / colors but actually literally do not contain
     * styles / colors. For spannable to text conversion it is important
     * for me to revert styles after a span, but since i can not cleanly
     * terminate single styles / colors i have to perform a full RESET
     * and reapply "unstyled" styles that match the overall style of the
     * Text. The defaults are RESET style / color.
	 * @param contextColor the color to reset to between spans
	 */
    public void setContextColor(TextColor contextColor) {
		this.contextColor = contextColor;
	}
	/**
	 * Text actually sucks hard and TextColor.NONE / TextStyle.NONE
	 * do not END styles / colors but actually literally do not contain
	 * styles / colors. For spannable to text conversion it is important
	 * for me to revert styles after a span, but since i can not cleanly
	 * terminate single styles / colors i have to perform a full RESET
	 * and reapply "unstyled" styles that match the overall style of the
	 * Text. The defaults are RESET style / color.
	 * @param contextStyle the style to reset to between spans
	 */
	public void setContextStyle(TextStyle contextStyle) {
		this.contextStyle = contextStyle;
	}
	/**
	 * Text actually sucks hard and TextColor.NONE / TextStyle.NONE
	 * do not END styles / colors but actually literally do not contain
	 * styles / colors. For spannable to text conversion it is important
	 * for me to revert styles after a span, but since i can not cleanly
	 * terminate single styles / colors i have to perform a full RESET
	 * and reapply "unstyled" styles that match the overall style of the
	 * Text. The defaults are RESET style / color.
	 * @param contextColor the color to reset to between spans
	 * @param contextStyle the style to reset to between spans
	 */
	public void setContextFormat(TextColor contextColor, TextStyle contextStyle) {
		this.contextColor = contextColor;
		this.contextStyle = contextStyle;
	}
	/**
	 * Text actually sucks hard and TextColor.NONE / TextStyle.NONE
	 * do not END styles / colors but actually literally do not contain
	 * styles / colors. For spannable to text conversion it is important
	 * for me to revert styles after a span, but since i can not cleanly
	 * terminate single styles / colors i have to perform a full RESET
	 * and reapply "unstyled" styles that match the overall style of the
	 * Text. The defaults are RESET style / color.
	 * @param formattedText flat text object formatted with the style / color to reset to between spans
	 */
	public void setContextFormat(Text formattedText) {
		contextColor = formattedText.getColor();
		contextStyle = formattedText.getStyle();
	}

    /** tries to get the default translation or returns the path if not found */
	@Override
	public String toString() {
		if (lang==null) return getLocal(path, null).toPlain();
		String result = lang.get(path, lang.def, null);
		return getLocal(result, null).toPlain();
	}
	public Text toText() {
		if (lang==null) return getLocal(path, null);
		String result = lang.get(path, lang.def, null);
		return getLocal(result, lang.def);
	}
}
