package com.htdz.data;


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
@ComponentScan(basePackages={"com.htdz"})
@EnableDubboConfiguration
public class DataApplication {
	public static void main(String[] args) {
		LogManager.info("----------DataApplication Main----------");
		
		SpringApplication application = new SpringApplication(DataApplication.class);
		application.addListeners(new ApplicationListener<ContextRefreshedEvent>() {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent context) {
				DataEngine engine = context.getApplicationContext().getBean(DataEngine.class);
				engine.onContextInitCompleted();
			}
		});
		
		application.run(args);
	}
	
	@Bean 
    @ConfigurationProperties(prefix="spring.datasource")
	public DataSource druidDataSource() {
        return new DruidDataSource();
    }
}
