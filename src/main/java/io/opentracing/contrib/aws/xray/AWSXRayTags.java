package io.opentracing.contrib.aws.xray;

import io.opentracing.tag.BooleanTag;
import io.opentracing.tag.StringTag;

/**
 * Additional known tags which are specific to AWS X-Ray. In general, users
 * should prefer the standard OpenTracing tag names and conventions, but it
 * can be useful to sometimes take advantage of X-Ray specific functionality.
 *
 * @author ashley.mercer@skylightipv.com
 */
@SuppressWarnings("WeakerAccess")
public final class AWSXRayTags {

    private AWSXRayTags(){}

    /**
     * FAULT indicates whether or not a Span ended in a fault
     * (non-recoverable exception) state.
     */
    public static final BooleanTag FAULT = new BooleanTag("fault");

    /**
     * THROTTLE indicates that the underlying request was throttled,
     * usually caused by resource constraints.
     */
    public static final BooleanTag THROTTLE = new BooleanTag("throttle");

    /**
     * IS_SAMPLED indicates that the current trace segment should be
     * subject to sampling by X-Ray.
     */
    public static final BooleanTag IS_SAMPLED = new BooleanTag("isSampled");

    /**
     * USER is the identifier for the user who initiated this request,
     * typically a logged-in website user or IAM username.
     *
     * Should only be set on top-level trace Spans.
     */
    public static final StringTag USER = new StringTag("user");

    /**
     * ORIGIN indicates the type of AWS resource running the application,
     * value is typically something like "AWS::EC2::Instance"
     */
    public static final StringTag ORIGIN = new StringTag("origin");
}
