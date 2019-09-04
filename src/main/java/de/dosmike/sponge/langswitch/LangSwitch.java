package de.dosmike.sponge.langswitch;

import com.google.inject.Inject;
import de.dosmike.sponge.VersionChecker;
import de.dosmike.sponge.geoip.GeoIPService;
import de.dosmike.sponge.languageservice.API.LanguageService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
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
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.text.Text;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

@Plugin(id="langswitch", name="LangSwitch", authors="DosMike", version="1.5")
public class LangSwitch {
	static LangSwitch instance;
//	static Lang myL;
	static String[] available;

	static boolean verbose=true;

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
	@Listener
	public void onGameStarted(GameStartedServerEvent event) {
		VersionChecker.checkVersion(this);
	}
	@Listener()
	public void reload(GameReloadEvent event) {
        l("Reloading config...");
        reload();
        l("Reloading translation files...");
		forceReloadTranslations();
	}
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private void reload() {
		if (!new File(".", "config/langswitch.conf").exists()) {
			new File(".", "config").mkdirs();
			CommentedConfigurationNode root = configManager.createEmptyNode(), node;
			node = root.getNode("DefaultLocale");
			node.setComment("This is the default fallback language for the server. If a translation is missing this language will be used, so make sure that the translations for this language are complete.");
			node.setValue(Locale.getDefault().getLanguage()+"_"+Locale.getDefault().getCountry());

			node = root.getNode("VerboseLogging");
			node.setComment("Verbose logging will inform you about any missing or broken translations. It is recommended that you boot up with this enabled at least once after updates, to get a quick glimpse if everything is ok.");
			node.setValue(true);

			node = root.getNode("VersionChecker");
			node.setComment("It's strongly recommended to enable automatic version checking,\n" +
					"This will also inform you about changes in dependencies.\n" +
					"Set this value to true to allow this Plugin to check for Updates on Ore");
			node.setValue(false);
			try {
				configManager.save(root);
			} catch (Exception e) {
				w("Could not save the default config");
			}
		}
		try {
			ConfigurationNode root = configManager.load();
			String locale = root.getNode("DefaultLocale").getString(Locale.getDefault().toString());
			l("Setting default locale to "+locale);
			Locale previous = serverDefault;
			serverDefault = Locale.forLanguageTag(locale.replace('_', '-'));
			loadLang(serverDefault);
			unloadLangIfUnused(previous);

			verbose = root.getNode("VerboseLogging").getBoolean(true);

			VersionChecker.setVersionCheckingEnabled(
					Sponge.getPluginManager().fromInstance(this).get().getId(),
					root.getNode("VersionChecker").getBoolean(false)
					);
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
	public static Locale getServerDefault() {
		return serverDefault;
	}
	
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
				String val=prop.getValue();
				if (val != null && !val.isEmpty()) {
					val = val.replace('_', '-');
					checkload=Locale.forLanguageTag(val);
					playerLang.put(player.getProfile().getUniqueId(), checkload);
				}
				break;
			}
		
		if (checkload==null)
			GeoIPService.getProvider().getLocaleFor(player).whenCompleteAsync((ol, e)->{
				Locale locale;
				if (e != null) {
					//probably a connection from localhost, not providing a ip to lookup
					locale = getServerDefault();
				} else {
					locale = ol.orElse(getServerDefault());
				}
				playerLang.put(player.getProfile().getUniqueId(), locale);
				loadLang(locale);
			});
		else
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
					if (verbose) l("Loading translations for %s in %s...", entry.getKey(), getLocale().toString());
					if (entry.getValue().loaded.contains(getLocale())) return;
					entry.getValue().loaded.add(getLocale()); //prevent stacking
					
					File to = instance.configDir.resolve(entry.getKey()).resolve("Lang").resolve(getLocale().toString()+".lang").toFile();
					if (!to.exists()) {
						if (verbose) l("No country specifig translations for "+getLocale().getDisplayLanguage()+", switching to "+getLocale().getLanguage()+".lang");
						to = new File(to.getParentFile(), getLocale().getLanguage()+".lang");
						if (!to.exists()) {
							if (verbose) l("No translation file for "+getLocale().getDisplayLanguage()+" was found!");
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
		if (lang==null) return;
		if (lang.equals(serverDefault) ||
			( (lang.getCountry().isEmpty() || serverDefault.getCountry().isEmpty()) &&
				lang.getLanguage().equals(serverDefault.getLanguage())
			)
		   ) { return; }
		for (Locale l : playerLang.values()) if (l.equals(lang)) return;
		for (Lang l : plugins.values()) {
			l.removeTranslation(lang);
			l.loaded.remove(lang);
		}
	}

	public static abstract class LocaleRunnable implements Runnable {
		private Locale loc;
		private LocaleRunnable(Locale forLocale) { loc = forLocale; }
		Locale getLocale() { return loc; }
	}

	public static void forceReloadTranslations() {
        Set<Locale> allLoaded = new HashSet<>(playerLang.values());
        allLoaded.add(serverDefault);
        for (Lang lang : plugins.values()) {
            for (Locale locale : allLoaded) {
                lang.removeTranslation(locale);
                lang.loaded.remove(locale);
            }
        }
        for (Locale locale : allLoaded) {
            loadLang(locale);
        }
    }
}
