package com.example.asd.controller;

import com.example.asd.dto.FileMetadataDto;
import com.example.asd.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<FileMetadataDto> uploadFiles(@RequestPart("files") Flux<FilePart> files) {
        return files.flatMap(service::processFile);
    }
}
