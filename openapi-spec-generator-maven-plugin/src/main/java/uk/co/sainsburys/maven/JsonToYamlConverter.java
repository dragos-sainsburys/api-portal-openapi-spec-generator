package uk.co.sainsburys.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Utility class for converting JSON OpenAPI specifications to YAML format.
 * Preserves the structure and formatting while ensuring proper YAML output.
 */
public class JsonToYamlConverter {

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public JsonToYamlConverter() {
        this.jsonMapper = new ObjectMapper();
        
        // Configure YAML mapper with proper formatting
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
                .build();
        
        this.yamlMapper = new ObjectMapper(yamlFactory);
        this.yamlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        this.yamlMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
    }

    /**
     * Converts a JSON file to YAML format.
     *
     * @param jsonFile   the input JSON file
     * @param yamlFile   the output YAML file
     * @throws IOException if file operations fail
     */
    public void convert(File jsonFile, File yamlFile) throws IOException {
        if (!jsonFile.exists()) {
            throw new IOException("JSON file does not exist: " + jsonFile.getAbsolutePath());
        }

        // Read JSON file
        String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
        
        // Convert to YAML
        String yamlContent = convertJsonToYaml(jsonContent);
        
        // Write YAML file
        Files.writeString(yamlFile.toPath(), yamlContent, StandardCharsets.UTF_8);
    }

    /**
     * Converts JSON string to YAML string.
     *
     * @param json the JSON string
     * @return the YAML string
     * @throws IOException if conversion fails
     */
    public String convertJsonToYaml(String json) throws IOException {
        JsonNode jsonNode = jsonMapper.readTree(json);
        return yamlMapper.writeValueAsString(jsonNode);
    }

    /**
     * Converts YAML string to JSON string (for testing purposes).
     *
     * @param yaml the YAML string
     * @return the JSON string
     * @throws IOException if conversion fails
     */
    public String convertYamlToJson(String yaml) throws IOException {
        JsonNode yamlNode = yamlMapper.readTree(yaml);
        return jsonMapper.writeValueAsString(yamlNode);
    }
}

