package nl.moj.server.metrics;

import java.util.*;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final MeterRegistry meterRegistry;

    public List<Meter> getMeters() {
        return meterRegistry.getMeters();
    }

    public Set<QueueMetrics> getQueueMetrics() {

        List<String> METER_NAMES = List.of(
                "artemis.message.count",
                "artemis.messages.acknowledged",
                "artemis.messages.added",
                "artemis.messages.expired",
                "artemis.messages.killed"
        );

        Map<String, Map<String, Double>> values = getMeters().stream()
                .peek( m -> log.info("Metrics: {}", m.getId().getName()))
                .filter(m -> METER_NAMES.contains(m.getId().getName()))
//                .filter(m -> m.getId().getType() == Meter.Type.COUNTER)
                .filter(m -> m.getId().getTag("queue") != null)
                .collect(Collectors.groupingBy(m -> m.getId().getTag("queue"),
                        TreeMap::new,
                        Collectors.toMap(m -> m.getId().getName(), m -> getOrZero(m))));

        Set<QueueMetrics> metrics = new HashSet<>();
        for (Map.Entry<String, Map<String, Double>> m : values.entrySet()) {
            metrics.add(QueueMetrics.builder()
                    .name(m.getKey())
                    .acknowledged(m.getValue().getOrDefault("artemis.messages.acknowledged", 0.0))
                    .added(m.getValue().getOrDefault("artemis.messages.added", 0.0))
                    .count(m.getValue().getOrDefault("artemis.message.count", 0.0))
                    .expired(m.getValue().getOrDefault("artemis.messages.expired", 0.0))
                    .killed(m.getValue().getOrDefault("artemis.messages.killed", 0.0))
                    .build());
        }

        return metrics;
    }

    private static double getOrZero(Meter m) {
        Iterator<Measurement> it = m.measure().iterator();
        if (it.hasNext()) {
            return it.next().getValue();
        }
        return 0.0;
    }

}
