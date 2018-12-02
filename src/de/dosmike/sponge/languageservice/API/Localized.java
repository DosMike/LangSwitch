package de.dosmike.sponge.languageservice.API;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;

/** A Localized holds reference to a certain {@link PluginTranslation} at a specified translation path.<br>
 * Adding replacements will refine the result and replace placeholders in your translations as the Localized gets Resolved for a CommandSource
 * */
public interface Localized<X> {
	
	/** This method allows you to add replacements for a translation that will be put into placeholder as the Translation get's resolved for a CommandSource.<br>
	 * Lets say the translation cmd.target.nothuman looks like
	 * <pre>The target was a %entity%, but humans are required</pre>
	 * Get the Localized from your {@link PluginTranslation} and add the replacement like
	 * <pre>translations.local("cmd.target.nothuman").replace("%entity%", entity.getType().getTranslation())</pre>
	 * When resolving the Localized %entity% may be replaced with Pig
	 * @param placeholder the string to be replaced
	 * @param replacement the value to take it's place (if not a string toString() will be called)
	 * @return this localized for chaining
	 */
	public Localized<X> replace(String placeholder, Object replacement);
	
	/** Retrieves the language for this CommandSender, get's the raw translation, puts in all found placeholders and returns the translated result.
	 * @param src the command source to get translations for
	 * @return The Localized result for this CommandSource */
	public Optional<X> resolve(CommandSource src);
	/** Retrieves the language for this Player, get's the raw translation, puts in all found placeholders and returns the translated result.
	 * @param player the player to get translations for
	 * @return The Localized result for this Player */
	public Optional<X> resolve(Player player);
	/** Retrieves the language for this Player, get's the raw translation, puts in all found placeholders and returns the translated result.
	 * @param player the player to get translations for
	 * @return The Localized result for this Player */
	public Optional<X> resolve(GameProfile player);
	/** Retrieves the language for this Player, get's the raw translation, puts in all found placeholders and returns the translated result.
	 * @param playerID the player to get translations for
	 * @return The Localized result for this Player */
	public Optional<X> resolve(UUID playerID);

}
