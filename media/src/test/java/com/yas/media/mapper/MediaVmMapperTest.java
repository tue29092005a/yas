package com.yas.media.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yas.media.model.Media;
import com.yas.media.viewmodel.MediaVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MediaVmMapperTest {

    private MediaVmMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(MediaVmMapper.class);
    }

    // ===================== toVm =====================
    @Nested
    class ToVmTest {

        @Test
        void toVm_whenMediaHasAllFields_shouldMapToMediaVm() {
            Media media = new Media();
            media.setId(1L);
            media.setCaption("Test Caption");
            media.setFileName("image.png");
            media.setMediaType("image/png");
            media.setFilePath("/uploads/image.png");

            MediaVm vm = mapper.toVm(media);

            assertNotNull(vm);
            assertEquals(1L, vm.getId());
            assertEquals("Test Caption", vm.getCaption());
            assertEquals("image.png", vm.getFileName());
            assertEquals("image/png", vm.getMediaType());
        }

        @Test
        void toVm_whenMediaHasNullFields_shouldMapNulls() {
            Media media = new Media();
            media.setId(2L);

            MediaVm vm = mapper.toVm(media);

            assertNotNull(vm);
            assertEquals(2L, vm.getId());
            assertNull(vm.getCaption());
            assertNull(vm.getFileName());
            assertNull(vm.getMediaType());
        }

        @Test
        void toVm_urlFieldShouldBeNullInitially() {
            Media media = new Media();
            media.setId(3L);
            media.setFileName("photo.jpg");
            media.setMediaType("image/jpeg");

            MediaVm vm = mapper.toVm(media);

            // url is not mapped from Media, caller sets it manually
            assertNull(vm.getUrl());
        }
    }

    // ===================== toModel =====================
    @Nested
    class ToModelTest {

        @Test
        void toModel_whenMediaVmHasAllFields_shouldMapToMedia() {
            MediaVm vm = new MediaVm(10L, "caption", "file.png", "image/png",
                "http://example.com/medias/10/file/file.png");

            Media media = mapper.toModel(vm);

            assertNotNull(media);
            assertEquals(10L, media.getId());
            assertEquals("caption", media.getCaption());
            assertEquals("file.png", media.getFileName());
            assertEquals("image/png", media.getMediaType());
        }

        @Test
        void toModel_whenMediaVmHasNullFields_shouldMapNulls() {
            MediaVm vm = new MediaVm(null, null, null, null, null);

            Media media = mapper.toModel(vm);

            assertNotNull(media);
            assertNull(media.getId());
            assertNull(media.getCaption());
        }

        @Test
        void toModel_urlFieldShouldNotBeMappedToMedia() {
            MediaVm vm = new MediaVm(5L, "cap", "img.png", "image/png",
                "http://cdn.example.com/medias/5/file/img.png");

            Media media = mapper.toModel(vm);

            // filePath is not mapped from MediaVm.url
            assertNull(media.getFilePath());
        }
    }

    // ===================== partialUpdate =====================
    @Nested
    class PartialUpdateTest {

        @Test
        void partialUpdate_whenVmHasAllFields_shouldUpdateMedia() {
            Media existingMedia = new Media();
            existingMedia.setId(1L);
            existingMedia.setCaption("old caption");
            existingMedia.setFileName("old.png");
            existingMedia.setMediaType("image/png");

            MediaVm vm = new MediaVm(1L, "new caption", "new.png", "image/jpeg",
                "http://example.com/medias/1/file/new.png");

            mapper.partialUpdate(existingMedia, vm);

            assertEquals("new caption", existingMedia.getCaption());
            assertEquals("new.png", existingMedia.getFileName());
            assertEquals("image/jpeg", existingMedia.getMediaType());
        }

        @Test
        void partialUpdate_whenVmFieldsAreNull_shouldNotOverwriteExistingValues() {
            Media existingMedia = new Media();
            existingMedia.setId(7L);
            existingMedia.setCaption("keep this");
            existingMedia.setFileName("keep.png");
            existingMedia.setMediaType("image/png");

            // All nullable fields in vm are null
            MediaVm vm = new MediaVm(7L, null, null, null, null);

            mapper.partialUpdate(existingMedia, vm);

            // NullValuePropertyMappingStrategy.IGNORE → existing values kept
            assertEquals("keep this", existingMedia.getCaption());
            assertEquals("keep.png", existingMedia.getFileName());
            assertEquals("image/png", existingMedia.getMediaType());
        }

        @Test
        void partialUpdate_shouldUpdateOnlyProvidedFields() {
            Media existingMedia = new Media();
            existingMedia.setId(9L);
            existingMedia.setCaption("original");
            existingMedia.setFileName("original.png");
            existingMedia.setMediaType("image/png");

            MediaVm vm = new MediaVm(9L, "updated caption", null, null, null);

            mapper.partialUpdate(existingMedia, vm);

            assertEquals("updated caption", existingMedia.getCaption());
            // fileName and mediaType unchanged because vm fields are null
            assertEquals("original.png", existingMedia.getFileName());
            assertEquals("image/png", existingMedia.getMediaType());
        }
    }
}
