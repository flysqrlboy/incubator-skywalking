package org.apache.skywalking.apm.plugin.lizhi.ds.define;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class WhalinMemcachedInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "com.whalin.MemCached.MemCachedClient";
    private static final String CONSTRUCTOR_WITH_POOLNAME_ARG_INTERCEPT_CLASS =
            "org.apache.skywalking.apm.plugin.lizhi.ds.WhalinMemCachedConstructorWithPoolnameArgInterceptor";
    private static final String METHOD_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.lizhi.ds.WhalinMemCachedInterceptor";
    
    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        
        return new ConstructorInterceptPoint[] {
            new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArguments(String.class);
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return CONSTRUCTOR_WITH_POOLNAME_ARG_INTERCEPT_CLASS;
                    }
                }
            
            };
    }
    @Override
    protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("keyExists").or(named("get")).or(named("set")).or(named("add"))
                                .or(named("replace")).or(named("gets")).or(named("append"))
                                .or(named("prepend")).or(named("cas")).or(named("delete"))
                                .or(named("touch")).or(named("incr")).or(named("decr"))
                                .or(named("storeCounter")).or(named("getCounter"))
                                .or(named("addOrIncr")).or(named("getMulti"))
                                .or(named("getMultiArray")).or(named("stats")).or(named("sync"))
                                .or(named("syncAll")).or(named("flushAll"));
                }

                @Override
                public String getMethodsInterceptor() {
                    return METHOD_INTERCEPT_CLASS;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }

    

    

}
