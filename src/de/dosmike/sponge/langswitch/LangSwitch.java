package de.dosmike.sponge.langswitch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

import de.dosmike.sponge.languageservice.API.LanguageService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id="langswitch", name="LangSwitch", authors="DosMike", version="1.1.2")
public class LangSwitch {
	static LangSwitch instance;
//	static Lang myL;
	static String[] available;
	static {
		Locale[] locs = Locale.getAvailableLocales();
		available = new String[locs.length];
		for (int i=0; i<locs.length; i++) available[i]=locs[i].toString();
	}
	@Listener(order=Order.FIRST)
	public void init(GameInitializationEvent event) { instance = this; //myL=L.createLang(this);
		reload();
	
		Sponge.getServiceManager().setProvider(this, LanguageService.class, new LanguageServiceProvider());
		l("The LanguageService is now available!");
		
		Map<String, String> listMap = new HashMap<>();
		for (String al : available) listMap.put(al, al);
		Sponge.getCommandManager().register(this, CommandSpec.builder().arguments(GenericArguments.onlyOne(GenericArguments.choices(Text.of("Language"), listMap))).executor(new CommandExecutor() {
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				if (!(src instanceof Player)) { src.sendMessage(Text.of("Only available for players")); return CommandResult.success(); }
				Optional<String> la = args.<String>getOne("Language");
				if (!la.isPresent()) {
					src.sendMessage(Text.of(playerLang.get(((Player)src).getUniqueId()).toString()));
				} else {
					String lang = la.get();
					lang = lang.replace('_', '-'); //Locales toString used a underscore but the language tag requires a dash
					Locale locale = Locale.forLanguageTag(lang);
					playerChangedLang(((Player)src).getProfile(), locale);
				}
				return CommandResult.success();
			}
		}).build(), "language");
	}
	@Listener()
	public void reload(GameReloadEvent event) {
		reload();
	}
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private void reload() {
		try {
			ConfigurationNode root = configManager.load();
			String locale = root.getNode("DefaultLocale").getString(Locale.getDefault().toString());
			l("Setting default locale to "+locale);
			Locale previous = serverDefault;
			serverDefault = Locale.forLanguageTag(locale.replace('_', '-'));
			loadLang(serverDefault);
			unloadLangIfUnused(previous);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	@Inject
	private Logger logger;
	public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	static Map<String, Lang> plugins = new HashMap<>(); //allows us to inject translations
	static String getID(Object plugin) {
		Optional<PluginContainer> cont = Sponge.getPluginManager().fromInstance(plugin);
		if (!cont.isPresent()) throw new RuntimeException("Supplied argument not a plugin!");
		return cont.get().getId();
	}
	
	static Locale serverDefault = Locale.getDefault();
	
	@Inject
	@ConfigDir(sharedRoot = true)
	private Path configDir;
	
	static Map<UUID, Locale> playerLang = new HashMap<>();
	@Listener(order=Order.FIRST)
	public void joined(ClientConnectionEvent.Join event) {
		Player player = event.getTargetEntity();
		
		Collection<ProfileProperty> props = player.getProfile().getPropertyMap().get("language");
		Locale checkload = null;
		for (ProfileProperty prop : props)
			if (prop.getName().equalsIgnoreCase("language")) { 
				playerLang.put(player.getProfile().getUniqueId(), checkload=Locale.forLanguageTag(prop.getValue().replace('_', '-')));
				break;
			}
		
		//use geo location in the future? player.getLocale seems to stick to en_US
		if (checkload==null)
			playerLang.put(player.getProfile().getUniqueId(), checkload=player.getLocale());
		
		loadLang(checkload);
	}
	
	@Listener
	public void part(ClientConnectionEvent.Disconnect event) {
		Locale lang = playerLang.get(event.getTargetEntity().getUniqueId());
		event.getTargetEntity().getProfile().getPropertyMap().removeAll("language");
		event.getTargetEntity().getProfile().addProperty(ProfileProperty.of("language", lang.toString()));
		playerLang.remove(event.getTargetEntity().getUniqueId());

		unloadLangIfUnused(lang);
	}
	
	static void playerChangedLang(GameProfile profile, Locale newLang) {
		Locale lang = playerLang.get(profile.getUniqueId());
		profile.getPropertyMap().removeAll("language");
		profile.addProperty(ProfileProperty.of("language", newLang.toString()));
		playerLang.put(profile.getUniqueId(), newLang);
		loadLang(newLang);
		unloadLangIfUnused(lang);
	}
	
	public static void loadLang(Locale lang) {
//		l("Loading translations for %s...", lang.toString());
		Sponge.getScheduler().createAsyncExecutor(instance).execute(new LocaleRunnable(lang) {
			public void run() {
				for (Entry<String, Lang> entry : plugins.entrySet()) {
					l("Loading translations for %s in %s...", entry.getKey(), getLocale().toString());
					if (entry.getValue().loaded.contains(getLocale())) return;
					entry.getValue().loaded.add(getLocale()); //prevent stacking
					
					File to = instance.configDir.resolve(entry.getKey()).resolve("Lang").resolve(getLocale().toString()+".lang").toFile();
					if (!to.exists()) {
						l("No country specifig translations for "+getLocale().getDisplayLanguage()+", switching to "+getLocale().getLanguage()+".lang");
						to = new File(to.getParentFile(), getLocale().getLanguage()+".lang");
						if (!to.exists()) {
							l("No translation file for "+getLocale().getDisplayLanguage()+" was found!");
							continue;
						}
					}
//					l("Trying to load " + to.getAbsolutePath());
					
					BufferedReader br=null;
					try {
						br = new BufferedReader(new InputStreamReader(new FileInputStream(to), "UTF8"));
						String line;
						while ((line=br.readLine())!=null) {
							if (line.isEmpty() || line.startsWith("#")) continue;
							int split = first(line.indexOf(':'), line.indexOf('=')); //allow the usage of either key.sub:value or key.sub=value
							if (split<=0) throw new RuntimeException("Translations are formatted [\\w\\.]+:.* (numers, letters underscores and dots > colon > some text)");
							String k=line.substring(0, split);
							if (!k.matches("[\\w\\.]+")) throw new RuntimeException("Translations are formatted [\\w\\.]+:.* (numers, letters underscores and dots > colon > some text)");
							String v=line.substring(split+1);
//							l("  Adding %s in %s with: %s", k, getLocale().toString(), v);
							entry.getValue().addTranslation(k, getLocale(), v);
						}
					}
					catch (FileNotFoundException|SecurityException e) {}
					catch (IOException|RuntimeException e) {
						e.printStackTrace();
					}
					finally {
						try { br.close(); } catch (Exception e) {}
					}
				}
			}
		});
	}
	
	private static int first(int a, int b) {
		return (a>=0 && b>=0 //both valid
				? (a<b?a:b) //minimum
				: (a>=0 //only a valid
				  ? a //a
				  : (b>=0) //only b valid
				    ? b //b
				    : -1 //nothing
				));
	}
	
	public static void unloadLangIfUnused(Locale lang) {
		if (lang==null || lang.toString().equals(serverDefault.toString())) return;
		for (Locale l : playerLang.values()) if (l.equals(lang)) return;
		for (Lang l : plugins.values()) {
			l("Unloading %s translations...", lang.toString());
			l.removeTranslation(lang);
		}
	}
	
	public static abstract class LocaleRunnable implements Runnable {
		private Locale loc;
		private LocaleRunnable(Locale forLocale) { loc = forLocale; }
		Locale getLocale() { return loc; }
	}
}
