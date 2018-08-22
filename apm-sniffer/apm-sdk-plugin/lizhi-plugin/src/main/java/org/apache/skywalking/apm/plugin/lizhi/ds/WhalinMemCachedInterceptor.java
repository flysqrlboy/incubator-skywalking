package org.apache.skywalking.apm.plugin.lizhi.ds;

import java.lang.reflect.Method;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

public class WhalinMemCachedInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String WHALINMEMCACHED = "WhalinMemCached/";
    
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        
        String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        AbstractSpan span = ContextManager.createExitSpan(WHALINMEMCACHED + method.getName(), peer);
        span.setComponent("WhalinMemCached");
        Tags.DB_TYPE.set(span, "WhalinMemCached");
        SpanLayer.asCache(span);
        Tags.DB_STATEMENT.set(span, method.getName() + " " + allArguments[0]);

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method,
            Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        
        ContextManager.activeSpan().errorOccurred().log(t);
    }

}
