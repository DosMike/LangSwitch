package de.dosmike.sponge.langswitch;

import de.dosmike.sponge.languageservice.API.PluginTranslation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Lang extends LangItem implements PluginTranslation {
	
	Set<Locale> loaded = new HashSet<>();
	public boolean isLocaleLoaded(Locale l) {
		return loaded.contains(l);
	}
	
	Locale def;
	Lang(Locale defaultLocale) {
		def=defaultLocale;
	}
	
	public Locale getDefaultLocale() {
		return def;
	}

	public String get(String path, Locale lang) {
		return get(path, lang, def);
	}
	
	public LocalizedString local(String path) {
		return new LocalizedString(this, path);
	}
	public LocalizedText localText(String path) {
		return new LocalizedText(this, path);
	}
}
