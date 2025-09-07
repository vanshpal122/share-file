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
    @Id Long ID;
    Long fileSize;
    String fileType;
}
