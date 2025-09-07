package com.example.kafkabenchmark.key;

import org.junit.Test;
import static org.junit.Assert.*;

public class HighCardinalityKeyGeneratorTest {
    
    @Test
    public void testGenerateKey() {
        HighCardinalityKeyGenerator generator = new HighCardinalityKeyGenerator();
        
        // Generate several keys and verify they're unique
        String key1 = generator.generateKey();
        String key2 = generator.generateKey();
        
        assertNotNull("Key should not be null", key1);
        assertNotNull("Key should not be null", key2);
        assertTrue("Key should not be empty", !key1.isEmpty());
        assertTrue("Key should not be empty", !key2.isEmpty());
        assertNotEquals("Keys should be unique", key1, key2);
    }
}