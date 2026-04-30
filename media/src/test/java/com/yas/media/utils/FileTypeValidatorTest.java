package com.yas.media.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileTypeValidatorTest {

    private FileTypeValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new FileTypeValidator();
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintViolationBuilder.class);

        // stub the context chain used when validation fails
        when(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(violationBuilder);

        // initialize with allowed types
        ValidFileType annotation = mock(ValidFileType.class);
        when(annotation.allowedTypes()).thenReturn(new String[]{"image/jpeg", "image/png", "image/gif"});
        when(annotation.message()).thenReturn("Invalid file type");
        validator.initialize(annotation);
    }

    @Nested
    class ValidFileTypeTests {

        @Test
        void isValid_whenFileIsNull_shouldReturnFalse() {
            assertFalse(validator.isValid(null, context));
        }

        @Test
        void isValid_whenContentTypeIsNull_shouldReturnFalse() {
            MultipartFile file = new MockMultipartFile("file", "image.png", null, new byte[]{});
            assertFalse(validator.isValid(file, context));
        }

        @Test
        void isValid_whenContentTypeNotAllowed_shouldReturnFalse() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3}
            );
            assertFalse(validator.isValid(file, context));
        }

        @Test
        void isValid_whenAllowedTypeButUnreadableContent_thenReturnFalse() {
            // content-type matches but bytes are not a parseable image → returns false
            MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", new byte[]{1, 2, 3, 4}
            );
            assertFalse(validator.isValid(file, context));
        }

        @Test
        void isValid_whenJpegTypeButUnreadableContent_thenReturnFalse() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpeg", "image/jpeg", new byte[]{10, 20, 30}
            );
            assertFalse(validator.isValid(file, context));
        }

        @Test
        void isValid_whenAllowedTypeButInvalidImageContent_shouldReturnFalse() {
            // content type says png but content is not a real image
            MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", new byte[]{0, 1, 2, 3}
            );
            assertFalse(validator.isValid(file, context));
        }

        @Test
        void isValid_whenEmptyFileWithAllowedContentType_shouldReturnFalse() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[]{}
            );
            assertFalse(validator.isValid(file, context));
        }
    }

}

