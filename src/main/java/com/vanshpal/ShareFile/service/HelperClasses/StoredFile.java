package com.vanshpal.ShareFile.service.HelperClasses;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Data
@Getter
@Setter
@AllArgsConstructor
public class StoredFile {
        String originalFileName;
        String storedFileName;
        String deviceName;
        @Id Long ID;
        Long deviceID;
        int numberOfChunks;
        String fileRelativePath;
        Long fileSize;
        String fileType;
}
