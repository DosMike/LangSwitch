package de.dosmike.sponge.langswitch;

import de.dosmike.sponge.languageservice.API.Localized;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import java.util.*;
import java.util.Map.Entry;

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
		Set<String> unusedPlaceholders = new HashSet<>();
		unusedPlaceholders.addAll(replacements.keySet()); //for translators
		elements.add(string);
		boolean round=true;
		while (round) {
			round=false;
			i: for (int i=0; i<elements.size(); i++) {
				if (elements.get(i) instanceof String) {
					String val = (String)elements.get(i);
					for (Entry<String, Object> e : replacements.entrySet()) {
						int pos=val.indexOf(e.getKey()); 
						if (pos>=0) { round=true;
							unusedPlaceholders.remove(e.getKey());
							
							if (pos+e.getKey().length()<val.length())
								elements.add(i+1, val.substring(pos+e.getKey().length()));
							elements.add(i+1, e.getValue());
							if (pos>0)
								elements.add(i+1, val.substring(0, pos));
							elements.remove(i);
							
							break i;
						}
					}
				}
			}
		}
		if (!unusedPlaceholders.isEmpty() && LangSwitch.verbose)
			LangSwitch.l("Localisation %s does not use the following placeholder: %s", path, StringUtils.join(unusedPlaceholders, ", "));

//		Text.Builder result = Text.builder();
//		for (Object o : elements)
//			if (o instanceof Text)
//				result.append((Text)o);
//			//not deserializing legacy code is the only thing that let's them work in this case, since they need to apply for the rest of the result
////			else if (o instanceof String)
////				result.append( TextSerializers.FORMATTING_CODE.deserializeUnchecked((String)o) );
////				result.append( TextSerializers.LEGACY_FORMATTING_CODE.deserializeUnchecked((String)o) );
//			else
//				result.append(Text.of(o));
//		return result.build();
		return Text.of(elements.toArray(new Object[0]));
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
		return resolve(loc);
	}
	public Optional<Text> resolve(Locale language) {
		Optional<String> optional = lang.query(path, language, lang.def);
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
