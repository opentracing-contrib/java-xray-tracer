package io.opentracing.contrib.aws.xray;

import com.amazonaws.xray.entities.Segment;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ashley.mercer@skylightipv.com
 */
class AWSXRaySpanTests extends AWSXRayTestParent {

    @Test
    @DisplayName("set ERROR tag on underlying Entity")
    void tagError() {
        final AWSXRaySpan span = mockSpan("test-tag-is-error");
        span.setTag(Tags.ERROR.getKey(), true);

        assertTrue(span.getEntity().isError());
        assertFalse(span.getEntity().isFault());
    }

    @Test
    @DisplayName("set FAULT tag on underlying Entity")
    void tagFault() {
        final AWSXRaySpan span = mockSpan("test-tag-is-fault");
        span.setTag(AWSXRayTags.FAULT.getKey(), true);

        assertFalse(span.getEntity().isError());
        assertTrue(span.getEntity().isFault());
    }

    @Test
    @DisplayName("set THROTTLE tag on underlying Entity")
    void tagThrottle() {
        final AWSXRaySpan span = mockSpan("test-tag-is-throttle");
        span.setTag(AWSXRayTags.THROTTLE.getKey(), true);

        assertFalse(span.getEntity().isFault());
        assertTrue(span.getEntity().isThrottle());
    }

    @Test
    @DisplayName("set IS_SAMPLED tag on underlying Entity")
    void tagSampled() {
        final AWSXRaySpan span = mockSpan("test-tag-is-sampled");
        assertTrue(span.getEntity() instanceof Segment);

        span.setTag(AWSXRayTags.IS_SAMPLED.getKey(), false);
        assertFalse(((Segment) span.getEntity()).isSampled());

        span.setTag(AWSXRayTags.IS_SAMPLED.getKey(), true);
        assertTrue(((Segment) span.getEntity()).isSampled());
    }

    @Test
    @DisplayName("set USER tag on underlying Entity")
    void tagUser() {
        final String expectedUser = "test.user@example.com";

        final AWSXRaySpan span = mockSpan("test-tag-user");
        span.setTag(AWSXRayTags.USER.getKey(), expectedUser);

        assertTrue(span.getEntity() instanceof Segment);
        assertEquals(expectedUser, ((Segment) span.getEntity()).getUser());
    }

    @Test
    @DisplayName("set ORIGIN tag on underlying Entity")
    void tagOrigin() {
        final String expectedOrigin = "AWS::EC2::Instance";

        final AWSXRaySpan span = mockSpan("test-tag-origin");
        span.setTag(AWSXRayTags.ORIGIN.getKey(), expectedOrigin);

        assertTrue(span.getEntity() instanceof Segment);
        assertEquals(expectedOrigin, ((Segment) span.getEntity()).getOrigin());
    }

    @Test
    @DisplayName("tag annotations values")
    void tagAnnotations() {
        testSpecialTags("annotations", s -> s.getEntity().getAnnotations());
    }

    @Test
    @DisplayName("tag AWS values")
    void tagAws() {
        testSpecialTags("aws", s -> s.getEntity().getAws());
    }

    @Test
    @DisplayName("tag HTTP values")
    void tagHTTP() {
        testSpecialTags("http", s -> s.getEntity().getHttp());
    }

    @Test
    @DisplayName("tag service values")
    void tagService() {
        testSpecialTags("service", s -> ((Segment) s.getEntity()).getService());
    }

    @Test
    @DisplayName("tag SQL values")
    void tagSQL() {
        testSpecialTags("sql", s -> s.getEntity().getSql());
    }

    @Test
    @DisplayName("tag metadata with empty key")
    void tagMetadataEmptyKey() {
        testMetadataTags("", AWSXRayMetadataNamespaces.DEFAULT, "");
    }

    @Test
    @DisplayName("tag metadata with no prefix, no namespace")
    void tagMetadataNoPrefixNoNamespace() {
        testMetadataTags("foo", AWSXRayMetadataNamespaces.DEFAULT, "foo");
    }
    @Test
    @DisplayName("tag metadata with no prefix, no namespace, key=metadata")
    void tagMetadataNoPrefixNoNamespaceAsNamespace() {
        testMetadataTags("metadata", AWSXRayMetadataNamespaces.DEFAULT, "metadata");
    }

    @Test
    @DisplayName("tag metadata with no namespace")
    void tagMetadataNoNamespace() {
        testMetadataTags("metadata.foo", AWSXRayMetadataNamespaces.DEFAULT, "foo");
    }

    @Test
    @DisplayName("tag metadata with no prefix")
    void tagMetadataNoPrefix() {
        testMetadataTags("bar.foo", "bar", "foo");
    }

    @Test
    @DisplayName("tag metadata with full name")
    void tagMetadataFullName() {
        testMetadataTags("metadata.bar.foo", "bar", "foo");
    }

    @Test
    @DisplayName("tag metadata with nested key")
    void tagMetadataNestedKey() {
        final AWSXRaySpan span = mockSpan("test-tag-metadata-nested-key");
        span.setTag("metadata.default.nested.is_test", true);
        span.setTag("metadata.default.nested.counter", 42);
        span.setTag("metadata.default.nested.service", "backend");

        @SuppressWarnings("unchecked")
        final Map<String, Object> targetMap = (Map<String, Object>) span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.DEFAULT).get("nested");
        assertEquals(true,      targetMap.get("is_test"));
        assertEquals(42,        targetMap.get("counter"));
        assertEquals("backend", targetMap.get("service"));
    }

    private void testSpecialTags(String keyPrefix, Function<AWSXRaySpan, Map<String, Object>> getTargetMap) {
        final AWSXRaySpan span = mockSpan("test-tag-" + keyPrefix);
        span.setTag(keyPrefix + ".is_test", true);
        span.setTag(keyPrefix + ".counter", 42);
        span.setTag(keyPrefix + ".service", "backend");

        final Map<String, Object> targetMap = getTargetMap.apply(span);
        assertEquals(true,      targetMap.get("is_test"));
        assertEquals(42,        targetMap.get("counter"));
        assertEquals("backend", targetMap.get("service"));
    }

    private void testMetadataTags(String fullKey, String expectedNamespace, String expectedKey) {
        final AWSXRaySpan span = mockSpan("test-tag-metadata-" + fullKey.replaceAll("\\.", "-"));
        span.setTag(fullKey + "_boolean", true);
        span.setTag(fullKey + "_number",  42);
        span.setTag(fullKey + "_string",  "backend");

        final Map<String, Object> targetMap = span.getEntity().getMetadata().get(expectedNamespace);
        assertEquals(true,      targetMap.get(expectedKey + "_boolean"));
        assertEquals(42,        targetMap.get(expectedKey + "_number"));
        assertEquals("backend", targetMap.get(expectedKey + "_string"));
    }

    private final String LOG_MESSAGE = "This is a log message";
    private final Map<String, Object> LOG_OBJECT = new HashMap<>();
    {
        LOG_OBJECT.put(Fields.MESSAGE, LOG_MESSAGE);
        LOG_OBJECT.put(Fields.EVENT,   "timeout");
    }

    @Test
    @DisplayName("log raw event")
    void logRawEvent() {
        final AWSXRaySpan span = mockSpan("test-log-raw-event");
        span.log(LOG_MESSAGE);

        assertNotNull(span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG));
        assertEquals(1, span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG).size());
    }

    @Test
    @DisplayName("log raw event with timestamp")
    void logRawEventWithTimestamp() {
        final AWSXRaySpan span = mockSpan("test-log-raw-event-with-timestamp");
        span.log(Instant.now().toEpochMilli() * 1000, LOG_MESSAGE);

        assertNotNull(span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG));
        assertEquals(1, span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG).size());
    }

    @Test
    @DisplayName("log structured event")
    void logStructuredEvent() {
        final AWSXRaySpan span = mockSpan("test-log-structured-event");
        span.log(LOG_OBJECT);

        assertNotNull(span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG));
        assertEquals(1, span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG).size());
    }

    @Test
    @DisplayName("log structured event with timestamp")
    void logStructuredEventWithTimestamp() {
        final AWSXRaySpan span = mockSpan("test-log-structured-event");
        span.log(Instant.now().toEpochMilli() * 1000, LOG_OBJECT);

        assertNotNull(span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG));
        assertEquals(1, span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG).size());
    }

    @Test
    @DisplayName("log multiple events")
    void logMultipleEvents() throws InterruptedException {
        final AWSXRaySpan span = mockSpan("test-log-multiple-events");

        // Currently only support millisecond precision for logs, so
        // multiple logs can have the same timestamp which fails the test
        span.log(LOG_MESSAGE);
        Thread.sleep(5);
        span.log(LOG_MESSAGE);
        Thread.sleep(5);
        span.log(LOG_MESSAGE);

        assertEquals(3, span.getEntity().getMetadata().get(AWSXRayMetadataNamespaces.LOG).size());
    }

    @Test
    @DisplayName("set baggage")
    void setBaggage() {
        final AWSXRaySpan span = mockSpan("test-set-baggage");
        span.setBaggageItem("baggage_key", "value");

        assertNotNull(span.context().getBaggage());
        assertFalse(span.context().getBaggage().isEmpty());
        assertTrue(span.context().baggageItems().iterator().hasNext());
        assertEquals("value", span.getBaggageItem("baggage_key"));
        assertEquals("value", span.context().getBaggageItem("baggage_key"));
    }

    @Test
    @DisplayName("refuse to set operation name after construction")
    void setOperationName() {
        final AWSXRaySpan span = mockSpan("test-set-operation-name");
        assertThrows(Exception.class, () -> span.setOperationName("some-other-name"));
    }

    @Test
    @DisplayName("close the underlying X-Ray Entity on finish")
    void finish() {

        // Fake scope management here by setting the current trace Entity
        final AWSXRaySpan span = mockSpan("test-finish");
        awsxRayRecorder.setTraceEntity(span.getEntity());
        assertTrue(span.getEntity().isInProgress());

        span.finish();

        // X-Ray automatically unsets the current trace Entity on completion
        assertFalse(span.getEntity().isInProgress());
        assertNull(awsxRayRecorder.getTraceEntity());
    }

    @Test
    @DisplayName("ignore repeated calls to finish")
    void finishMultiple() {

        // Fake scope management here by setting the current trace entity
        final AWSXRaySpan span = mockSpan("test-finish");
        awsxRayRecorder.setTraceEntity(span.getEntity());
        assertTrue(span.getEntity().isInProgress());

        // If the second call to finish() proceeded, X-Ray would throw
        // an exception because the underlying Entity has already completed
        span.finish();
        span.finish();

        // X-Ray automatically unsets the current trace Entity on completion
        assertFalse(span.getEntity().isInProgress());
        assertNull(awsxRayRecorder.getTraceEntity());
    }
}
