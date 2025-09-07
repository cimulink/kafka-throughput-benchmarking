package com.example.kafkabenchmark.key;

import java.util.concurrent.ThreadLocalRandom;

public class LowCardinalityKeyGenerator implements KeyGenerator {
    private static final String[] KEYS = {
        "event-type-1", "event-type-2", 
        "event-type-3", "event-type-4",
        "event-type-5", "event-type-6"
    };
    
    @Override
    public String generateKey() {
        // Using ThreadLocalRandom for better performance in multi-threaded environments
        return KEYS[ThreadLocalRandom.current().nextInt(KEYS.length)];
    }
}