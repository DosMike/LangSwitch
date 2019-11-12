package de.dosmike.sponge.langswitch;

import de.dosmike.sponge.languageservice.API.Localized;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.*;
import java.util.Map.Entry;

public class LocalizedString implements Localized<String> {
    private Lang lang;
    private String path;

    private Map<String, Object> replacements = new HashMap<>();
	/** Calls toString on replacements when resolving
	 * returns the same Localized for easy chaining. */
    @Override
    public LocalizedString replace(String charSequence, Object replacement) {
		replacements.put(charSequence, replacement);
		return this;
	}
	
	private String replace(String string) {
		if (lang==null) return path;
		//String replace = TextSerializers.LEGACY_FORMATTING_CODE.stripCodes(string);
		String replace = TextSerializers.formattingCode('\u00a7').stripCodes(string);
		Set<String> unusedPlaceholders = new HashSet<>();
		unusedPlaceholders.addAll(replacements.keySet()); //for translators
		boolean round=true;
		
		while(round) {
			round=false;
			for (Entry<String, Object> e : replacements.entrySet()) {
				if (replace.contains(e.getKey())) { round=true;
					unusedPlaceholders.remove(e.getKey());
					replace = replace.replace(e.getKey(), e.getValue().toString());
					break;
				}
			}
		}
		if (!unusedPlaceholders.isEmpty() && LangSwitch.verbose)
			LangSwitch.l("Localisation %s does not use the following placeholder: %s", path, StringUtils.join(unusedPlaceholders, ", "));
		
		return replace;
	}
	LocalizedString(String path) {
		lang=null;
		this.path=path;
	}
	public LocalizedString(Lang yourLang, String path) {
		lang=yourLang;
		this.path=path;
	}
    @Override
    public Optional<String> resolve(CommandSource src) {
		if (lang==null)return Optional.empty();
		if (src instanceof Player) return resolve((Player)src);
		Optional<String> optional = lang.query(path, lang.def, null);
        return optional.map(this::replace);
    }
    @Override
    public Optional<String> resolve(Player player) {
		return resolve(player.getUniqueId());
	}
    @Override
    public Optional<String> resolve(GameProfile player) {
		return resolve(player.getUniqueId());
	}
    @Override
    public Optional<String> resolve(UUID playerID) {
		if (lang==null)return Optional.empty();
		Locale loc = LangSwitch.playerLang.get(playerID);
		return resolve(loc);
	}
    @Override
    public Optional<String> resolve(Locale language) {
		Optional<String> optional = lang.query(path, language, lang.def);
        return optional.map(this::replace);
    }

    @Override
    public String orLiteral(CommandSource src) {
        if (lang==null) return replace(path);
        if (src instanceof Player) return orLiteral((Player)src);
        String template = lang.query(path, lang.def, null).orElse(path);
        return replace(template);
    }
    @Override
    public String orLiteral(Player player) {
        return orLiteral(player.getUniqueId());
    }
    @Override
    public String orLiteral(GameProfile player) {
        return orLiteral(player.getUniqueId());
    }
    @Override
    public String orLiteral(UUID playerID) {
        if (lang==null) return replace(path);
        return orLiteral(LangSwitch.playerLang.get(playerID));
    }
    @Override
    public String orLiteral(Locale locale) {
        String template = lang.query(path, locale, lang.def).orElse(path);
        return replace(template);
    }

    /** tries to get the default translation or returns the path if not found */
	@Override
	public String toString() {
		String result = lang.get(path, lang.def, null);
		return replace(result);
	}
}
