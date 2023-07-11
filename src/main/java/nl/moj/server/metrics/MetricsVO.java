package nl.moj.server.metrics;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class MetricsVO {

    Set<QueueMetrics> queueMetrics;
    Set<OperationMetrics> operationMetrics;
}
