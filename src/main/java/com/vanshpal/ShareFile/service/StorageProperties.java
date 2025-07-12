package com.vanshpal.ShareFile.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Setter
@ConfigurationProperties("storage")
public class StorageProperties {
    private Path tempLocation = Paths.get("D:", "shareFile", "temp");
    private Path finalLocation = Paths.get("D:", "shareFile", "final");
}
