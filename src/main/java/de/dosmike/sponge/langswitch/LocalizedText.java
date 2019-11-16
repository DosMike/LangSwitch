package de.dosmike.sponge.langswitch;

import de.dosmike.sponge.languageservice.API.Localized;
import de.dosmike.sponge.spannable.Spannable;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import java.util.*;
import java.util.Map.Entry;

public class LocalizedText implements Localized<Text> {
    private Lang lang;
	private String path;

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
		Spannable raw = Spannable.from(string);
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
		return raw.toText();
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
        String template = lang.query(path, lang.def, null).orElse(path);
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
        return getLocal(lang.query(path, locale, lang.def).orElse(path), locale);
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
