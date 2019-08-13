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
	Lang lang;
	String path;
	
	Map<String, Object> replacements = new HashMap<>();
	/** Calls toString on replacements when resolving
	 * returns the same Localized for easy chaining. */
	public LocalizedString replace(String charSequence, Object replacement) {
		replacements.put(charSequence, replacement);
		return this;
	}
	
	private String replace(String string) {
		if (lang==null) return path;
		String replace = TextSerializers.LEGACY_FORMATTING_CODE.stripCodes(string);
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
	public Optional<String> resolve(CommandSource src) {
		if (lang==null)return Optional.empty();
		if (src instanceof Player) return resolve((Player)src);
		Optional<String> optional = lang.query(path, lang.def, null);
		if (!optional.isPresent()) return optional;
		return Optional.of(replace(optional.get()));
	}
	public Optional<String> resolve(Player player) {
		return resolve(player.getUniqueId());
	}
	public Optional<String> resolve(GameProfile player) {
		return resolve(player.getUniqueId());
	}
	public Optional<String> resolve(UUID playerID) {
		if (lang==null)return Optional.empty();
		Locale loc = LangSwitch.playerLang.get(playerID);
		return resolve(loc);
	}
	public Optional<String> resolve(Locale language) {
		Optional<String> optional = lang.query(path, language, lang.def);
		if (!optional.isPresent()) return optional;
		return Optional.of(replace(optional.get()));
	}
	/** tries to get the default translation or returns the path if not found */
	@Override
	public String toString() {
		String result = lang.get(path, lang.def, null);
		return replace(result);
	}
}
