package de.dosmike.sponge.geoip;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Optional;

public class GeoIPService {

    private static GeoIPProvider provider = null;

    public static GeoIPProvider getProvider() {
        if (provider == null) detect();
        return provider;
    }

    private static void detect() {

        Optional<PluginContainer> plugin;

        plugin = Sponge.getPluginManager().getPlugin("nucleus-heisenberg");
        if (plugin.isPresent()) {
            provider = new NucleusHeisenbergProvider();
            return;
        }

        provider = new AbsentProvider();

    }

}
