package io.opentracing.contrib.aws.xray;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.SegmentImpl;
import io.opentracing.Tracer;

import java.util.Collections;

/**
 * @author ashley.mercer@skylightipv.com
 */
abstract class AWSXRayTestParent {

    final AWSXRayRecorder awsxRayRecorder = AWSXRayRecorderBuilder.defaultRecorder();

    /**
     * Make sure this reference stays as pure {@link Tracer} since we don't
     * want to rely on implementation-specific details or return types.
     */
    final Tracer tracer = new AWSXRayTracer(awsxRayRecorder);

    /**
     * @param operationName the operation name
     */
    AWSXRaySpan mockSpan(String operationName) {
        return new AWSXRaySpan(
                new SegmentImpl(awsxRayRecorder, operationName),
                new AWSXRaySpanContext(Collections.emptyMap()));
    }
}
