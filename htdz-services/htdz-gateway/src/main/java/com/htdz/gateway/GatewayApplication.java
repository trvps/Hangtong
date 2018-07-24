package com.htdz.gateway;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import com.htdz.common.LogManager;
import com.htdz.common.utils.SpringContextUtil;


@ComponentScan(basePackages = { "com.htdz" })
@EnableDubboConfiguration
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GatewayApplication {
	public static void main(String[] args) {
		LogManager.info("----------GatewayApplication Main----------");
	
		SpringApplication application = new SpringApplication(GatewayApplication.class);
		application.addListeners(new ApplicationListener<ContextRefreshedEvent>() {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent context) {
				SpringContextUtil.setApplicationContext(context.getApplicationContext());
				GatewayEngine engine = context.getApplicationContext().getBean(GatewayEngine.class);
				engine.onContextInitCompleted();
			}
		});
		
		ApplicationContext applicationContext = application.run(args);
		SpringContextUtil.setApplicationContext(applicationContext);
	}
}
