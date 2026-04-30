package com.yas.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.config.YasConfig;
import com.yas.media.mapper.MediaVmMapper;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.model.dto.MediaDto.MediaDtoBuilder;
import com.yas.media.repository.FileSystemRepository;
import com.yas.media.repository.MediaRepository;
import com.yas.media.service.MediaServiceImpl;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class MediaServiceUnitTest {

    @Spy
    private MediaVmMapper mediaVmMapper = Mappers.getMapper(MediaVmMapper.class);

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Mock
    private YasConfig yasConfig;

    @Mock
    private MediaDtoBuilder builder;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Media media;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        media = new Media();
        media.setId(1L);
        media.setCaption("test");
        media.setFileName("file");
        media.setMediaType("image/jpeg");
    }

    // ===================== getMediaById =====================
    @Nested
    class GetMediaByIdTest {

        @Test
        void getMedia_whenValidId_thenReturnData() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
            when(yasConfig.publicUrl()).thenReturn("/media/");

            MediaVm mediaVm = mediaService.getMediaById(1L);

            assertNotNull(mediaVm);
            assertEquals("Test", mediaVm.getCaption());
            assertEquals("fileName", mediaVm.getFileName());
            assertEquals("image/png", mediaVm.getMediaType());
            assertEquals(String.format("/media/medias/%s/file/%s", 1L, "fileName"), mediaVm.getUrl());
        }

        @Test
        void getMedia_whenMediaNotFound_thenReturnNull() {
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(null);

            MediaVm mediaVm = mediaService.getMediaById(1L);

            assertNull(mediaVm);
        }

        @Test
        void getMedia_whenPublicUrlHasTrailingSlash_thenUrlIsCorrect() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(5L, "cap", "my-img.png", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(5L)).thenReturn(noFileMediaVm);
            when(yasConfig.publicUrl()).thenReturn("https://cdn.example.com");

            MediaVm mediaVm = mediaService.getMediaById(5L);

            assertNotNull(mediaVm);
            assertThat(mediaVm.getUrl()).contains("/medias/5/file/my-img.png");
        }
    }

    // ===================== removeMedia =====================
    @Nested
    class RemoveMediaTest {

        @Test
        void removeMedia_whenMediaNotFound_thenThrowsNotFoundException() {
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(null);

            NotFoundException exception = assertThrows(NotFoundException.class,
                () -> mediaService.removeMedia(1L));
            assertEquals(String.format("Media %s is not found", 1L), exception.getMessage());
            verify(mediaRepository, never()).deleteById(any());
        }

        @Test
        void removeMedia_whenValidId_thenRemoveSuccess() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
            doNothing().when(mediaRepository).deleteById(1L);

            mediaService.removeMedia(1L);

            verify(mediaRepository, times(1)).deleteById(1L);
        }

        @Test
        void removeMedia_whenDifferentId_thenThrowsNotFoundException() {
            when(mediaRepository.findByIdWithoutFileInReturn(999L)).thenReturn(null);

            assertThrows(NotFoundException.class, () -> mediaService.removeMedia(999L));
        }
    }

    // ===================== saveMedia =====================
    @Nested
    class SaveMediaTest {

        @Test
        void saveMedia_whenTypePNG_thenSaveSuccess() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "example.png", "image/png", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("media", mediaSave.getCaption());
            assertEquals("fileName", mediaSave.getFileName());
            assertEquals("image/png", mediaSave.getMediaType());
        }

        @Test
        void saveMedia_whenTypeJPEG_thenSaveSuccess() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "example.jpeg", "image/jpeg", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("media", mediaSave.getCaption());
            assertEquals("fileName", mediaSave.getFileName());
            assertEquals("image/jpeg", mediaSave.getMediaType());
        }

        @Test
        void saveMedia_whenTypeGIF_thenSaveSuccess() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "example.gif", "image/gif", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("image/gif", mediaSave.getMediaType());
        }

        @Test
        void saveMedia_whenFileNameOverrideIsNull_thenUseOriginalFileName() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "example.png", "image/png", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, null);
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertEquals("example.png", mediaSave.getFileName());
        }

        @Test
        void saveMedia_whenFileNameOverrideIsEmpty_thenUseOriginalFileName() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "example.png", "image/png", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "");
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertEquals("example.png", mediaSave.getFileName());
        }

        @Test
        void saveMedia_whenFileNameOverrideIsBlank_thenUseOriginalFileName() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "example.png", "image/png", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "   ");
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertEquals("example.png", mediaSave.getFileName());
        }

        @Test
        void saveMedia_whenFileNameOverrideHasLeadingTrailingSpaces_thenTrimmed() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "example.png", "image/png", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "  custom-name  ");
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertEquals("custom-name", mediaSave.getFileName());
        }

        @Test
        void saveMedia_whenCaptionIsNull_thenSaveWithNullCaption() {
            MultipartFile multipartFile = new MockMultipartFile(
                "file", "photo.png", "image/png", new byte[]{}
            );
            MediaPostVm mediaPostVm = new MediaPostVm(null, multipartFile, "photo.png");
            when(mediaRepository.save(any(Media.class))).thenAnswer(i -> i.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNull(mediaSave.getCaption());
            assertEquals("photo.png", mediaSave.getFileName());
        }
    }

    // ===================== getFile =====================
    @Nested
    class GetFileTest {

        @Test
        void getFile_whenMediaNotFound_thenReturnEmptyDto() {
            MediaDto expectedDto = MediaDto.builder().build();
            when(mediaRepository.findById(1L)).thenReturn(Optional.empty());
            when(builder.build()).thenReturn(expectedDto);

            MediaDto mediaDto = mediaService.getFile(1L, "fileName");

            assertNull(mediaDto.getMediaType());
            assertNull(mediaDto.getContent());
        }

        @Test
        void getFile_whenFileNameNotMatch_thenReturnEmptyDto() {
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
            // media.fileName = "file", but we request "wrong-name"
            MediaDto mediaDto = mediaService.getFile(1L, "wrong-name");

            assertNull(mediaDto.getMediaType());
            assertNull(mediaDto.getContent());
        }

        @Test
        void getFile_whenMediaFoundAndFileNameMatches_thenReturnDtoWithContent() {
            media.setFilePath("/tmp/file");
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(fileSystemRepository.getFile("/tmp/file"))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

            MediaDto mediaDto = mediaService.getFile(1L, "file");

            assertNotNull(mediaDto.getContent());
            assertEquals(MediaType.IMAGE_JPEG, mediaDto.getMediaType());
        }

        @Test
        void getFile_whenFileNameMatchesCaseInsensitive_thenReturnDtoWithContent() {
            // media.fileName = "file", request with "FILE" (case-insensitive match)
            media.setFilePath("/tmp/file");
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(fileSystemRepository.getFile("/tmp/file"))
                .thenReturn(new ByteArrayInputStream("data".getBytes()));

            MediaDto mediaDto = mediaService.getFile(1L, "FILE");

            assertNotNull(mediaDto.getContent());
        }
    }

    // ===================== getMediaByIds =====================
    @Nested
    class GetMediaByIdsTest {

        @Test
        void getFileByIds_whenMediasExist_thenReturnListWithUrls() {
            var ip15 = getMedia(-1L, "Iphone 15");
            var macbook = getMedia(-2L, "Macbook");
            var existingMedias = List.of(ip15, macbook);

            when(mediaRepository.findAllById(List.of(ip15.getId(), macbook.getId())))
                .thenReturn(existingMedias);
            when(yasConfig.publicUrl()).thenReturn("https://media/");

            var medias = mediaService.getMediaByIds(List.of(ip15.getId(), macbook.getId()));

            assertFalse(medias.isEmpty());
            verify(mediaVmMapper, times(existingMedias.size())).toVm(any());
            assertThat(medias).allMatch(m -> m.getUrl() != null);
        }

        @Test
        void getFileByIds_whenNoMediasExist_thenReturnEmptyList() {
            when(mediaRepository.findAllById(List.of(99L, 100L))).thenReturn(List.of());

            var medias = mediaService.getMediaByIds(List.of(99L, 100L));

            assertThat(medias).isEmpty();
        }

        @Test
        void getFileByIds_whenSingleMediaExists_thenUrlContainsFileInfo() {
            var singleMedia = getMedia(10L, "single.png");
            singleMedia.setMediaType("image/png");
            when(mediaRepository.findAllById(List.of(10L))).thenReturn(List.of(singleMedia));
            when(yasConfig.publicUrl()).thenReturn("https://cdn.example.com");

            var medias = mediaService.getMediaByIds(List.of(10L));

            assertThat(medias).hasSize(1);
            assertThat(medias.getFirst().getUrl()).contains("/medias/10/file/single.png");
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private static @NotNull Media getMedia(Long id, String name) {
        var media = new Media();
        media.setId(id);
        media.setFileName(name);
        media.setMediaType("image/jpeg");
        return media;
    }
}

// force rebuild
