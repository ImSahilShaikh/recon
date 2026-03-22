package dev.scout.recon;

import dev.scout.recon.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ReconApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReconApplication.class, args);
	}

}
