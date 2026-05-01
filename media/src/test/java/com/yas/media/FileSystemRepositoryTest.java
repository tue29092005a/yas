package com.yas.media;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.yas.media.config.FilesystemConfig;
import com.yas.media.repository.FileSystemRepository;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
class FileSystemRepositoryTest {

    private static final String TEST_URL = "src/test/resources/test-directory";

    @Mock
    private FilesystemConfig filesystemConfig;

    @Mock
    private File file;

    @InjectMocks
    private FileSystemRepository fileSystemRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws IOException {
        Path testDir = Paths.get(TEST_URL);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                .sorted((p1, p2) -> p2.compareTo(p1))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
    }

    // ===================== persistFile =====================
    @Nested
    class PersistFileTest {

        @Test
        void testPersistFile_whenDirectoryNotExist_thenThrowsException() {
            when(filesystemConfig.getDirectory()).thenReturn("non-exist-directory");

            assertThrows(IllegalStateException.class,
                () -> fileSystemRepository.persistFile("test-file.png", "content".getBytes()));
        }

        @Test
        void testPersistFile_whenFilenameContainsDotDot_thenThrowsIllegalArgument() throws IOException {
            File directory = new File(TEST_URL);
            directory.mkdirs();
            when(filesystemConfig.getDirectory()).thenReturn(directory.getAbsolutePath());

            assertThrows(IllegalArgumentException.class,
                () -> fileSystemRepository.persistFile("../malicious.png", "content".getBytes()));
        }

        @Test
        void testPersistFile_whenFilenameContainsSlash_thenThrowsIllegalArgument() throws IOException {
            File directory = new File(TEST_URL);
            directory.mkdirs();
            when(filesystemConfig.getDirectory()).thenReturn(directory.getAbsolutePath());

            assertThrows(IllegalArgumentException.class,
                () -> fileSystemRepository.persistFile("sub/file.png", "content".getBytes()));
        }

        @Test
        void testPersistFile_whenFilenameContainsBackslash_thenThrowsIllegalArgument() throws IOException {
            File directory = new File(TEST_URL);
            directory.mkdirs();
            when(filesystemConfig.getDirectory()).thenReturn(directory.getAbsolutePath());

            assertThrows(IllegalArgumentException.class,
                () -> fileSystemRepository.persistFile("sub\\file.png", "content".getBytes()));
        }

        @Test
        void testPersistFile_whenValidFilename_thenSaveAndReturnPath() throws IOException {
            File directory = new File(TEST_URL);
            directory.mkdirs();
            String absPath = directory.getAbsolutePath();
            when(filesystemConfig.getDirectory()).thenReturn(absPath);

            String filePath = fileSystemRepository.persistFile("valid-file.png", "image-data".getBytes());

            assertNotNull(filePath);
            assertTrue(Files.exists(Paths.get(filePath)));

            byte[] written = Files.readAllBytes(Paths.get(filePath));
            assertArrayEquals("image-data".getBytes(), written);
        }

        @Test
        void testPersistFile_filePathNotContainsDirectory() {
            // Uses relative path so normalized absolute path won't start with the relative TEST_URL
            File directory = new File(TEST_URL);
            directory.mkdirs();
            when(filesystemConfig.getDirectory()).thenReturn(TEST_URL);

            assertThrows(IllegalArgumentException.class,
                () -> fileSystemRepository.persistFile("test-file.png", "content".getBytes()));
        }
    }

    // ===================== getFile =====================
    @Nested
    class GetFileTest {

        @Test
        void testGetFile_whenFileExists_thenReturnInputStream() throws IOException {
            String filename = "test-file.png";
            String filePathStr = Paths.get(TEST_URL, filename).toString();
            byte[] content = "test-content".getBytes();

            when(filesystemConfig.getDirectory()).thenReturn(TEST_URL);

            Path filePath = Paths.get(filePathStr);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content);

            InputStream inputStream = fileSystemRepository.getFile(filePathStr);
            byte[] fileContent = inputStream.readAllBytes();
            assertArrayEquals(content, fileContent);
        }

        @Test
        void testGetFile_whenFileDoesNotExist_thenThrowsException() {
            String directoryPath = "non-exist-directory";
            String filePathStr = Paths.get(directoryPath, "test-file.png").toString();

            when(filesystemConfig.getDirectory()).thenReturn(directoryPath);

            assertThrows(IllegalStateException.class,
                () -> fileSystemRepository.getFile(filePathStr));
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private static void assertNotNull(Object obj) {
        org.junit.jupiter.api.Assertions.assertNotNull(obj);
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
