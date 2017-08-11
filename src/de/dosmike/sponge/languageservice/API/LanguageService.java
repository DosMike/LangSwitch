package de.dosmike.sponge.languageservice.API;

import java.util.Optional;

import de.dosmike.sponge.langswitch.LocalizedString;
import de.dosmike.sponge.langswitch.LocalizedText;

/** The service interface to access translations for your plugin.
 */
public interface LanguageService {

	/** Get a Localized String as the plugin with a given pluginID from the given translation-path.<br>
	 * Effects for not existing plugins may varry, the default implementation will return the path.
	 * @param plugin the pluginId of the plugin to mimic
	 * @param path the translation-path to get translations from
	 * @return a {@link Localized} the when resolved returns a String
	  */
	public LocalizedString local(String plugin, String path);

	/** Get a Localized String as the plugin with a given pluginID from the given translation-path.<br>
	 * Effects for not existing plugins may varry, the default implementation will return the path.
	 * @param plugin the pluginId of the plugin to mimic
	 * @param path the translation-path to get translations from
	 * @return a {@link Localized} the when resolved returns a String
	  */
	public LocalizedText localText(String plugin, String path);

	/** This function returns your {@link PluginTranslation} at any point.<br>
	 * Remember that you have to register your plugin before you're able to receive the PluginTranslation.
	 * @param plugin your plugin instance
	 * @returns your PluginTranslation if available
	  */
	public Optional<PluginTranslation> getTranslation(Object plugin);

	/** This function registers your plugin in the service allowing you to get your {@link PluginTranslation}
	 * and to automatically inject languages into the translation object as needed by the player base.<br>
	 * As the translations load in asynchronously, translations may not instantly be available  
	 * @param plugin the plugin instance you want to register
	 * @return your plugin translation instance. */
	public PluginTranslation registerTranslation(Object plugin);

}
