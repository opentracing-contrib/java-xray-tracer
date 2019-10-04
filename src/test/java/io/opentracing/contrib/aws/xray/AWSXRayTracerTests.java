package io.opentracing.contrib.aws.xray;

import com.amazonaws.xray.entities.TraceHeader;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.propagation.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ashley.mercer@skylightipv.com
 */
class AWSXRayTracerTests extends AWSXRayTestParent {

    @Test
    @DisplayName("store a reference to the current span")
    void storeReference() {
        final Scope scope = tracer
            .buildSpan("simple-span")
            .startActive(true);

        assertNotNull(tracer.activeSpan());
        assertNotNull(tracer.scopeManager().active().span());

        assertEquals(tracer.activeSpan(), scope.span());
        assertEquals(tracer.scopeManager().active().span(), scope.span());

        scope.close();
    }

    @Test
    @DisplayName("succeed on SpanContext injection (empty)")
    void contextInjectEmpty() {
        final TextMap textMap = new TextMapAdapter(new HashMap<>());
        final SpanContext context = new AWSXRaySpanContext(new HashMap<>());

        tracer.inject(context, Format.Builtin.TEXT_MAP, textMap);
        assertFalse(textMap.iterator().hasNext());
    }

    @Test
    @DisplayName("succeed on SpanContext injection (TraceID)")
    void contextInjectTraceId() {
        final TextMap textMap = new TextMapAdapter(new HashMap<>());
        final SpanContext context = new AWSXRaySpanContext(Collections.singletonMap(
            TraceHeader.HEADER_KEY,
            traceHeader.toString()
        ));

        tracer.inject(context, Format.Builtin.TEXT_MAP, textMap);
        assertTrue(textMap.iterator().hasNext());

        final TraceHeader extractedTraceHeader = TraceHeader.fromString(textMap.iterator().next().getValue());
        assertEquals(traceHeader.getRootTraceId(), extractedTraceHeader.getRootTraceId());
        assertEquals(traceHeader.getParentId(), extractedTraceHeader.getParentId());
        assertEquals(traceHeader.getSampled(), extractedTraceHeader.getSampled());
    }

    @Test
    @DisplayName("succeed on SpanContext extraction (empty)")
    void contextExtractEmpty() {
        final TextMap textMap = new TextMapAdapter(new HashMap<>());
        final SpanContext context = tracer.extract(Format.Builtin.TEXT_MAP, textMap);
        assertFalse(context.baggageItems().iterator().hasNext());
    }

    @Test
    @DisplayName("succeed on SpanContext extraction (TraceID)")
    void contextExtractTraceId() {
        final TextMap textMap = new TextMapAdapter(Collections.singletonMap(
            TraceHeader.HEADER_KEY,
            traceHeader.toString()
        ));

        final SpanContext context = tracer.extract(Format.Builtin.TEXT_MAP, textMap);
        assertTrue(context.baggageItems().iterator().hasNext());

        final TraceHeader extractedTraceHeader = TraceHeader.fromString(context.baggageItems().iterator().next().getValue());
        assertEquals(traceHeader.getRootTraceId(), extractedTraceHeader.getRootTraceId());
        assertEquals(traceHeader.getParentId(), extractedTraceHeader.getParentId());
        assertEquals(traceHeader.getSampled(), extractedTraceHeader.getSampled());
    }
}
