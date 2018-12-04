package de.dosmike.sponge.geoip;

import de.dosmike.sponge.langswitch.LangSwitch;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.translation.locale.Locales;
import uk.org.whoami.geoip.GeoIPTools;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GeoIPToolsProvider implements GeoIPProvider {

    private GeoIPTools pluginInstance;
    private LangSwitch self;

    public GeoIPToolsProvider() {
        pluginInstance = (GeoIPTools)Sponge.getPluginManager().getPlugin("geoiptools").get().getInstance().get();
        self = (LangSwitch)Sponge.getPluginManager().getPlugin("langswitch").get().getInstance().get();
    }

    @Override
    public CompletableFuture<Optional<Locale>> getLocaleFor(InetAddress address) {
        CompletableFuture<Optional<Locale>> result = new CompletableFuture<>();
        Sponge.getScheduler().createAsyncExecutor(self).execute(()-> {
            try {
                String code;
                if (address instanceof Inet4Address)
                    code = pluginInstance.getGeoIPLookup().getLocation(address).countryCode;
                else {
                    code = pluginInstance.getGeoIPLookup().getCountry(address).getCode();
                }
                if (code == null || code.length() < 2)
                    result.complete(Optional.empty());
                else
                    result.complete(Optional.ofNullable(Locale.forLanguageTag(code)));
            } catch (Exception e) {
                /* unknown but catch anyways */
                result.completeExceptionally(e);
            }
        });

        return result;
    }
}
