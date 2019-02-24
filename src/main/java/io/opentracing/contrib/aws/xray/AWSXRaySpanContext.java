package io.opentracing.contrib.aws.xray;

import io.opentracing.SpanContext;

import java.util.HashMap;
import java.util.Map;

/**
 * There's no real need to implement {@link SpanContext} separately in AWS
 * because the Amazon client libraries will automatically add the relevant
 * HTTP headers to all downstream service requests. This is mostly here for
 * completeness, and is not intended to be used.
 *
 * @author ashley.mercer@skylightipv.com
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
