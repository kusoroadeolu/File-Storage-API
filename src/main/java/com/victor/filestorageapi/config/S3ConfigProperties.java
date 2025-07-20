package com.victor.filestorageapi.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties("cloud.aws.credentials")
@Data
@Component
public class S3ConfigProperties {
    private String secretKey;

    private String accessKey;
}
