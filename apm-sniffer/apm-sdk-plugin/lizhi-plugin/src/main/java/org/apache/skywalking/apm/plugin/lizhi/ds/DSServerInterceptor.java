package org.apache.skywalking.apm.plugin.lizhi.ds;

import java.lang.reflect.Method;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;

import fm.lizhi.datastore.client.DataStoreReqMsg;
import fm.lizhi.datastore.logsync.SyncLogMsg;

public class DSServerInterceptor implements StaticMethodsAroundInterceptor {

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments,
            Class<?>[] parameterTypes, MethodInterceptResult result) {

        SyncLogMsg slm = (SyncLogMsg) allArguments[0];
        DataStoreReqMsg dsmsg = DataStoreReqMsg.fromJsonString(slm.cmd);

        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(dsmsg.getAttachment(next.getHeadKey()));
        }

        AbstractSpan span = ContextManager.createEntrySpan("/DS/server", contextCarrier);

        span.setComponent("DS");
        SpanLayer.asDB(span);
    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments,
            Class<?>[] parameterTypes, Object ret) {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments,
            Class<?>[] parameterTypes, Throwable t) {

        ContextManager.activeSpan().errorOccurred().log(t);
    }

}
