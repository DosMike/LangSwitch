package de.dosmike.sponge.geoip;

import org.spongepowered.api.entity.living.player.Player;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GeoIPProvider {

    CompletableFuture<Optional<Locale>> getLocaleFor(InetAddress address);

    default CompletableFuture<Optional<Locale>> getLocaleFor(Player source) {
        return getLocaleFor(source.getConnection().getAddress().getAddress());
    }

}
