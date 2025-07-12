package com.vanshpal.ShareFile;

import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import org.springframework.data.repository.CrudRepository;

public interface FileShareRepository extends CrudRepository<StoredFile, Long> {

}

