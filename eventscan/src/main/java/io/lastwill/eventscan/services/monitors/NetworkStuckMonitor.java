package io.lastwill.eventscan.services.monitors;

import io.lastwill.eventscan.events.utility.NetworkStuckEvent;
import io.mywish.scanner.model.NetworkType;
import io.mywish.scanner.model.NewBlockEvent;
import io.mywish.scanner.model.NewBtcBlockEvent;
import io.mywish.scanner.services.EventPublisher;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NetworkStuckMonitor {
    private final ConcurrentHashMap<NetworkType, LastEvent> lastEvents = new ConcurrentHashMap<>();
    @Value("${io.lastwill.eventscan.network-stuck.interval.btc}")
    private long btcInterval;
    @Value("${io.lastwill.eventscan.network-stuck.interval.eth}")
    private long ethInterval;

    @Autowired
    private EventPublisher eventPublisher;

    @EventListener
    public void newBlockEvent(NewBlockEvent event) {
        lastEvents.put(
                event.getNetworkType(),
                new LastEvent(
                        Instant.ofEpochSecond(event.getBlock().getTimestamp().longValue()),
                        event.getBlock().getNumber().longValue()
                )
        );
    }


    @EventListener
    public void newBtcBlockEvent(NewBtcBlockEvent event) {
        lastEvents.put(
                event.getNetworkType(),
                new LastEvent(
                        Instant.ofEpochSecond(event.getBlock().getTimeSeconds()),
                        event.getBlockNumber()
                )
        );
    }

    @Scheduled(fixedDelayString = "${io.lastwill.eventscan.network-stuck.interval.eth}", initialDelayString = "${io.lastwill.eventscan.network-stuck.interval.eth}")
    protected void checkEth() {
        final Instant now = Instant.now();
        lastEvents.keySet()
                .stream()
                .filter(networkType -> networkType != NetworkType.BTC_MAINNET && networkType != NetworkType.BTC_TESTNET_3)
                .forEach(networkType -> {
                    LastEvent lastEvent = lastEvents.get(networkType);
                    // last block + interval is in future
                    if (lastEvent.timestamp.plusSeconds(ethInterval).isAfter(now)) {
                        return;
                    }

                    eventPublisher.publish(new NetworkStuckEvent(networkType, lastEvent.timestamp, lastEvent.blockNo));
                });
    }

    @Scheduled(fixedDelayString = "${io.lastwill.eventscan.network-stuck.interval.btc}", initialDelayString = "${io.lastwill.eventscan.network-stuck.interval.btc}")
    protected void checkBtc() {
        final Instant now = Instant.now();
        lastEvents.keySet()
                .stream()
                .filter(networkType -> networkType == NetworkType.BTC_MAINNET || networkType == NetworkType.BTC_TESTNET_3)
                .forEach(networkType -> {
                    LastEvent lastEvent = lastEvents.get(networkType);
                    // last block + interval is in future
                    if (lastEvent.timestamp.plusSeconds(btcInterval).isAfter(now)) {
                        return;
                    }

                    eventPublisher.publish(new NetworkStuckEvent(networkType, lastEvent.timestamp, lastEvent.blockNo));
                });
    }

    @Getter
    private static class LastEvent {
        private final Instant timestamp;
        private final long blockNo;

        private LastEvent(Instant timestamp, long blockNo) {
            this.timestamp = timestamp;
            this.blockNo = blockNo;
        }
    }
}
