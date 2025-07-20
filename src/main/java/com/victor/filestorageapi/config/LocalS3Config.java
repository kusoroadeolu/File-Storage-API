package com.victor.filestorageapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalS3Config
{
     private final S3ConfigProperties configProperties;

     @Value("${cloud.s3.endpoint}")
     private String endpoint;

     @Value("${cloud.aws.region.static}")
     private String region;

     @Bean
     public S3Client s3Client(){
       return S3Client
               .builder()
               .credentialsProvider(StaticCredentialsProvider.create(
                       AwsBasicCredentials.create(configProperties.getAccessKey(), configProperties.getSecretKey())
               ))
               .region(Region.of(region))
               .endpointOverride(URI.create(endpoint))
               .serviceConfiguration(S3Configuration
                       .builder()
                       .pathStyleAccessEnabled(true)
                       .build())
               .build();

     }
}
