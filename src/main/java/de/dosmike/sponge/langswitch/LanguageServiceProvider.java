package de.dosmike.sponge.langswitch;

import de.dosmike.sponge.languageservice.API.LanguageService;
import de.dosmike.sponge.languageservice.API.PluginTranslation;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Locale;
import java.util.Optional;

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
//		LangSwitch.l("Register plugin "+id+" with "+LangSwitch.serverDefault.toString()+" as default language");
		Lang newLang = new Lang(LangSwitch.serverDefault);
		LangSwitch.plugins.put(id, newLang);
		LangSwitch.loadSingleLang(LangSwitch.serverDefault, id, newLang);
		return newLang;
	}

	public Locale getSelectedLocale(CommandSource target) {
		Locale l = LangSwitch.serverDefault;
		if (target instanceof Player) {
			l = LangSwitch.playerLang.get(((Player) target).getUniqueId());
			if (l == null) return ((Player) target).getLocale();
		}
		return l;
	}

}
