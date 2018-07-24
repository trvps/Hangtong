package com.htdz.resource;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import com.htdz.common.LogManager;


/*
@SuppressWarnings("deprecation")
@Configuration
public class ResourceApplicationConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private Environment env;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// 在此处配置的虚拟路径，用springboot内置的tomcat时有效
		LogManager.info("-----ResourceApplicationConfig----------");
		registry.addResourceHandler(env.getProperty("v.file.location"))
				.addResourceLocations(env.getProperty("spring.http.multipart.location"));
	}
}
*/

@Configuration
public class ResourceApplicationConfig extends WebMvcConfigurationSupport {
	@Autowired
	private Environment env;
	
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
        	.addResourceHandler(env.getProperty("v.file.location"))
        	.addResourceLocations(env.getProperty("spring.http.multipart.location"));
        super.addResourceHandlers(registry);
    }
}
