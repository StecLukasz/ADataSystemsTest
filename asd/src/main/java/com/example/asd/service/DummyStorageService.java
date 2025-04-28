package com.example.asd.service;

import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DummyStorageService implements StorageService {

    @Override
    public void store(String fileName, InputStream content) {
        // Tymczasowo tylko logowanie - symulacja zapisu
        System.out.println("Storing file: " + fileName);
    }
}
