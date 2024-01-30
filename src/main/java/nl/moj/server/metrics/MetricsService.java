package nl.moj.server.metrics;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import lombok.RequiredArgsConstructor;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.test.model.TestAttempt;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private Timer compileDuration;
    private Timer compileRoundTrip;
    private Timer testDuration;
    private Timer testRoundTrip;
    private Timer submitDuration;
    private Timer submitRoundTrip;

    @PostConstruct
    public void init() {
        compileDuration = registerTimer("moj.compile.duration");
        compileRoundTrip = registerTimer("moj.compile.round-trip");
        testDuration = registerTimer("moj.test.duration");
        testRoundTrip = registerTimer("moj.test.round-trip");
        submitDuration = registerTimer("moj.submit.duration");
        submitRoundTrip = registerTimer("moj.submit.round-trip");
    }

    public void reset() {
        meterRegistry.remove(compileRoundTrip.getId());
        meterRegistry.remove(compileDuration.getId());
        meterRegistry.remove(testRoundTrip.getId());
        meterRegistry.remove(testDuration.getId());
        meterRegistry.remove(submitRoundTrip.getId());
        meterRegistry.remove(submitDuration.getId());
        init();
    }

    public void registerCompileAttemptMetrics(CompileAttempt ca) {
        if (ca != null && ca.getDateTimeRegister() != null && ca.getDateTimeStart() != null && ca.getDateTimeEnd() != null) {
            if (compileDuration != null) {
                compileDuration.record(Duration.between(ca.getDateTimeStart(), ca.getDateTimeEnd()));
            }
            if (compileRoundTrip != null) {
                compileRoundTrip.record(Duration.between(ca.getDateTimeRegister(), ca.getDateTimeEnd()));
            }
        }
    }

    public void registerTestAttemptMetrics(TestAttempt ta) {
        if (ta != null && ta.getDateTimeRegister() != null && ta.getDateTimeStart() != null && ta.getDateTimeEnd() != null) {
            registerCompileAttemptMetrics(ta.getCompileAttempt());
            if (testDuration != null) {
                testDuration.record(Duration.between(ta.getDateTimeStart(), ta.getDateTimeEnd()));
            }
            if (testRoundTrip != null) {
                testRoundTrip.record(Duration.between(ta.getDateTimeRegister(), ta.getDateTimeEnd()));
            }
        }
    }

    public void registerSubmitAttemptMetrics(SubmitAttempt sa) {
        if (sa != null && sa.getDateTimeRegister() != null && sa.getDateTimeStart() != null && sa.getDateTimeEnd() != null) {
            registerTestAttemptMetrics(sa.getTestAttempt());
            if (submitDuration != null) {
                submitDuration.record(Duration.between(sa.getDateTimeStart(), sa.getDateTimeEnd()));
            }
            if (submitRoundTrip != null) {
                submitRoundTrip.record(Duration.between(sa.getDateTimeRegister(), sa.getDateTimeEnd()));
            }
        }
    }

    private Timer registerTimer(String metricName) {
        if (meterRegistry != null) {
            return Timer.builder(metricName)
                    .distributionStatisticExpiry(Duration.ofHours(24))
                    .publishPercentiles(0.0, 0.3, 0.5, 0.9)
                    .publishPercentileHistogram()
                    .register(meterRegistry);
        }
        return null;
    }

    private <T> void registerGauge(String metricName, T state, ToDoubleFunction<T> f) {
        if (meterRegistry != null) {
            Gauge.builder(metricName, state, f).register(meterRegistry);
        }
    }

    public List<Meter> getMeters() {
        return meterRegistry.getMeters();
    }

    public MetricsVO getMetrics() {
        return MetricsVO.builder()
                .operationMetrics(getOperationMetrics())
                .queueMetrics(getQueueMetrics())
                .build();
    }

    public Set<OperationMetrics> getOperationMetrics() {

        record TimerSnapshot(String name, HistogramSnapshot snapshot) {
        }
        Set<TimerSnapshot> timers = Set.of(
                new TimerSnapshot("Compile Duration", compileDuration.takeSnapshot()),
                new TimerSnapshot("Compile Round Trip", compileRoundTrip.takeSnapshot()),
                new TimerSnapshot("Test Duration", testDuration.takeSnapshot()),
                new TimerSnapshot("Test Round Trip", testRoundTrip.takeSnapshot()),
                new TimerSnapshot("Submit Duration", submitDuration.takeSnapshot()),
                new TimerSnapshot("Submit Round Trip", submitRoundTrip.takeSnapshot()));

        return timers.stream().map(t -> OperationMetrics.builder()
                .name(t.name())
                .max(t.snapshot().max(TimeUnit.SECONDS))
                .mean(t.snapshot().mean(TimeUnit.SECONDS))
                .min(Arrays.stream(t.snapshot().percentileValues()).filter(pv -> pv.percentile() == 0.0)
                        .map(pv -> pv.value(TimeUnit.SECONDS)).findFirst().orElse(0.0))
                .build()).collect(Collectors.toSet());
    }

    public Set<QueueMetrics> getQueueMetrics() {

        List<String> METER_NAMES = List.of(
                "artemis.message.count",
                "artemis.messages.acknowledged",
                "artemis.messages.added",
                "artemis.messages.expired",
                "artemis.messages.killed");

        Map<String, Map<String, Double>> values = getMeters().stream()
                .filter(m -> METER_NAMES.contains(m.getId().getName()))
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
