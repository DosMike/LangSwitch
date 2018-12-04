package de.dosmike.sponge.geoip;

import de.dosmike.sponge.langswitch.LangSwitch;
import org.spongepowered.api.Sponge;
import uk.org.whoami.geoip.GeoIPTools;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AbsentProvider implements GeoIPProvider {

    public AbsentProvider() {
    }

    @Override
    public CompletableFuture<Optional<Locale>> getLocaleFor(InetAddress address) {
        CompletableFuture<Optional<Locale>> result = new CompletableFuture<>();
        result.complete(Optional.of(LangSwitch.getServerDefault()));
        return result;
    }
}
