[![Build Status](https://travis-ci.org/skylight-ipv/opentracing-java-aws-xray.svg?branch=master)](https://travis-ci.org/skylight-ipv/opentracing-java-aws-xray)

# opentracing-java-aws-xray
Java OpenTracing implementation backed by AWS X-Ray.

**WARNING: this code is currently in beta: please test thoroughly before deploying to production, and report any issues.**

## Overview

The [OpenTracing specification](https://opentracing.io) is a vendor-neutral API for instrumentation and distributed 
tracing. This library provides an implementation which is backed by the [AWS X-Ray Java SDK](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java.html),
and for the most part just provides a thin wrapper around the underlying X-Ray classes.

## Naming conventions

The OpenTracing standard and the AWS X-Ray system each use different naming conventions for some of the same concepts 
(e.g. HTTP response codes). Since the goal of this project is to largely hide the fact that we're using X-Ray under the 
hood, and to only expose the OpenTracing API:

- client code should prefer using the [OpenTracing naming conventions](https://opentracing.io/specification/conventions/) 
  for tag names (however, if you supply the X-Ray-specific names, values will still end up in the right place)
- this library will silently convert *some* known tag names values to their X-Ray equivalents
- it will also convert *some* known tag names for boolean values directly to set flags on the underlying X-Ray trace
- X-Ray traces further subdivide tagged values into separate sub-objects for e.g. HTTP request and response data:
  - where possible, ensure values end up in the correct place on the trace
  - all other values are stored in the `metadata` section, under a `default` namespace if necessary
  - see the [X-Ray Segment Documents](https://docs.aws.amazon.com/xray/latest/devguide/xray-api-segmentdocuments.html)
    for further details

The following table shows how tag names will be modified to fit the X-Ray format:

| OpenTracing tag name  | X-Ray property name / behaviour |
|-----------------------|---------------------------------|
| `error`               | sets the `isError()` flag       |
| `fault`               | sets the `isFault()` flag       |
| `throttle`            | sets the `isThrottle()` flag    |
| `version`             | `service.version`               |
| `db.instance`         | `sql.url`                       |
| `db.statement`        | `sql.sanitized_query`           |
| `db.type`             | `sql.database_type`             |
| `db.user`             | `sql.user`                      |
| `db.driver`           | `sql.driver`                    |
| `db.version`          | `sql.version`                   |
| `http.method`         | `http.request.method`           |
| `http.url`            | `http.request.url`              |
| `http.client_ip`      | `http.request.client_ip`        |
| `http.user_agent`     | `http.request.user_agent`       |
| `http.status_code`    | `http.response.status`          |
| `http.content_length` | `http.response.content_length`  |
| `foo`                 | `metadata.default.foo`          |
| `widget.foo`          | `metadata.widget.foo`           |

## Known limitations

This library does not currently provide a full implementation of the OpenTracing API: the X-Ray classes themselves 
already provide some of the same features, and in other cases the APIs are incompatible. The following limitations 
currently apply:

#### References

OpenTracing provides for arbitrary [references between spans](https://opentracing.io/specification/#references-between-spans), 
including parent-child and follows-from relationships. In practice X-Ray only supports parent-child relationships, and
each span can have at most one parent. Calls to add references of different types, or multiple parent-child relationships,
will generally be ignored.

#### Context injection / extraction

This library does not currently implement [injection and extraction of SpanContext](https://opentracing.io/specification/#inject-a-spancontext-into-a-carrier):
in most cases it is expected that this library will be used in AWS-hosted systems, and calls between AWS services using
the official SDK will already handle passing trace IDs across.

Support could be added in future if required.

#### Logging

OpenTracing provides methods to add [logs to the trace](https://opentracing.io/specification/#log-structured-data). 
These methods will work as expected: structured data are stored in X-Ray metadata under a "log" namespace, but this
approach isn't advised since the resulting JSON format is clunky. A better approach in AWS is to make use of 
[CloudWatch](https://aws.amazon.com/cloudwatch/).

## AWS compatibility

Since this library mostly just wraps the standard X-Ray classes, it *should* work seamlessly in code which makes use of
multiple AWS services: the standard AWS SDK will add the necessary trace headers automatically, and recover them on 
remote servers (e.g. when invoking lambda functions). However, this hasn't yet been extensively tested, so feedback
and bug reports are very welcome!

## License

This project is licensed under the [Apache 2.0](/LICENSE) License.
