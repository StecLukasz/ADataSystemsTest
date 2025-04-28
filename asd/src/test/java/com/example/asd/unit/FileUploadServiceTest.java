package com.example.asd.unit;

import com.example.asd.model.FileMetadata;
import com.example.asd.repository.FileMetadataRepository;
import com.example.asd.service.FileUploadService;
import com.example.asd.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

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
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(fileContent.getBytes(StandardCharsets.UTF_8));
        FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        when(filePart.filename()).thenReturn("testfile.txt");

        var metadata = new FileMetadata(UUID.randomUUID(), "testfile.txt", (long) fileContent.length(), "dummyHash");
        when(repository.save(any(FileMetadata.class))).thenReturn(Mono.just(metadata));

        // when
        var result = underTest.processFile(filePart);

        // then
        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.fileName().equals("testfile.txt") && dto.size() == fileContent.length())
                .verifyComplete();

        verify(storageService).store(eq("testfile.txt"), any(ByteArrayInputStream.class));
        verify(repository).save(any(FileMetadata.class));
    }

    @Test
    void shouldReturnErrorWhenStorageFails() {
        // given
        var fileContent = "Broken file content";
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(fileContent.getBytes(StandardCharsets.UTF_8));
        FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        when(filePart.filename()).thenReturn("brokenfile.txt");

        doThrow(new RuntimeException("Storage error")).when(storageService)
                .store(eq("brokenfile.txt"), any(ByteArrayInputStream.class));

        // when
        var result = underTest.processFile(filePart);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().contains("Błąd przetwarzania pliku"))
                .verify();

        verify(storageService).store(eq("brokenfile.txt"), any(ByteArrayInputStream.class));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnErrorWhenHashGenerationFails() {
        // given
        var fileContent = "Invalid content for hashing";
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(fileContent.getBytes(StandardCharsets.UTF_8));
        FilePart filePart = mock(FilePart.class);

        when(filePart.content()).thenReturn(Flux.just(buffer));
        when(filePart.filename()).thenReturn("brokenfile.txt");

        try (var mocked = mockStatic(MessageDigest.class)) {
            mocked.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new RuntimeException("Hashing algorithm error"));

            // when
            var result = underTest.processFile(filePart);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                            && throwable.getMessage().contains("Błąd przetwarzania pliku"))
                    .verify();

            verifyNoInteractions(repository);
            verifyNoInteractions(storageService);
        }
    }

}
