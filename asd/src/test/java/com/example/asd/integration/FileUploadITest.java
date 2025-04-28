package com.example.asd.integration;

import com.example.asd.AsdtestApplication;
import com.example.asd.repository.FileMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AsdtestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadITest {

    @LocalServerPort
    private int port;

    @Autowired
    private FileMetadataRepository repository;

    @Test
    void shouldUploadFileSuccessfully() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        var resource = new ClassPathResource("testfile.txt");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", resource).filename("testfile.txt");

        webTestClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].fileName").isEqualTo("testfile.txt");

        var saved = repository.findAll().collectList().block();
        assertThat(saved).isNotEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenNoFileProvided() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        var emptyMultipartBuilder = new MultipartBodyBuilder();

        webTestClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(emptyMultipartBuilder.build()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnBadRequestWhenWrongContentType() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        webTestClient.post()
                .uri("/files")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("this-is-not-a-file")
                .exchange()
                .expectStatus().isEqualTo(415);
    }
    @Test
    void shouldUploadLargeFileSuccessfully() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMinutes(5))
                .build();

        var resource = new ClassPathResource("largefile.txt");

        webTestClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(new org.springframework.util.LinkedMultiValueMap<>() {{
                    add("files", resource);
                }}))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].fileName").isEqualTo("largefile.txt");

        var saved = repository.findAll().collectList().block();
        assertThat(saved).isNotEmpty();
    }

    @Test
    void shouldUploadMultipleFilesSuccessfully() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        var resource1 = new ClassPathResource("testfile1.txt");
        var resource2 = new ClassPathResource("testfile2.txt");

        var multipartData = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        multipartData.add("files", resource1);
        multipartData.add("files", resource2);

        webTestClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);

        var saved = repository.findAll().collectList().block();
        assertThat(saved).hasSizeGreaterThanOrEqualTo(2);
    }

}
