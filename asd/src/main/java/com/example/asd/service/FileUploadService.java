package com.example.asd.service;

import com.example.asd.dto.FileMetadataDto;
import com.example.asd.model.FileMetadata;
import com.example.asd.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

        Flux<DataBuffer> content = filePart.content();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return storageService.store(filePart.filename(), content, digest)
                    .flatMap(size -> {
                        String hash = Base64.getEncoder().encodeToString(digest.digest());
                        FileMetadata metadata = new FileMetadata(null, filePart.filename(), size, hash);
                        return repository.save(metadata);
                    })
                    .map(saved -> {
                        log.info("Zapisano metadane pliku: {}", saved.getFileName());
                        return new FileMetadataDto(saved.getFileName(), saved.getSize(), saved.getDigest());
                    });

        } catch (Exception e) {
            return Mono.error(new RuntimeException("Błąd przetwarzania pliku", e));
        }
    }
}
