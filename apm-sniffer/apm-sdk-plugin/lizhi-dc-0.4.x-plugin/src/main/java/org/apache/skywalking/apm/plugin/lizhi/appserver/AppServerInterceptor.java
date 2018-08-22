package org.apache.skywalking.apm.plugin.lizhi.appserver;

import java.lang.reflect.Method;

import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import fm.lizhi.server.conn.socket.codec.SocketContext;
import fm.lizhi.server.conn.socket.codec.SocketRequest;

public class AppServerInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        

        SocketContext socketContext = (SocketContext)allArguments[0];
        ContextCarrier contextCarrier = new ContextCarrier();

//        CarrierItem next = contextCarrier.items();
//        while (next.hasNext()) {
//            next = next.next();
//            next.setHeadValue(request.getHeader(next.getHeadKey()));
//        }
        SocketRequest socketRequest = socketContext.getRequest();


        AbstractSpan span = ContextManager.createEntrySpan(
                "Op:" + socketRequest.getOp() + ";Acceptor:" + socketRequest.getAcceptor()
                        + ";ClientId:" + socketRequest.getClientId() + ";",
                contextCarrier);
        span.setComponent("AppServer");
        SpanLayer.asHttp(span);

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
