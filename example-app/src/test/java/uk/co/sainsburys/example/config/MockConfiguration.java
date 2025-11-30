package uk.co.sainsburys.example.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Mock configuration for OpenAPI spec generation.
 * This configuration is loaded when the 'openapi-generation' profile is active.
 * 
 * Add any mocked beans here that your application needs to start but aren't
 * required for OpenAPI spec generation (e.g., external service clients, message queues, etc.)
 */
@TestConfiguration
@Profile("openapi-generation")
public class MockConfiguration {
    
    // Example: Mock an external service client
    // @Bean
    // public ExternalServiceClient externalServiceClient() {
    //     return Mockito.mock(ExternalServiceClient.class);
    // }
    
    // Example: Mock a Kafka template
    // @Bean
    // public KafkaTemplate<String, Object> kafkaTemplate() {
    //     return Mockito.mock(KafkaTemplate.class);
    // }
}

