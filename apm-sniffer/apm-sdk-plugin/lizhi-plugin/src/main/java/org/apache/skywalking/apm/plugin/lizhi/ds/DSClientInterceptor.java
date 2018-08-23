package org.apache.skywalking.apm.plugin.lizhi.ds;

import java.lang.reflect.Method;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;

import fm.lizhi.datastore.CacheBeanObjectInfo;
import fm.lizhi.datastore.DataStoreStatement;
import fm.lizhi.datastore.DataStoreTransaction;
import fm.lizhi.datastore.RedisCmdInfo;
import fm.lizhi.datastore.StoreBeanObjectInfo;
import fm.lizhi.datastore.client.DataStoreReqMsg;

public class DSClientInterceptor implements StaticMethodsAroundInterceptor {

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments,
            Class<?>[] parameterTypes, MethodInterceptResult result) {

        DataStoreReqMsg reqMsg = (DataStoreReqMsg) allArguments[0];
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span =
                ContextManager.createExitSpan("DS/client/", contextCarrier, remotePeer(reqMsg));

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            reqMsg.setAttachment(next.getHeadKey(), next.getHeadValue());
        }
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

    private String remotePeer(DataStoreReqMsg reqMsg) {
        if (reqMsg.type != DataStoreReqMsg.MsgTypeTransaction) {
            return "other";
        }
        DataStoreTransaction transaction = (DataStoreTransaction) reqMsg.data;

        for (DataStoreReqMsg req : transaction.getReqList()) {
            if (req.type == DataStoreReqMsg.MsgTypeStoreBean) {
                StoreBeanObjectInfo beanInfo = (StoreBeanObjectInfo) req.data;
                return "mysql-" + beanInfo.getClassinfo().dbname + "."
                        + beanInfo.getClassinfo().tableName;
            } else if (req.type == DataStoreReqMsg.MsgTypeRedis) {
                RedisCmdInfo rds = (RedisCmdInfo) req.data;
                return "redis-" + rds.server;
            } else if (req.type == DataStoreReqMsg.MsgTypeStatement) {
                DataStoreStatement stmt = (DataStoreStatement) req.data;
                return "mysql-statement-" + stmt.db + "-" + stmt.statement;
            } else if (req.type == DataStoreReqMsg.MsgTypeCacheBean) {
                CacheBeanObjectInfo cachebeanInfo = (CacheBeanObjectInfo) req.data;
                return "memcached-" + cachebeanInfo.getClassInfo().getServer();
            }
        }
        return "other";
    }

}
