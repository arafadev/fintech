package com.fintech.core.auth.support;

import com.fintech.core.common.config.AppProperties;
import com.fintech.core.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePictureValidatorTest {

    private ProfilePictureValidator validator;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getUpload().setMaxFileSizeBytes(5L * 1024 * 1024);
        validator = new ProfilePictureValidator(props);
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "pic.jpg", "image/jpeg", new byte[0]);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(file));
        assertEquals("error.upload.file_required", ex.getMessageKey());
    }

    @Test
    void shouldRejectOversizedFile() {
        byte[] bytes = new byte[(int) (5L * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "pic.jpg", "image/jpeg", bytes);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(file));
        assertEquals("error.upload.file_too_large", ex.getMessageKey());
    }

    @Test
    void shouldRejectNonImageContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "x".getBytes());
        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(file));
        assertEquals("error.upload.invalid_image_type", ex.getMessageKey());
    }

    @Test
    void shouldAcceptValidJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "pic.jpg", "image/jpeg", "img".getBytes());
        assertDoesNotThrow(() -> validator.validate(file));
    }
}
