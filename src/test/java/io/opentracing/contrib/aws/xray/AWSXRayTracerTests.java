package io.opentracing.contrib.aws.xray;

import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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
    @DisplayName("fail on SpanContext injection")
    void contextInject() {
        final TextMap emptyTextMap = new TextMapExtractAdapter(Collections.emptyMap());
        final SpanContext emptyContext = new AWSXRaySpanContext(Collections.emptyMap());
        assertThrows(UnsupportedOperationException.class, () -> tracer.inject(emptyContext, Format.Builtin.TEXT_MAP, emptyTextMap));
    }

    @Test
    @DisplayName("fail on SpanContext extraction")
    void contextExtract() {
        final TextMap emptyTextMap = new TextMapExtractAdapter(Collections.emptyMap());
        assertThrows(UnsupportedOperationException.class, () -> tracer.extract(Format.Builtin.TEXT_MAP, emptyTextMap));
    }
}
