package nl.moj.server.metrics;

import java.util.Set;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MetricsVO {

    Set<QueueMetrics> queueMetrics;
    Set<OperationMetrics> operationMetrics;
}
