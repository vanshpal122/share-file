package com.vanshpal.ShareFile;

import com.vanshpal.ShareFile.service.Exceptions.StorageException;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import com.vanshpal.ShareFile.service.LocalFileStorageService;
import com.vanshpal.ShareFile.service.StorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class LocalStorageServiceTests {

    private LocalFileStorageService service;
    private StorageProperties storageProperties;


    @BeforeEach
    public void initialize() {
        storageProperties = new StorageProperties();
        Path tempPath = Paths.get("src", "test", "resources", "tempTest").toAbsolutePath();
        Path finalPath = Paths.get("src", "test", "resources", "finalTest").toAbsolutePath();
        storageProperties.setTempLocation(tempPath);
        storageProperties.setFinalLocation(finalPath);
        service = new LocalFileStorageService(storageProperties);
    }


    @AfterEach
    void tearDown() {
        LocalFileStorageService.deleteDirectory(storageProperties.getTempLocation());
        LocalFileStorageService.deleteDirectory(storageProperties.getFinalLocation());
    }


    @Test
    public void isChunkSuccessfullySavedTest() {
        Random random = new Random();
        FileChunk chunkFileObject = new
                FileChunk(0, random.nextLong(), random.nextLong(), new MockMultipartFile("foo", "foo.txt", MediaType.TEXT_PLAIN_VALUE,
                "foo".getBytes())
        );
        Path storedRelativeChunkPath = service.storeChunk(chunkFileObject);
        Path expectedDeviceFileDirectoryPath = storageProperties.getTempLocation().resolve(chunkFileObject.deviceID() + "").resolve(chunkFileObject.fileID() + "");
        Path expectedChunkFilePath = expectedDeviceFileDirectoryPath.resolve(Paths.get("0"));
        assertThat(expectedDeviceFileDirectoryPath).exists();
        assertEquals(expectedChunkFilePath, storageProperties.getTempLocation().resolve(storedRelativeChunkPath));
        assertThat(expectedChunkFilePath).exists();
    }

    //Merge file Chunk Test

    @Test
    public void checkDeviceNameValidTest() {
        Random random = new Random();
        String invalidDeviceName = "../HackerDevice";
        StoredFile fileToBeSaved = new StoredFile("J", null, invalidDeviceName, random.nextLong(), random.nextLong(), 1, null, null, null);
        assertThrows(StorageException.class, () -> service.mergeFileChunks(fileToBeSaved));
    }

    @Test
    public void checkFileNameValidTest() {
        Random random = new Random();
        String invalidFileName = "/../Destroy.txt";
        StoredFile fileToBeSaved = new StoredFile(invalidFileName, null, "GoodDevice", random.nextLong(), random.nextLong(), 1, null, null, null);
        assertThrows(StorageException.class, () -> service.mergeFileChunks(fileToBeSaved));
    }

    @Test
    public void invalidNumberOfChunksTest() {
        Random random = new Random();
        Long fileID = random.nextLong();
        Long deviceID = random.nextLong();
        FileChunk chunk1 = new FileChunk(0, fileID, deviceID, new MockMultipartFile("Chunk1", "Chunk1.txt", MediaType.TEXT_PLAIN_VALUE,
                "Chunk1".getBytes()));
        service.storeChunk(chunk1);
        FileChunk chunk2 = new FileChunk(1, fileID, deviceID, new MockMultipartFile("Chunk2", "Chunk2.txt", MediaType.TEXT_PLAIN_VALUE,
                "Chunk2".getBytes()));
        service.storeChunk(chunk2);


        int totalNumberOfChunksStored = 4; //actual stored chunk is 2
        StoredFile fileToBeSaved = new StoredFile("Good.txt", null, "GoodDevice", fileID, deviceID, totalNumberOfChunksStored, null, null, null);
        assertThrows(StorageException.class, () -> service.mergeFileChunks(fileToBeSaved));
    }


    @Test
    public void fileSuccessfullySavedTest() {
        Random random = new Random();
        Long fileID = random.nextLong();
        Long deviceID = random.nextLong();
        String deviceName = "Device";
        String fileName = "file.txt";

        FileChunk chunk = new FileChunk(0, fileID, deviceID, new MockMultipartFile("foo", "foo.txt", MediaType.TEXT_PLAIN_VALUE,
                "foo".getBytes()));
        service.storeChunk(chunk);
        int totalNumberOfChunksStored = 1;
        StoredFile fileToBeSaved = new StoredFile(fileName, null, deviceName, fileID, deviceID, totalNumberOfChunksStored, null, null, null);
        Path expectedFilePath = storageProperties.getFinalLocation().resolve(deviceName).resolve(fileName);
        Path actualStoredRelativeFilePath = Paths.get(service.mergeFileChunks(fileToBeSaved).getFileRelativePath());

        assertEquals(expectedFilePath, storageProperties.getFinalLocation().resolve(actualStoredRelativeFilePath));
        assertThat(storageProperties.getFinalLocation().resolve(fileToBeSaved.getDeviceName())).exists();
        assertThat(storageProperties.getFinalLocation().resolve(fileToBeSaved.getDeviceName()).resolve(fileToBeSaved.getOriginalFileName())).exists();
        assertThat(storageProperties.getTempLocation().resolve(Paths.get(deviceID + ""))).doesNotExist();
        assertThat(storageProperties.getFinalLocation().resolve(actualStoredRelativeFilePath)).exists();
    }

    @Test
    public void duplicateFileNameStoreFromCommonDeviceTest() {
        Random random = new Random();
        Long deviceID = random.nextLong();
        String commonFileName = "CommonName.txt";
        String commonDeviceName = "DeviceName";

        //File 1 Chunk
        Long file1ID = random.nextLong();
        FileChunk chunkOfFile1 = new FileChunk(0, file1ID, deviceID, new MockMultipartFile("Chunk1", "Chunk1.txt", MediaType.TEXT_PLAIN_VALUE,
                "foo1".getBytes()));
        service.storeChunk(chunkOfFile1);

        //File 1 Save
        int totalNumberOfChunksStoredFile1 = 1;
        StoredFile file1ToBeSaved = new StoredFile(commonFileName, null, commonDeviceName, file1ID, deviceID, totalNumberOfChunksStoredFile1, null, null, null);
        service.mergeFileChunks(file1ToBeSaved);

        //File2 Chunk
        Long file2ID = random.nextLong();
        FileChunk chunkOfFile2 = new FileChunk(0, file2ID, deviceID, new MockMultipartFile("Chunk2", "Chunk2.txt", MediaType.TEXT_PLAIN_VALUE,
                "foo2".getBytes()));
        service.storeChunk(chunkOfFile2);
        //File2 Save
        int totalNumberOfChunksStoredFile2 = 1;
        StoredFile file2ToBeSaved = new StoredFile(commonFileName, null, commonDeviceName, file2ID, deviceID, totalNumberOfChunksStoredFile2, null, null, null);
        service.mergeFileChunks(file2ToBeSaved);

        //File2Name
        String file2ExpectedName = LocalFileStorageService.appendToFileNameRespectingExtension(commonFileName, file2ID + "");

        assertThat(storageProperties.getFinalLocation().resolve(file1ToBeSaved.getDeviceName()).resolve(file1ToBeSaved.getOriginalFileName())).exists();
        assertThat(storageProperties.getFinalLocation().resolve(file2ToBeSaved.getDeviceName()).resolve(file2ExpectedName)).exists();
    }

}