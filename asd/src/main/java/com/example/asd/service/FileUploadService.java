package com.example.asd.service;

import com.example.asd.dto.FileMetadataDto;
import com.example.asd.model.FileMetadata;
import com.example.asd.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileMetadataRepository repository;
    private final StorageService storageService;

    public Mono<FileMetadataDto> processFile(FilePart filePart) {
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

                        return repository.save(metadata)
                                .map(saved -> new FileMetadataDto(saved.getFileName(), saved.getSize(), saved.getDigest()));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Błąd przetwarzania pliku", e));
                    }
                });
    }
}
