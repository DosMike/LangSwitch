package de.dosmike.sponge.geoip;

import io.github.nucleuspowered.heisenberg.relocate.com.maxmind.geoip2.record.Country;
import io.github.nucleuspowered.nucleusheisenberg.NucleusHeisenberg;
import org.spongepowered.api.Sponge;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class NucleusHeisenbergProvider implements GeoIPProvider {

    private NucleusHeisenberg pluginInstance;

    public NucleusHeisenbergProvider() {
        this.pluginInstance = (NucleusHeisenberg)Sponge.getPluginManager().getPlugin("nucleus-heisenberg").get().getInstance().get();
    }

    @Override
    public CompletableFuture<Optional<Locale>> getLocaleFor(InetAddress address) {
        CompletableFuture<Optional<Locale>> result = new CompletableFuture<>();

        try {
            CompletableFuture<Optional<Country>> future = pluginInstance.getHandler().getDetails(address);
            future.whenCompleteAsync((c,e)->{
                if (e!=null)
                    result.completeExceptionally(e);
                else if (c==null || !c.isPresent())
                    result.complete(Optional.empty());
                else {
                    result.complete(Optional.ofNullable(
                            Locale.forLanguageTag(c.get().getIsoCode())
                    ));
                }
            });
        } catch (Exception e) {
            // seems to be license stuff
            pluginInstance.getLogger().warn("[Proxy:LangSwitch]: Please read and accept the GeoIP License in config/nucleus-heisenberg/nucleus-heisenberg.conf");
            result.completeExceptionally(e);
        }

        return result;
    }
}
