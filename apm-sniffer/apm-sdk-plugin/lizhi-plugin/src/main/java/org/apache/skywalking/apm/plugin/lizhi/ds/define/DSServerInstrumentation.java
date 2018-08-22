package org.apache.skywalking.apm.plugin.lizhi.ds.define;

import static net.bytebuddy.matcher.ElementMatchers.named;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class DSServerInstrumentation extends ClassStaticMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "fm.lizhi.datastore.server.DataStoreCmdProcessor";
    private static final String INTERCEPTOR_CLASS =
            "org.apache.skywalking.apm.plugin.lizhi.ds.DSServerInterceptor";

    @Override
    protected StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[] {new StaticMethodsInterceptPoint() {

            @Override
            public ElementMatcher<MethodDescription> getMethodsMatcher() {
                return named("exec");
            }

            @Override
            public String getMethodsInterceptor() {
                return INTERCEPTOR_CLASS;
            }

            @Override
            public boolean isOverrideArgs() {
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
