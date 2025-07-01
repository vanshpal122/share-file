package com.vanshpal.ShareFile.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("storage")
public class StorageProperties {
    private String tempLocation = "D:/shareFile/temp";
    private String finalLocation = "D:/shareFile/final";
}
