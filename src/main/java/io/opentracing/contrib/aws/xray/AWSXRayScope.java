package io.opentracing.contrib.aws.xray;

import io.opentracing.Scope;

/**
 * @see Scope
 * @author ashley.mercer@skylightipv.com
 */
class AWSXRayScope implements Scope {

    private final AWSXRayScopeManager scopeManager;
    private final AWSXRayScope previousScope;

    private final AWSXRaySpan span;
    private final boolean finishOnClose;

    AWSXRayScope(AWSXRayScopeManager scopeManager, AWSXRayScope previousScope, AWSXRaySpan span, boolean finishOnClose) {
        this.scopeManager = scopeManager;
        this.previousScope = previousScope;
        this.span = span;
        this.finishOnClose = finishOnClose;
    }

    @Override
    public void close() {
        if (finishOnClose) span.finish();
        scopeManager.setCurrentScope(previousScope);
    }

    @Override
    public AWSXRaySpan span() {
        return span;
    }
}
