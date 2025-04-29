package com.example.asd.unit;

import com.example.asd.model.FileMetadata;
import com.example.asd.repository.FileMetadataRepository;
import com.example.asd.service.FileUploadService;
import com.example.asd.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FileUploadServiceTest {

    @Mock
    private FileMetadataRepository repository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileUploadService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProcessFileCorrectly() {
        // given
        var fileContent = "Test file content";
        var fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
        var buffer = new DefaultDataBufferFactory().wrap(fileBytes);
        var expectedFileName = "testfile.txt";
        var expectedSize = (long) fileBytes.length;

        FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        when(filePart.filename()).thenReturn(expectedFileName);

        when(storageService.store(eq(expectedFileName), any(Flux.class), any(MessageDigest.class)))
                .thenReturn(Mono.just(expectedSize));
        when(repository.save(any(FileMetadata.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when
        var result = underTest.processFile(filePart);

        // then
        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.fileName()).isEqualTo(expectedFileName);
                    assertThat(dto.size()).isEqualTo(expectedSize);
                    assertThat(dto.digest()).isNotNull();
                    assertThat(dto.digest()).isNotEmpty();
                })
                .verifyComplete();

        verify(storageService).store(eq(expectedFileName), any(Flux.class), any(MessageDigest.class));
        verify(repository).save(any(FileMetadata.class));
    }

    @Test
    void shouldReturnErrorWhenStorageFails() {
        // given
        var fileContent = "Broken file content";
        var fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
        var buffer = new DefaultDataBufferFactory().wrap(fileBytes);
        var expectedFileName = "brokenfile.txt";

        FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        when(filePart.filename()).thenReturn(expectedFileName);

        when(storageService.store(eq(expectedFileName), any(Flux.class), any(MessageDigest.class)))
                .thenReturn(Mono.error(new RuntimeException("Błąd przetwarzania pliku")));

        // when
        var result = underTest.processFile(filePart);

        // then
        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(RuntimeException.class);
                    assertThat(throwable.getMessage()).contains("Błąd przetwarzania pliku");
                })
                .verify();

        verify(storageService).store(eq(expectedFileName), any(Flux.class), any(MessageDigest.class));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnErrorWhenHashGenerationFails() {
        // given
        var fileContent = "Invalid content for hashing";
        var fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
        var buffer = new DefaultDataBufferFactory().wrap(fileBytes);
        var expectedFileName = "brokenfile.txt";

        FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        when(filePart.filename()).thenReturn(expectedFileName);

        try (var mockedDigest = mockStatic(MessageDigest.class)) {
            mockedDigest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new RuntimeException("Hashing algorithm error"));

            // when
            var result = underTest.processFile(filePart);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable).isInstanceOf(RuntimeException.class);
                        assertThat(throwable.getMessage()).contains("Błąd przetwarzania pliku");
                    })
                    .verify();

            verifyNoInteractions(repository);
            verifyNoInteractions(storageService);
        }
    }

}
