package com.example.kafkabenchmark.key;

import com.example.kafkabenchmark.config.BenchmarkConfig;

public class KeyGeneratorFactory {
    
    public static KeyGenerator createKeyGenerator(BenchmarkConfig.KeyCardinality keyCardinality) {
        switch (keyCardinality) {
            case LOW:
                return new LowCardinalityKeyGenerator();
            case HIGH:
                return new HighCardinalityKeyGenerator();
            default:
                throw new IllegalArgumentException("Unknown key cardinality: " + keyCardinality);
        }
    }
}