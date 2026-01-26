package com.vanshpal.ShareFile.service.storageService;

import com.vanshpal.ShareFile.service.entityClasses.StoredFile;
import org.springframework.data.repository.CrudRepository;

public interface FileShareRepository extends CrudRepository<StoredFile, Long> {

}

