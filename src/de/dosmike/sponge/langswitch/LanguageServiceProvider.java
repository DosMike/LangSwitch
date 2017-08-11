package de.dosmike.sponge.langswitch;

import java.util.Optional;

import de.dosmike.sponge.languageservice.API.LanguageService;
import de.dosmike.sponge.languageservice.API.PluginTranslation;

public class LanguageServiceProvider implements LanguageService {

	public LocalizedString local(String plugin, String path) {
		return LangSwitch.plugins.containsKey(plugin)?new LocalizedString(LangSwitch.plugins.get(plugin), path):new LocalizedString(path);
	}

	public LocalizedText localText(String plugin, String path) {
		return LangSwitch.plugins.containsKey(plugin)?new LocalizedText(LangSwitch.plugins.get(plugin), path):new LocalizedText(path);
	}

	public Optional<PluginTranslation> getTranslation(Object plugin) {
		String id = LangSwitch.getID(plugin);
		Lang l = LangSwitch.plugins.get(id);
		return (l==null?Optional.empty():Optional.of(l));
	}

	public PluginTranslation registerTranslation(Object plugin) {
		String id = LangSwitch.getID(plugin);
		Lang newLang = new Lang(LangSwitch.serverDefault);
		LangSwitch.plugins.put(id, newLang);
		LangSwitch.loadLang(LangSwitch.serverDefault);
		return newLang;
	}

}
