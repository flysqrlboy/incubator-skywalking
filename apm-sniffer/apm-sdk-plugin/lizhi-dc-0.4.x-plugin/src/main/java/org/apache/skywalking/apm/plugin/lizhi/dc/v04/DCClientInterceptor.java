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
import fm.lizhi.commons.service.client.filter.client.ClientInvokeContext;

public class DCClientInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        RpcContext rpcContext = RpcContext.getContext();
        ClientInvokeContext clientContext = (ClientInvokeContext) allArguments[0];
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan("DC/client/invoke", contextCarrier,
                "domain:" + clientContext.getRequestBuilder().getDomain() + ",op:"
                        + clientContext.getRequestBuilder().getOp());

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            rpcContext.setAttachment(next.getHeadKey(), next.getHeadValue());
        }
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
