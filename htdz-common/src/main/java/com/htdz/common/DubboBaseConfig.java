package com.htdz.common;


//import org.springframework.context.annotation.Bean;
//import com.alibaba.dubbo.config.ApplicationConfig;
//import com.alibaba.dubbo.config.MonitorConfig;
//import com.alibaba.dubbo.config.ProtocolConfig;
//import com.alibaba.dubbo.config.ProviderConfig;
//import com.alibaba.dubbo.config.ReferenceConfig;
//import com.alibaba.dubbo.config.RegistryConfig;


public class DubboBaseConfig {
	/*
	 
	@Bean
    public RegistryConfig registry() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("127.0.0.1:2181");
        registryConfig.setProtocol("zookeeper");
        return registryConfig;
    }

    @Bean
    public ApplicationConfig application() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("htdz-gateway");
        return applicationConfig;
    }

    @Bean
    public MonitorConfig monitorConfig() {
        MonitorConfig mc = new MonitorConfig();
        mc.setProtocol("registry");
        return mc;
    }

    @Bean
    public ReferenceConfig referenceConfig() {
        ReferenceConfig rc = new ReferenceConfig();
        rc.setMonitor(monitorConfig());
        return rc;
    }

    @Bean
    public ProtocolConfig protocol() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(20880);
        return protocolConfig;
    }

    @Bean
    public ProviderConfig provider() {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setMonitor(monitorConfig());
        return providerConfig;
    }
    
    // 发布服务
    public ServiceBean<E> serviceProvider(E e) {
        ServiceBean<E> serviceBean = new ServiceBean<E>();
        serviceBean.setProxy("javassist");
        serviceBean.setVersion("1.0.0");
        serviceBean.setInterface(E.class.getName());
        serviceBean.setRef(person);
        serviceBean.setTimeout(5000);
        serviceBean.setRetries(3);
        return serviceBean;
    }
    
    // 消费服务
    public ReferenceBean<E> serviceConsumer() {
        ReferenceBean<E> refBean = new ReferenceBean<>();
        refBean.setApplication(applicationConfig);
        refBean.setVersion("1.0.0");
        refBean.setInterface(E.class);
        refBean.setTimeout(5000);
        refBean.setRetries(3);
        refBean.setCheck(false);
        refBean.setUrl(url);
        return refBean;
    }
    
	*/
}
