package de.dosmike.sponge.langswitch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class LangItem {
	Map<String, LangItem> tree = new HashMap<>();
	Map<Locale, String> translations = new HashMap<>();
	
	/** will force a string, will return the path if nothing was found */
	public String get(String path, Locale lang, Locale fallback) {
		String[] route = path.split("\\.");
		return get(route, 0, lang, fallback);
	}
	String get(String[] path, int location, Locale lang, Locale fallback) {
		if (location<path.length) { //path down
			if (!tree.containsKey(path[location])) {
				LangSwitch.l("Missing translation %s", String.join(".", path));
				return String.join(".", path);
			}
			else return tree.get(path[location]).get(path, location+1, lang, fallback);
		} else if (location>path.length) { //invalid
			throw new RuntimeException("index exceeded");
		} else {//get value
			if (!translations.containsKey(lang)) { 
				if (!translations.containsKey(fallback)) {
					LangSwitch.l("Missing translation %s[Default:%s]", String.join(".", path), (fallback==null?"":fallback.toString()));
					return String.join(".", path)+String.format("[%s]", lang.toString());
				} else {
					LangSwitch.l("Missing translation %s[%s]", String.join(".", path), lang.toString());
					return translations.get(fallback);
				}
			} else {
				return translations.get(lang);
			}
		}
	}
	/** tries to retrieve a translation, if neither the translation nor a fallback are available Optional.empty() is returned */
	public Optional<String> query(String path, Locale lang, Locale fallback) {
		String[] route = path.split("\\.");
		return query(route, 0, lang, fallback);
	}
	Optional<String> query(String[] path, int location, Locale lang, Locale fallback) {
		if (location<path.length) { //path down
			if (!tree.containsKey(path[location])) {
				LangSwitch.l("Missing translation %s", String.join(".", path));
				return Optional.empty();
			}
			else return tree.get(path[location]).query(path, location+1, lang, fallback);
		} else if (location>path.length) { //invalid
			throw new RuntimeException("index exceeded");
		} else {//get value
			if (!translations.containsKey(lang)) { 
				if (!translations.containsKey(fallback)) {
					LangSwitch.l("Missing translation %s[Default:%s]", String.join(".", path), (fallback==null?"":fallback.toString()));
					return Optional.empty();
				} else {
					LangSwitch.l("Missing translation %s[%s]", String.join(".", path), lang.toString());
					return Optional.of(translations.get(fallback));
				}
			} else {
				return Optional.of(translations.get(lang));
			}
		}
	}
	
	public void addTranslation(String path, Locale lang, String value) {
		String[] route = path.split("\\.");
		add(route, 0, lang, value);
		//dumptree 
//		dump("");
	}
//	void dump(String prefix) {
//		for (Locale l : translations.keySet()) LangSwitch.l(prefix+"l %s", l.toString());
//		for (Entry<String, LangItem> e : tree.entrySet()) {
//			LangSwitch.l(prefix+".%s", e.getKey());
//			e.getValue().dump(prefix+" ");
//		}
//	}
	void add(String[] path, int location, Locale lang, String value) {
		if (location<path.length) { //path down
			LangItem sub = (tree.containsKey(path[location])?tree.get(path[location]):new LangItem());
			sub.add(path, location+1, lang, value);
			tree.put(path[location], sub);
		} else if (location>path.length) { //invalid
			throw new RuntimeException("index exceeded");
		} else {//add value
			translations.put(lang, value);
		}
	}
	public boolean isEmpty() {
		return tree.isEmpty() && translations.isEmpty();
	}
	
	public void removeTranslation(Locale lang) {
		translations.remove(lang);
		Set<String> empty = new HashSet<String>();
		for (Entry<String, LangItem> s : tree.entrySet()) {
			LangItem sub = s.getValue();
			sub.removeTranslation(lang);
			if (sub.isEmpty()) empty.add(s.getKey());
			else tree.put(s.getKey(), sub);
		}
		for (String s : empty) tree.remove(s);
	}
	public void removeTranslation(String path) {
		String[] parts = path.split("\\.");
		remove(parts, 0);
	}
	void remove(String[] path, int location) {
		if (location < path.length) {
			if (tree.containsKey(path[location])) tree.get(path[location]).remove(path, location+1);
			else return;
		} else if (location>path.length) { //invalid
			throw new RuntimeException("index exceeded");
		} else {//remove value
			tree.remove(path[location]);
		}
	}
}
