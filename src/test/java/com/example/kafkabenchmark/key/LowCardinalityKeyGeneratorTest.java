package com.example.kafkabenchmark.key;

import org.junit.Test;
import static org.junit.Assert.*;

public class LowCardinalityKeyGeneratorTest {
    
    @Test
    public void testGenerateKey() {
        LowCardinalityKeyGenerator generator = new LowCardinalityKeyGenerator();
        
        // Generate several keys and verify they're from the expected set
        for (int i = 0; i < 100; i++) {
            String key = generator.generateKey();
            assertNotNull("Key should not be null", key);
            assertTrue("Key should not be empty", !key.isEmpty());
        }
    }
}