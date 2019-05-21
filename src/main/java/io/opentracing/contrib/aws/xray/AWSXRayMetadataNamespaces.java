package io.opentracing.contrib.aws.xray;

import com.amazonaws.xray.entities.Entity;
import io.opentracing.Span;

/**
 * When storing tagged values on X-Ray traces, we try to use the
 * standard X-Ray naming conventions where possible; other values
 * will end up being stored in the "metadata" part of the trace
 * which requires at least a three-part key:
 *
 * <ul>
 *     <li>the literal value "metadata"</li>
 *     <li>a "namespace" which logically groups values</li>
 *     <li>one or more sub-keys within the namespace</li>
 * </ul>
 *
 * This class provides some different metadata namespaces which can
 * be used externally if required.
 *
 * When storing values on the {@link Span}, if the tag name does not
 * correspond to one of the standard "known" names, and also does not
 * conform to the above pattern, then it will be coerced to the above
 * by being placed in the {@link #DEFAULT} namespace. Examples:
 *
 * <table summary="metadata namespace mappings">
 *     <tr>
 *         <th>Original key</th>
 *         <th>X-Ray key</th>
 *         <th>Comments</th>
 *     </tr>
 *     <tr>
 *         <td><tt>db.user</tt></td>
 *         <td><tt>sql.user</tt></td>
 *         <td>X-Ray traces use a "sql" element for database info</td>
 *     </tr>
 *     <tr>
 *         <td><tt>foo_value</tt></td>
 *         <td><tt>metadata.default.foo_value</tt></td>
 *         <td>Stored in "metadata" under the "default" namespace</td>
 *     </tr>
 *     <tr>
 *         <td><tt>widget.bar_value</tt></td>
 *         <td><tt>metadata.widget.bar_value</tt></td>
 *         <td>Stored in "metadata" under the custom "widget" namespace</td>
 *     </tr>
 * </table>
 *
 * @author ashley.mercer@skylightipv.com
 * @see Entity#getMetadata()
 * @see <a href="https://docs.aws.amazon.com/xray/latest/devguide/xray-api-segmentdocuments.html">X-Ray Segment documentation</a>
 */
@SuppressWarnings("WeakerAccess")
public final class AWSXRayMetadataNamespaces {

    private AWSXRayMetadataNamespaces(){}

    /**
     * The default namespace for anything which doesn't explicitly
     * specify its own namespace.
     */
    public static final String DEFAULT = "default";

    /**
     * Log statements are also stored in the metadata since X-Ray doesn't
     * have another mechanism to expose them currently.
     */
    public static final String LOG = "log";
}
