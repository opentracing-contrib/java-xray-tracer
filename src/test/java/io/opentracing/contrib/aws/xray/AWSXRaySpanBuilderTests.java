package io.opentracing.contrib.aws.xray;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import io.opentracing.Scope;
import io.opentracing.Span;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ashley.mercer@skylightipv.com
 */
class AWSXRaySpanBuilderTests extends AWSXRayTestParent {

    private final AWSXRayRecorder recorder = AWSXRayRecorderBuilder.defaultRecorder();
    private final AWSXRayTracer tracer = new AWSXRayTracer(recorder);

    @Test
    @DisplayName("set span name")
    void setSpanName() {
        final Span span = tracer
                .buildSpan("test-span-name")
                .start();

        assertEquals("test-span-name", ((AWSXRaySpan) span).getEntity().getName());
    }

    @Test
    @DisplayName("set active span")
    void setActiveSpan() {
        final Scope activeScope = tracer
                .buildSpan("test-active-span")
                .startActive(true);

        assertEquals(activeScope.span(), tracer.activeSpan());

        activeScope.close();
    }

    @Test
    @DisplayName("set implicit active span as parent")
    void setImplicitParentSpan() {
        final Scope parentScope = tracer
                .buildSpan("parent-span")
                .startActive(true);

        final Scope childScope =  tracer
                .buildSpan("child-span")
                .startActive(true);

        final Entity parentEntity = ((AWSXRayScope) parentScope).span().getEntity();
        final Entity childEntity  = ((AWSXRayScope) childScope).span().getEntity();

        assertFalse(parentEntity.getSubsegments().isEmpty());
        assertEquals(parentEntity, childEntity.getParent());

        childScope.close();
        parentScope.close();
    }

    @Test
    @DisplayName("set explicit span as parent")
    void setExplicitParentSpan() {

        // NB we *don't* startActive here - assume this Span
        // object came from somewhere else in the code
        final AWSXRaySpan explicitParentSpan = mockSpan("explicit-parent-span");

        // This implicit parent should be ignored by SpanBuilder
        // when we set the explicit parent
        final Scope implicitParentScope = tracer
                .buildSpan("implicit-parent-span")
                .startActive(true);

        final Scope childScope =  tracer
                .buildSpan("child-span")
                .asChildOf(explicitParentSpan)
                .startActive(true);

        final Entity explicitParentEntity = explicitParentSpan.getEntity();
        final Entity implicitParentEntity = ((AWSXRayScope) implicitParentScope).span().getEntity();
        final Entity childEntity  = ((AWSXRayScope) childScope).span().getEntity();

        assertFalse(explicitParentEntity.getSubsegments().isEmpty());
        assertTrue(implicitParentEntity.getSubsegments().isEmpty());

        assertEquals(explicitParentEntity, childEntity.getParent());
        assertNotEquals(explicitParentEntity.getId(), childEntity.getId());

        childScope.close();
        implicitParentScope.close();
    }

    @Test
    @DisplayName("ignore implicit active span on ignoreActiveSpan")
    void ignoreImplicitParentSpan() {
        final Scope parentScope = tracer
                .buildSpan("parent-span")
                .startActive(true);

        final Scope childScope = tracer
                .buildSpan("child-span")
                .ignoreActiveSpan()
                .startActive(true);

        final Entity parentEntity = ((AWSXRayScope) parentScope).span().getEntity();
        final Entity childEntity = ((AWSXRayScope) childScope).span().getEntity();

        assertTrue(parentEntity.getSubsegments().isEmpty());
        assertNull(childEntity.getParent());
        assertNotEquals(parentEntity.getTraceId(), childEntity.getTraceId());

        childScope.close();
        parentScope.close();
    }

    /**
     * In systems where the surrounding code is using X-Ray directly, but
     * not the OpenTracing API, we should detect if a trace is already in
     * progress. For example, in AWS Lambda functions, the lambda server
     * creates a top-level trace segment for the whole function call
     *
     * @see <a href="https://docs.aws.amazon.com/xray/latest/devguide/xray-services-lambda.html">https://docs.aws.amazon.com/xray/latest/devguide/xray-services-lambda.html</a>
     */
    @Test
    @DisplayName("detect a pre-existing X-Ray trace")
    void detectPreExisting() {
        final Segment parentEntity = awsxRayRecorder.beginSegment("pre-existing-trace");

        final Scope childScope = tracer
                .buildSpan("child-of-pre-existing-trace")
                .startActive(true);

        final Entity childEntity = ((AWSXRayScope) childScope).span().getEntity();

        assertFalse(parentEntity.getSubsegments().isEmpty());
        assertEquals(parentEntity, childEntity.getParent());

        childScope.close();
    }

    @Test
    @DisplayName("set tags correctly")
    @SuppressWarnings("unchecked")
    void setTags() {
        final Scope scope = tracer
                .buildSpan("test-set-tags")
                .withTag("http.request.method", "POST")
                .withTag("http.response.status_code", 503)
                .withTag("fault", true)
                .startActive(true);

        final Entity entity = ((AWSXRayScope) scope).span().getEntity();
        assertEquals("POST", ((Map<String, Object>) entity.getHttp().get("request")).get("method"));
        assertEquals(503,    ((Map<String, Object>) entity.getHttp().get("response")).get("status_code"));
        assertTrue(entity.isFault());

        scope.close();
    }

    @Test
    @DisplayName("set start timestamp correctly")
    void setStartTimestamp() {
        final Scope scope = tracer
                .buildSpan("test-set-start-timestamp")
                .withStartTimestamp(1551016321000000L)
                .startActive(true);

        final Entity entity = ((AWSXRayScope) scope).span().getEntity();
        assertEquals(1551016321.0, entity.getStartTime());

        scope.close();
    }
}
