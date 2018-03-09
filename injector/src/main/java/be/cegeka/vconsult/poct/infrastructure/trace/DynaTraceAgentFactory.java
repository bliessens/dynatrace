package be.cegeka.vconsult.poct.infrastructure.trace;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

@Component
class DynaTraceAgentFactory implements FactoryBean<OneAgentSDK> {

    @Override
    public OneAgentSDK getObject() throws Exception {
        return OneAgentSDKFactory.createInstance();
    }

    @Override
    public Class<?> getObjectType() {
        return OneAgentSDK.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
