package io.opentracing.contrib.aws.xray;

import io.opentracing.SpanContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ashley.mercer@skylightipv.com
 */
@SuppressWarnings("WeakerAccess")
public final class AWSXRayUtils {

    private AWSXRayUtils(){}

    /**
     * Utility method to copy baggage values from a {@link SpanContext}
     * back into a vanilla Map, which is much easier to work with.
     *
     * @param baggage the view of underlying baggage
     * @return a <em>new, unmodifiable</em> Map instance containing all
     * of the values copied from the underlying baggage
     */
    public static Map<String, String> extract(Iterable<Map.Entry<String, String>> baggage) {
        final Map<String, String> targetMap = new HashMap<>();
        baggage.forEach(e -> targetMap.put(e.getKey(), e.getValue()));
        return Collections.unmodifiableMap(targetMap);
    }
}
