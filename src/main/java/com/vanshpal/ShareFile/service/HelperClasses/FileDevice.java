package com.vanshpal.ShareFile.service.HelperClasses;

import java.util.List;

public record FileDevice(
        String deviceName, Long ID, List<String> fileList
) {
}
