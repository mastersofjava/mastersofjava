package nl.moj.server.metrics;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class QueueMetrics {

    String name;
    Double count;
    Double acknowledged;
    Double added;
    Double expired;
    Double killed;

}
