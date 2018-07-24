package com.htdz.liteguardian;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import com.htdz.common.LogManager;

@SpringBootApplication
@ComponentScan(basePackages = { "com.htdz" })
@EnableDubboConfiguration
public class LiteguardianApplication {
	public static void main(String[] args) {
		LogManager.info("----------LiteguardianApplication Main----------");

		SpringApplication application = new SpringApplication(LiteguardianApplication.class);
		application.addListeners(new ApplicationListener<ContextRefreshedEvent>() {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent context) {
				LiteguardianEngine engine = context.getApplicationContext().getBean(LiteguardianEngine.class);
				engine.onContextInitCompleted();
			}
		});

		application.run(args);
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource druidDataSource() {
		return new DruidDataSource();
	}
}
