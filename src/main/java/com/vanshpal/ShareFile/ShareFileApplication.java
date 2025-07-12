package com.vanshpal.ShareFile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShareFileApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShareFileApplication.class, args);
	}

}
