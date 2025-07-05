package com.victor.filestorageapi;

import com.victor.filestorageapi.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class FilestorageapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FilestorageapiApplication.class, args);
	}

}
