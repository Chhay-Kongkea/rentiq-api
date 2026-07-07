package co.istad.rentiq_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@EnableConfigurationProperties
@EnableJpaAuditing
@SpringBootApplication
public class RentiqApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentiqApiApplication.class, args);
	}

}
