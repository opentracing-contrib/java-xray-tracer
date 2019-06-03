package io.opentracing.contrib.aws.xray;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.contexts.LambdaSegmentContext;
import com.amazonaws.xray.entities.*;
import io.opentracing.*;
import io.opentracing.propagation.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Top-level OpenTracing {@link Tracer} implementation which is backed
 * by the AWS X-Ray client libraries.
 *
 * @see <a href="https://opentracing.io">https://opentracing.io</a>
 * @see <a href="https://docs.aws.amazon.com/xray/latest/devguide/aws-xray.html">https://docs.aws.amazon.com/xray/latest/devguide/aws-xray.html</a>
 * @author ashley.mercer@skylightipv.com
 */
@SuppressWarnings("WeakerAccess")
public class AWSXRayTracer implements Tracer {

    private static final Logger log = LoggerFactory.getLogger(AWSXRayTracer.class);

    private final AWSXRayRecorder xRayRecorder;
    private final AWSXRayScopeManager scopeManager;

    public AWSXRayTracer(AWSXRayRecorder xRayRecorder) {
        this.xRayRecorder = xRayRecorder;
        this.scopeManager = new AWSXRayScopeManager(xRayRecorder);
    }

    @Override
    public ScopeManager scopeManager() {
        return scopeManager;
    }

    @Override
    public Span activeSpan() {
        return scopeManager.activeSpan();
    }

    @Override
    public AWSXRaySpanBuilder buildSpan(String operationName) {
        return new AWSXRaySpanBuilderImpl(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        throw new UnsupportedOperationException("SpanContext propagation is not currently supported");
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        throw new UnsupportedOperationException("SpanContext propagation is not currently supported");
    }

    /**
     * Additional API for {@link SpanBuilder} which can be used to modify
     * X-Ray behaviour directly.
     */
    public interface AWSXRaySpanBuilder extends SpanBuilder {

        /**
         * Attempt to send an in-progress span back to X-Ray as soon as
         * it starts; this can be useful for long-running spans or those
         * which trigger downstream tasks, since the default behaviour
         * is to only send trace data to X-Ray once the whole tree of
         * spans is complete.
         */
        AWSXRaySpanBuilder sendOnStart();
    }

    /**
     * AWS-specific {@link io.opentracing.Tracer.SpanBuilder} implementation
     */
    private final class AWSXRaySpanBuilderImpl implements AWSXRaySpanBuilder {

        private final String operationName;

        private final Map<String, String>  stringTags;
        private final Map<String, Boolean> booleanTags;
        private final Map<String, Number>  numberTags;

        /**
         * AWS X-Ray timestamps are stored a number of seconds since
         * the UNIX epoch, with the fractional part giving sub-second
         * precision. Defaults to creation time of this builder.
         *
         * @see #withStartTimestamp(long)
         * @see Entity#getStartTime()
         */
        private final AtomicReference<Double> startTimestampEpochSeconds;

        /**
         * @see SpanBuilder#ignoreActiveSpan()
         */
        private final AtomicReference<Boolean> ignoreActiveSpan;

        /**
         * @see AWSXRaySpanBuilder#sendOnStart()
         */
        private final AtomicReference<Boolean> sendOnStart;

        /**
         * Currently only support a single reference to the parent Span (if
         * it exists). Other references are not supported.
         *
         * @see References
         */
        private final Map<String, SpanContext> references;

        private AWSXRaySpanBuilderImpl(String operationName) {
            this.operationName = operationName;

            this.stringTags = new HashMap<>();
            this.booleanTags = new HashMap<>();
            this.numberTags = new HashMap<>();

            this.startTimestampEpochSeconds = new AtomicReference<>();
            this.ignoreActiveSpan = new AtomicReference<>(false);
            this.sendOnStart = new AtomicReference<>(false);
            this.references = new ConcurrentHashMap<>();
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parent) {
            return addReference(References.CHILD_OF, parent);
        }

        @Override
        public SpanBuilder asChildOf(Span parent) {
            if (parent == null) {
                return this;
            }
            else if (parent instanceof AWSXRaySpan) {
                return addReference(References.CHILD_OF, new CapturingSpanContext((AWSXRaySpan) parent));
            }
            else {
                return addReference(References.CHILD_OF, parent.context());
            }
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            if (references.containsKey(referenceType)) {
                log.warn("Replacing reference of type '" + referenceType + "': multiple references of the same type are not supported by X-Ray");
            }
            references.put(referenceType, referencedContext);
            return this;
        }

        @Override
        public SpanBuilder ignoreActiveSpan() {
            ignoreActiveSpan.set(true);
            return this;
        }

        @Override
        public AWSXRaySpanBuilder sendOnStart() {
            sendOnStart.set(true);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, String value) {
            stringTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, boolean value) {
            booleanTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, Number value) {
            numberTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds) {
            startTimestampEpochSeconds.set(microseconds / 1000.0 / 1000.0);
            return this;
        }

        @Override
        @Deprecated
        public Span startManual() {
            return start();
        }

        @Override
        public Scope startActive(boolean finishSpanOnClose) {
            final Span span = start();
            return scopeManager.activate(span, finishSpanOnClose);
        }

        @Override
        public Span start() {

            // X-Ray only supports parent-child relationships between spans
            // (OpenTracing allows for other references e.g. FOLLOWS_FROM)
            references.forEach((key, value) -> {
                if (!References.CHILD_OF.equals(key)) {
                    log.warn("Ignoring reference of type '" + key + "': references of this type are not supported by X-Ray");
                }
            });

            // If an explicit CHILD_OF reference is set, this should override
            // any (implicit) reference to the current trace entity
            final Entity originalTraceEntity = xRayRecorder.getTraceEntity();
            final SpanContext explicitParentContext = references.get(References.CHILD_OF);

            final Entity parentEntity;
            final Map<String, String> parentBaggage;

            // Because X-Ray an OpenTracing maintain their references to the
            // "current" trace separately, we can be in one of four possible states:
            //
            // 1. an explicit parent is set, and it has captured a full AWSXRaySpan
            //    i.e. this is an in-memory Span with a real X-Ray Entity
            //
            if (explicitParentContext instanceof CapturingSpanContext) {
                parentEntity = ((CapturingSpanContext) explicitParentContext).span.getEntity();
                parentBaggage = AWSXRayUtils.extract(explicitParentContext.baggageItems());
            }

            // 2. an explicit parent is set but it doesn't have an X-Ray Entity
            //    attached: we can present a FacadeSegment to X-Ray
            //    TODO extract the real underlying trace and span IDs from explicitParentContext
            //
            else if (explicitParentContext != null) {
                parentEntity = new FacadeSegment(xRayRecorder, null, null, null);
                parentBaggage = AWSXRayUtils.extract(explicitParentContext.baggageItems());
            }

            // 3. no explicit parent is set, but ignoreActiveSpan has been set so
            //    make sure the parent Entity is null (i.e. we'll create a new
            //    Segment in X-Ray terms)
            //
            else if (ignoreActiveSpan.get()) {
                parentEntity = null;
                parentBaggage = Collections.emptyMap();
            }

            // 4. no explicit parent, and ignoreActiveSpan is not set so create an
            //    implicit reference to the current trace entity
            //
            else {
                parentEntity = originalTraceEntity;
                parentBaggage = Collections.emptyMap();
            }

            // X-Ray automatically maintains internal references between Segments and
            // Subsegments - rather than trying to replicate that logic here, we cheat
            // by (temporarily) overwriting the parent trace Entity, creating the new
            // Entity, then setting it back once we're done
            xRayRecorder.setTraceEntity(parentEntity);

            // Special case when running in AWS Lambda: the Lambda infrastructure
            // creates a top-level trace Segment to which we do not have access, so
            // creating another Segment here would be an error. Instead, we need to
            // forcibly create a Subsegment.
            final boolean isAwsLambda = xRayRecorder.getSegmentContextResolverChain().resolve() instanceof LambdaSegmentContext;

            final Entity childEntity = (xRayRecorder.getTraceEntity() == null && !isAwsLambda) ?
                    xRayRecorder.beginSegment(operationName) :
                    xRayRecorder.beginSubsegment(operationName);

            xRayRecorder.setTraceEntity(originalTraceEntity);

            // AWS X-Ray doesn't support the notion of "not-yet-started" segments
            // so set the Entity to be "in progress"
            childEntity.setInProgress(true);

            // Default to "now" if an explicit start time wasn't set
            startTimestampEpochSeconds.compareAndSet(null, Instant.now().toEpochMilli() / 1000.0);
            childEntity.setStartTime(startTimestampEpochSeconds.get());

            // Baggage items should be carried over from the parent Span's
            // context (if it exists) to the child Span
            final AWSXRaySpanContext newSpanContext = new AWSXRaySpanContext(parentBaggage);

            // Defer to AWSXRaySpan to set tag values since this will handle
            // converting to X-Ray's naming conventions and format
            final AWSXRaySpan newSpan = new AWSXRaySpan(childEntity, newSpanContext);
            stringTags.forEach(newSpan::setTag);
            booleanTags.forEach(newSpan::setTag);
            numberTags.forEach(newSpan::setTag);

            // Allow segments to be explicitly sent back to X-Ray at the time
            // the span is start()-ed - this can be useful so we can see the
            // in-progress span in X-Ray
            if (sendOnStart.get()) {
                if (newSpan.getEntity() instanceof Segment) {
                    xRayRecorder.sendSegment((Segment) newSpan.getEntity());
                }
                else if (newSpan.getEntity() instanceof Subsegment) {
                    xRayRecorder.sendSubsegment((Subsegment) newSpan.getEntity());
                }
            }

            return newSpan;
        }
    }

    /**
     * Parent-child relationships between Spans are typically only defined in
     * terms of the SpanContext (i.e. we only need to know the parent span's
     * trace and span ID). However, X-Ray also holds directly object references
     * to the underlying Segment and Subsegment instances, so try to capture
     * the full AWSXRaySpan instance here if we can.
     */
    private static final class CapturingSpanContext implements SpanContext {
        private final AWSXRaySpan span;

        public CapturingSpanContext(AWSXRaySpan span) {
            this.span = span;
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems() {
            return span.context().baggageItems();
        }
    }
}
