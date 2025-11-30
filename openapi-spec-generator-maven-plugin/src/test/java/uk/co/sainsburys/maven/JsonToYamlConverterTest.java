package uk.co.sainsburys.maven;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class JsonToYamlConverterTest {

    @Test
    void testJsonToYamlConversion() throws IOException {
        JsonToYamlConverter converter = new JsonToYamlConverter();
        
        String json = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Test API\",\"version\":\"1.0.0\"}}";
        String yaml = converter.convertJsonToYaml(json);
        
        assertNotNull(yaml);
        assertTrue(yaml.contains("openapi: \"3.0.1\"") || yaml.contains("openapi: 3.0.1"));
        assertTrue(yaml.contains("title: Test API") || yaml.contains("title: \"Test API\""));
        assertTrue(yaml.contains("version: \"1.0.0\"") || yaml.contains("version: 1.0.0"));
    }

    @Test
    void testNullJsonConversion() throws IOException {
        JsonToYamlConverter converter = new JsonToYamlConverter();
        
        // Jackson converts null to the string "null"
        String yaml = converter.convertJsonToYaml("null");
        assertNotNull(yaml);
        assertTrue(yaml.contains("null") || yaml.trim().equals("null"));
    }

    @Test
    void testInvalidJsonConversion() {
        JsonToYamlConverter converter = new JsonToYamlConverter();
        
        assertThrows(IOException.class, () -> {
            converter.convertJsonToYaml("not valid json");
        });
    }

    @Test
    void testYamlToJsonConversion() throws IOException {
        JsonToYamlConverter converter = new JsonToYamlConverter();
        
        String yaml = "openapi: 3.0.1\ninfo:\n  title: Test API\n  version: 1.0.0";
        String json = converter.convertYamlToJson(yaml);
        
        assertNotNull(json);
        assertTrue(json.contains("\"openapi\""));
        assertTrue(json.contains("\"3.0.1\""));
        assertTrue(json.contains("\"title\""));
        assertTrue(json.contains("\"Test API\""));
    }
}

