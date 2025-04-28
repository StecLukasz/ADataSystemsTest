package com.example.asd.repository;

import com.example.asd.model.FileMetadata;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface FileMetadataRepository extends ReactiveCrudRepository<FileMetadata, UUID> {
}
