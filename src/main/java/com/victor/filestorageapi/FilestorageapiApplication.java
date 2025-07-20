package com.victor.filestorageapi;

import com.victor.filestorageapi.models.FolderNode;
import com.victor.filestorageapi.service.utils.PathValidator;
import com.victor.filestorageapi.service.utils.UserFolderUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class FilestorageapiApplication {

	public static void main(String[] args) {
	SpringApplication.run(FilestorageapiApplication.class, args);

	}
}
