package com.fintech.fintech.auth.storage;

import com.fintech.fintech.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class ProfilePictureStorage {

    private final AppProperties appProperties;

    public String store(MultipartFile file, String oldRelativeUrl) throws IOException {
        Path uploadPath = Paths.get(appProperties.getUpload().getProfileDir());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        if (oldRelativeUrl != null && !oldRelativeUrl.isEmpty()) {
            Path oldFile = Paths.get(oldRelativeUrl);
            if (Files.exists(oldFile)) {
                Files.delete(oldFile);
            }
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String newFileName = UUID.randomUUID() + fileExtension;
        Files.copy(file.getInputStream(), uploadPath.resolve(newFileName));
        return "profile-picture/" + newFileName;
    }
}
