package com.example.asd.service;

import com.example.asd.dto.FileMetadataDto;
import com.example.asd.model.FileMetadata;
import com.example.asd.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileMetadataRepository repository;
    private final StorageService storageService;

    public Mono<FileMetadataDto> processFile(FilePart filePart) {
        log.info("Rozpoczynam przetwarzanie pliku: {}", filePart.filename());

        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        digest.update(bytes);
                        String hash = Base64.getEncoder().encodeToString(digest.digest());

                        FileMetadata metadata = new FileMetadata(
                                null,
                                filePart.filename(),
                                (long) bytes.length,
                                hash
                        );

                        InputStream inputStream = new ByteArrayInputStream(bytes);
                        storageService.store(filePart.filename(), inputStream);

                        log.info("Plik {} został zapisany w StorageService i metadane przygotowane.", filePart.filename());

                        return repository.save(metadata)
                                .map(saved -> {
                                    log.info("Metadane pliku {} zapisane w bazie danych.", saved.getFileName());
                                    return new FileMetadataDto(saved.getFileName(), saved.getSize(), saved.getDigest());
                                });
                    } catch (Exception e) {
                        log.error("Błąd podczas przetwarzania pliku {}: {}", filePart.filename(), e.getMessage(), e);
                        return Mono.error(new RuntimeException("Błąd przetwarzania pliku", e));
                    }
                });
    }
}
