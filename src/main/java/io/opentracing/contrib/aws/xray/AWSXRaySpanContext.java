package io.opentracing.contrib.aws.xray;

import io.opentracing.SpanContext;

import java.util.HashMap;
import java.util.Map;

/**
 * In AWS, the span context usually only needs to hold the trace header
 * information (current trace ID, parent segment ID, sampling decision) such
 * that this can be propagated to child processes.
 *
 * @author ashley.mercer@skylightipv.com
 * @see io.opentracing.SpanContext
 * @see com.amazonaws.xray.entities.TraceHeader
 */
class AWSXRaySpanContext implements SpanContext {

    private final Map<String, String> baggage;

    AWSXRaySpanContext(Map<String, String> baggage) {
        this.baggage = new HashMap<>(baggage);
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return baggage.entrySet();
    }

    Map<String, String> getBaggage() {
        return baggage;
    }

    void setBaggageItem(String key, String value) {
        baggage.put(key, value);
    }

    String getBaggageItem(String key) {
        return baggage.get(key);
    }
}
