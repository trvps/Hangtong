package com.htdz.device;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import com.htdz.common.LogManager;
import com.htdz.common.utils.SpringContextUtil;

@SpringBootApplication
@ComponentScan(basePackages = { "com.htdz" })
@EnableDubboConfiguration
public class DeviceApplication {
	public static void main(String[] args) {
		LogManager.info("----------DeviceApplication Main----------");

		SpringApplication application = new SpringApplication(
				DeviceApplication.class);
		application
				.addListeners(new ApplicationListener<ContextRefreshedEvent>() {
					@Override
					public void onApplicationEvent(ContextRefreshedEvent context) {
						SpringContextUtil.setApplicationContext(context
								.getApplicationContext());

						DeviceEngine engine = context.getApplicationContext()
								.getBean(DeviceEngine.class);
						engine.onContextInitCompleted();
					}
				});

		ApplicationContext applicationContext = application.run(args);
		SpringContextUtil.setApplicationContext(applicationContext);
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource druidDataSource() {
		return new DruidDataSource();
	}
}
