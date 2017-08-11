package de.dosmike.sponge.languageservice.API;

import java.util.Locale;

import org.spongepowered.api.text.Text;

/** Interface providing translations for your Plugin.
 * You can hold the instance during as these PluginTranslations are only generated once. */
public interface PluginTranslation {
	
	/** this method checks if a certain locale was loaded for your plugin, you might not need this.<br>
	 * In the default implementation all plugins load the same locales at the same time
	 * @param locale the locale to check for
	 * @returns true if translations for this locale were loaded */
	public boolean isLocaleLoaded(Locale locale);
	
	/** this method returns the default locale for your plugin, in the default locale this will be the derver default locale
	 * reflecting the value specified in the langswitch configuration.
	 * @returns This plugins default locale for translations */
	public Locale getDefaultLocale();

	/** this will try to resolve to translation path in your plugin for a certain locale or fall back to the default locale
	 * if the locale was not found. If there is no result for the default locale as well it will return the path-string with 
	 * the missing language in brackets.
	 * @param the path to search for translations
	 * @param lang the locale to return
	 * @returns the translation for lang or the default locale */
	public String get(String path, Locale lang);
	
	/** Returns a {@link Localized} that accepts replace parameters and has no defined language yet.
	 * This localized to return a String one resolved for a CommandSource
	 * @param path the translation path to be resolved for a translation
	 * @return a prepared Localized resolving into a String */
	public Localized<String> local(String path);
	/** Returns a {@link Localized} that accepts replace parameters and has no defined language yet.
	 * This localized to return a SpongeAPI Text one resolved for a CommandSource
	 * @param path the translation path to be resolved for a translation
	 * @return a prepared Localized resolving into a Text */
	public Localized<Text> localText(String path);
}
