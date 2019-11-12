package de.dosmike.sponge.langswitch;

import de.dosmike.sponge.languageservice.API.PluginTranslation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Lang extends LangItem implements PluginTranslation {
	
	Set<Locale> loaded = new HashSet<>();
	@Override
	public boolean isLocaleLoaded(Locale l) {
		return loaded.contains(l);
	}
	
	Locale def;
	Lang(Locale defaultLocale) {
		def=defaultLocale;
	}

	@Override
	public Locale getDefaultLocale() {
		return def;
	}

	@Override
	public String get(String path, Locale lang) {
		return get(path, lang, def);
	}
	@Override
	public boolean has(String path, Locale lang) {
		return query(path, lang, null).isPresent();
	}
	@Override
	public boolean hasOrDefault(String path, Locale lang) {
		return query(path, lang, def).isPresent();
	}

	@Override
	public LocalizedString local(String path) {
		return new LocalizedString(this, path);
	}
	@Override
	public LocalizedText localText(String path) {
		return new LocalizedText(this, path);
	}
}
