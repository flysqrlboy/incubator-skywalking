package org.apache.skywalking.apm.toolkit.activation.log.logback.v1.x;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;

public class AsyncAppenderInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        if (ContextManager.isActive()) {
            if (allArguments[0] instanceof LoggingEvent) {
                LoggingEvent loggingEvent = (LoggingEvent) allArguments[0];
                if (loggingEvent.getLoggerContextVO() != null) {
                    LoggerContextVO loggerContextVO = loggingEvent.getLoggerContextVO();
                    Map<String, String> propertyMap = loggerContextVO.getPropertyMap();
                    if (propertyMap != null) {
                        propertyMap.put("globalTraceId", ContextManager.getGlobalTraceId());
                    }

                } else {
                    Map<String, String> propertyMap = new HashMap<String, String>();
                    propertyMap.put("globalTraceId", ContextManager.getGlobalTraceId());
                    LoggerContextVO loggerContextVO = new LoggerContextVO("TraceContext",
                            propertyMap, System.currentTimeMillis());
                    loggingEvent.setLoggerContextRemoteView(loggerContextVO);
                }
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Object ret) throws Throwable {

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method,
            Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }

}
