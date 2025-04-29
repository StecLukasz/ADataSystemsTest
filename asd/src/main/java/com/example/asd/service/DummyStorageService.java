package com.example.asd.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

@Service
public class DummyStorageService implements StorageService {

    private static final String STORAGE_DIR = "storage";

    @Override
    public Mono<Long> store(String fileName, Flux<DataBuffer> content, MessageDigest digest) {
        return Mono.fromCallable(() -> {
            Path destination = Paths.get(STORAGE_DIR).resolve(fileName);
            Files.createDirectories(destination.getParent());
            return destination;
        }).flatMap(path -> {
            try {
                OutputStream fos = new FileOutputStream(path.toFile());
                DigestOutputStream digestStream = new DigestOutputStream(fos, digest);

                return DataBufferUtils.write(content, digestStream)
                        .doOnNext(DataBufferUtils::release)
                        .reduce(0L, (acc, buffer) -> acc + buffer.readableByteCount())
                        .doFinally(signal -> {
                            try {
                                digestStream.close();
                            } catch (Exception e) {
                                throw new RuntimeException("Błąd zamykania pliku", e);
                            }
                        });

            } catch (Exception e) {
                return Mono.error(new RuntimeException("Błąd zapisu pliku", e));
            }
        });
    }
}
