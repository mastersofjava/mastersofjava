package nl.moj.server.metrics;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OperationMetrics {

    String name;
    Double min;
    Double mean;
    Double max;
}
