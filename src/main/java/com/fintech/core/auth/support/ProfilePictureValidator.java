package com.fintech.core.auth.support;

import com.fintech.core.common.config.AppProperties;
import com.fintech.core.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfilePictureValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif"
    );

    private final AppProperties appProperties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.upload.file_required");
        }

        long maxBytes = appProperties.getUpload().getMaxFileSizeBytes();
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("error.upload.file_too_large", formatMb(maxBytes));
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("error.upload.invalid_image_type");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("error.upload.invalid_image_type");
        }
    }

    private static String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private static String formatMb(long bytes) {
        return String.valueOf(bytes / (1024 * 1024));
    }
}
