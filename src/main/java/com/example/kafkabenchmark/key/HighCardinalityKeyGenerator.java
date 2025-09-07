package com.example.kafkabenchmark.key;

import java.util.UUID;

public class HighCardinalityKeyGenerator implements KeyGenerator {
    
    @Override
    public String generateKey() {
        // Directly return the UUID string for maximum performance
        return UUID.randomUUID().toString();
    }
}