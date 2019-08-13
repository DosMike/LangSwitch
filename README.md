# LangSwitch

This plugin provides a simple localiyation service for SpongePowered plugins.

The default implemetation requires a plugin to have translation files in their 
configuration Folder under `<SERVER>\config\<PLUGIN>\Lang\<LOCALE>.lang` where
`LOCALE`is a language_COUNTRY tage like `en_US`, `fr_FR` or `de_DE`.

These language files are NO hocon configs!
Lines that start with `#` are comments, every other line should follow the
structure `Translation Path:Translation Text`.

A translation path may only consist of lowercase letters, number and
underscores. The dot is used to group translations together (similar to the
permission node system). The colon is neccessary, afterwards any text can
follow, representing the localised String for this translation path.

Plugin developers can load LangSwitch like any service. The API package was
named separately to make other implementaitons easier (They just need to copy
paste the API package into their implementation to provide some code behind
the interfaces).

As 'normal' plugin developer you can proceed similar to receiving the economy
service:
```
private LanguageService languageService = null;
@Listener
public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
  if (event.getService().equals(LanguageService.class)) {
    languageService = (LanguageService) event.getNewProvider();
    languageService.registerTranslation(this); //add this plugin to the translation provider
  }
}
public static LanguageService getTranslator() { return instance.languageService; }
```
The line languageService.registerTranslation(this); is important so the provider
will be able to inject the translations into your PluginTranslation object.
You can receive a PluginTranslation object either from 
`languageService.registerTranslation(this)` or at any point using
`languageService().getTranslation(this)`. The later method returns a optional 
that will be empty if registerTranslation was not yet called.

**Don't forget that your plugin now depends on LangSwitch so you'll have to add the dependency in your mcmod.info like**
`"dependencies": [ "langswitch" ]`

From the PluginTranslation you normally proceed like
```
Localized<String> localized = translation.local("cmd.success");
localized.replace("%name%", displayName);
player.sendMessage(Text.of(localized.resolve(player).orElse("[command success message]")));
```
the replace method returns the localized so you can chain the block into a 
single line if wanted. Resovle returns a optional, that will be empty if no
translation could be received.

There is also `translation.localText` that is capable of handling Text
placeholders incase you plan on having clickable chat messages. The above
example would change to something like this:
```
Localized<Text> localized = translation.localText("cmd.success");
localized.replace("%name%", Text.of(TextColors.GREEN, displayName));
player.sendMessage(localized.resolve(player).orElse(Text.of("[command success message]")));
```

LangSwitch will use the server default locale as fallback language for all
plugins. If this is not desired you can change the default language in the
config under `SERVER\config\langswitch.conf`. Just set the value for
`DefaultLocale` to a locale as described at the top of this readme.

Example config:
```
# This is the default fallback language for the server.
# If a translation is missing this language will be used,
# so make sure that the translations for this language are complete.
DefaultLocale=en_US

# Verbose logging will inform you about any missing or
# broken translations. It is recommended that you boot
# up with this enabled at least once after updates,
# to get a quick glimpse if everything is ok.
VerboseLogging=true

# It's strongly recommended to enable automatic version checking,
# This will also inform you about changes in dependencies.
# Set this value to true to allow this Plugin to check for Updates on Ore
VersionChecker=false
```

### Depending on this plugin

This plugin is jitpack-compatible, if you're using gradle just add this:
```{groovy}
repositories {
    ...
    maven { url "https://jitpack.io" }
}
dependencies {
    ...
    compile 'com.github.DosMike:LangSwitch:master-SNAPSHOT'
}
```

### External Connections

**[Version Checker](https://github.com/DosMike/SpongePluginVersionChecker)**  
This plugin uses a version checker to notify you about available updates.  
This updater is **disabled by default** and can be enabled in `config/langswitch.conf`
by setting the value `VersionChecker` to `true`.  
If enabled it will asynchronously check (once per server start) if the Ore repository has any updates.  
This will *only print update notes into the server log*, no files are being downlaoded!