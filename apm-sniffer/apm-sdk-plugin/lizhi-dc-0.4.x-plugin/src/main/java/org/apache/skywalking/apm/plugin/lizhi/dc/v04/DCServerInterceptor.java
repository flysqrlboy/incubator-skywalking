package org.apache.skywalking.apm.plugin.lizhi.dc.v04;

import java.lang.reflect.Method;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import fm.lizhi.commons.service.client.context.RpcContext;
import fm.lizhi.commons.service.client.filter.InvokeContext;

public class DCServerInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        RpcContext rpcContext = RpcContext.getContext();
        InvokeContext invokeContext = (InvokeContext) allArguments[0];
        ContextCarrier contextCarrier = new ContextCarrier();

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(rpcContext.getAttachment(next.getHeadKey()));
        }

        AbstractSpan span = ContextManager.createEntrySpan("/DC/server/process-domain:"
                + invokeContext.getDomain() + ";op:" + invokeContext.getOp(), contextCarrier);

        span.setComponent("DC");
        SpanLayer.asRPCFramework(span);
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
