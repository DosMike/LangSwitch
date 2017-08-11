package de.dosmike.sponge.langswitch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import de.dosmike.sponge.languageservice.API.Localized;

public class LocalizedText implements Localized<Text> {
	Lang lang;
	String path;
	
	Map<String, Object> replacements = new HashMap<>();
	/** Calls toString on replacements when resolving
	 * returns the same Localized for easy chaining. */
	public LocalizedText replace(String charSequence, Object replacement) {
		replacements.put(charSequence, replacement);
		return this;
	}
	
	private Text replace(String string) {
		if (lang==null)return Text.of(path);
		List<Object> elements = new LinkedList<>();
		Set<String> unusedPlaceholders = replacements.keySet(); //for translators
		elements.add(string);
		boolean round=true;
		while (round) {
			round=false;
			for (int i=0; i<elements.size(); i++) {
				if (elements.get(i) instanceof String) {
					String val = (String)elements.get(i);
					for (Entry<String, Object> e : replacements.entrySet()) {
						int pos=val.indexOf(e.getKey()); 
						if (pos>=0) { round=true;
							unusedPlaceholders.remove(e.getKey());
							if (pos+e.getKey().length()<val.length()) elements.add(i+1, val.substring(pos+e.getKey().length()));
							elements.add(i+1, e.getValue());
							if (pos>0) elements.add(i+1, val.substring(0, pos));
							elements.remove(i);
							break;
						}
					}
				}
			}
		}
		if (!unusedPlaceholders.isEmpty())
			LangSwitch.l(String.format("Localisation %s[%s] does not use the following placeholder: %s", path, lang.toString(), StringUtils.join(unusedPlaceholders, ", ")));
		
		Text.Builder result = Text.builder();
		for (Object o : elements) if (o instanceof Text) result.append((Text)o); else { result.append(Text.of(o)); }
		return result.build();
	}
	
	LocalizedText(String path) {
		lang=null;
		this.path=path;
	}
	public LocalizedText(Lang yourLang, String path) {
		lang=yourLang;
		this.path=path;
	}
	public Optional<Text> resolve(CommandSource src) {
		if (lang==null)return Optional.empty();
		if (src instanceof Player) return resolve((Player)src);
		Optional<String> optional = lang.query(path, lang.def, null);
		if (!optional.isPresent()) return Optional.empty();
		return Optional.of(replace(optional.get()));
	}
	public Optional<Text> resolve(Player player) {
		return resolve(player.getUniqueId());
	}
	public Optional<Text> resolve(GameProfile player) {
		return resolve(player.getUniqueId());
	}
	public Optional<Text> resolve(UUID playerID) {
		if (lang==null)return Optional.empty();
		Locale loc = LangSwitch.playerLang.get(playerID);
		Optional<String> optional = lang.query(path, loc, lang.def);
		if (!optional.isPresent()) return Optional.empty();
		return Optional.of(replace(optional.get()));
	}
	/** tries to get the default translation or returns the path if not found */
	@Override
	public String toString() {
		String result = lang.get(path, lang.def, null);
		return replace(result).toPlain();
	}
	public Text toText() {
		String result = lang.get(path, lang.def, null);
		return replace(result);
	}
}
