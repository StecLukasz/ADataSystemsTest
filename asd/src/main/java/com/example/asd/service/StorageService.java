package com.example.asd.service;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;

public interface StorageService {
    Mono<Long> store(String fileName, Flux<DataBuffer> content, MessageDigest digest);
}
