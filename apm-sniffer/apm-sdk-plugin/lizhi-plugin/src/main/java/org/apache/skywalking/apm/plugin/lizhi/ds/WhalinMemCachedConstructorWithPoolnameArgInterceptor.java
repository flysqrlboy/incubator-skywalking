package org.apache.skywalking.apm.plugin.lizhi.ds;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

public class WhalinMemCachedConstructorWithPoolnameArgInterceptor
        implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        
        objInst.setSkyWalkingDynamicField(allArguments[0]);
    }

}
