# OpenAPI Spec Generator - Technical Architecture

## Overview

This document provides a comprehensive technical deep-dive into the OpenAPI Spec Generator Maven Plugin architecture, design decisions, and implementation details. It is intended for developers who will maintain, extend, or troubleshoot the plugin.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Core Components](#core-components)
- [Design Decisions](#design-decisions)
- [Implementation Details](#implementation-details)
- [Extension Points](#extension-points)
- [Testing Strategy](#testing-strategy)
- [Performance Considerations](#performance-considerations)
- [Security Considerations](#security-considerations)

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Maven Build Process                     │
│                                                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   compile    │ -> │ process-     │ -> │   package    │  │
│  │              │    │ classes      │    │              │  │
│  └──────────────┘    └──────┬───────┘    └──────────────┘  │
│                              │                               │
│                              v                               │
│                   ┌──────────────────────┐                  │
│                   │  OpenApiGenerator    │                  │
│                   │  Mojo.execute()      │                  │
│                   └──────────┬───────────┘                  │
└──────────────────────────────┼──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                v                             v
    ┌───────────────────────┐    ┌───────────────────────┐
    │ Spring Boot Process   │    │  JsonToYamlConverter  │
    │                       │    │                       │
    │ - Start with profile  │    │ - Jackson JSON        │
    │ - Wait for ready      │    │ - Jackson YAML        │
    │ - Serve /v3/api-docs  │    │ - Format conversion   │
    │ - Shutdown gracefully │    │                       │
    └───────────────────────┘    └───────────────────────┘
                │
                v
    ┌───────────────────────┐
    │   target/openapi.*    │
    │   - openapi.json      │
    │   - openapi.yaml      │
    └───────────────────────┘
```

### Execution Flow

```
Maven Build
    │
    ├─> compile phase (Java classes compiled)
    │
    ├─> process-classes phase
    │   │
    │   └─> OpenApiGeneratorMojo.execute()
    │       │
    │       ├─> 1. validateConfiguration()
    │       │   └─> Check outputFormat, paths, etc.
    │       │
    │       ├─> 2. findMainClass()
    │       │   └─> Auto-detect or use configured main class
    │       │
    │       ├─> 3. startApplication()
    │       │   ├─> Build classpath from Maven artifacts
    │       │   ├─> Create ProcessBuilder with system properties
    │       │   ├─> Activate Spring profile
    │       │   └─> Start output consumer thread
    │       │
    │       ├─> 4. waitForApplicationReady()
    │       │   ├─> Poll /v3/api-docs endpoint
    │       │   ├─> Retry with exponential backoff
    │       │   └─> Timeout after configured duration
    │       │
    │       ├─> 5. fetchOpenApiSpec()
    │       │   ├─> HTTP GET to /v3/api-docs
    │       │   └─> Return JSON string
    │       │
    │       ├─> 6. Format Conversion (if needed)
    │       │   ├─> Write JSON file
    │       │   └─> Convert to YAML using JsonToYamlConverter
    │       │
    │       └─> 7. stopApplication()
    │           ├─> Graceful shutdown (Process.destroy())
    │           └─> Force kill if timeout (Process.destroyForcibly())
    │
    └─> Continue build (package, install, etc.)
```

## Core Components

### 1. OpenApiGeneratorMojo

**Location**: `openapi-spec-generator-maven-plugin/src/main/java/uk/co/sainsburys/maven/OpenApiGeneratorMojo.java`

**Purpose**: Main Maven plugin entry point that orchestrates the entire spec generation process.

**Key Responsibilities**:
- Configuration validation
- Application lifecycle management
- Process orchestration
- Error handling and recovery
- File I/O operations

**Maven Annotations**:

```java
@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
```

- `name = "generate"`: Goal name (invoked as `openapi-generator:generate`)
- `defaultPhase = PROCESS_CLASSES`: Runs after compilation but before packaging
- `requiresDependencyResolution = COMPILE_PLUS_RUNTIME`: Ensures all dependencies are resolved

**Configuration Parameters**:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `outputDir` | File | `${project.build.directory}` | Output directory for generated specs |
| `apiDocsPath` | String | `/v3/api-docs` | SpringDoc endpoint path (configurable for custom paths) |
| `outputFormat` | String | `yaml` | Output format: json, yaml, or both |
| `springProfile` | String | `openapi-generation` | Spring profile to activate |
| `failOnError` | boolean | `true` | Whether to fail build on errors |
| `serverPort` | int | `8080` | Port for Spring Boot app |
| `startupTimeout` | int | `60` | Max startup wait time (seconds) |
| `mainClass` | String | (auto-detected) | Spring Boot main class |

**Note on `apiDocsPath`**: This parameter is fully configurable to support:
- Different SpringDoc versions
- Custom SpringDoc configurations
- Applications with servlet context paths
- Legacy Swagger endpoints
- Custom documentation endpoints

**Example Custom Path Configurations**:

```xml
<!-- Custom SpringDoc configuration -->
<configuration>
    <apiDocsPath>/api/documentation</apiDocsPath>
</configuration>

<!-- Application with context path -->
<configuration>
    <apiDocsPath>/myapp/v3/api-docs</apiDocsPath>
</configuration>

<!-- Legacy Swagger 2.x -->
<configuration>
    <apiDocsPath>/v2/api-docs</apiDocsPath>
</configuration>
```

**State Management**:

The Mojo is stateless between invocations. Each execution:
1. Reads configuration from POM and project
2. Creates new processes and resources
3. Cleans up all resources before completion
4. Does not persist state between builds

### 2. JsonToYamlConverter

**Location**: `openapi-spec-generator-maven-plugin/src/main/java/uk/co/sainsburys/maven/JsonToYamlConverter.java`

**Purpose**: Utility class for converting OpenAPI specs from JSON to YAML format.

**Key Responsibilities**:
- JSON parsing
- YAML generation
- Format preservation
- Charset handling

**Implementation Details**:

```java
public class JsonToYamlConverter {
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    
    public JsonToYamlConverter() {
        this.jsonMapper = new ObjectMapper();
        
        YAMLFactory yamlFactory = YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)  // No "---"
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)          // Clean YAML
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR) // Readable arrays
            .build();
        
        this.yamlMapper = new ObjectMapper(yamlFactory);
        this.yamlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        this.yamlMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
    }
}
```

**Why These Settings?**

- `WRITE_DOC_START_MARKER` disabled: Removes `---` at the beginning (cleaner for single-doc files)
- `MINIMIZE_QUOTES` enabled: Only quotes strings when necessary (more readable)
- `INDENT_ARRAYS_WITH_INDICATOR` enabled: Uses `-` for array items (YAML best practice)
- `ORDER_MAP_ENTRIES_BY_KEYS` disabled: Preserves original order from JSON

### 3. Process Management

**Process Lifecycle**:

```
startApplication()
    │
    ├─> Create classpath string
    │   ├─> Add project output directory
    │   ├─> Add test output directory (for MockConfiguration)
    │   └─> Add all Maven dependencies
    │
    ├─> Build command array
    │   ├─> java
    │   ├─> -cp <classpath>
    │   ├─> -Dspring.profiles.active=openapi-generation
    │   ├─> -Dserver.port=8080
    │   ├─> -Dspringdoc.api-docs.enabled=true
    │   ├─> -Dspringdoc.api-docs.path=/v3/api-docs
    │   └─> <mainClass>
    │
    ├─> Create ProcessBuilder
    │   └─> redirectErrorStream(true) // Merge stdout/stderr
    │
    ├─> Start process
    │
    └─> Start output consumer thread (daemon)
        └─> Continuously read and log application output
```

**Why Separate Process?**

1. **Isolation**: Plugin and application have separate classpaths
2. **Lifecycle**: Full control over startup/shutdown
3. **Profiles**: Can activate specific Spring profiles
4. **Dependencies**: Application dependencies don't conflict with plugin dependencies
5. **Resources**: Application resources load correctly from project structure

**Alternative Approaches Considered**:

| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| Embedded execution | Faster, no process overhead | Classpath conflicts, complex lifecycle | ❌ Rejected |
| Docker container | Full isolation, reproducible | Requires Docker, slower | 🔮 Future enhancement |
| Testcontainers | Best isolation | Complex setup, dependencies | 🔮 Future enhancement |
| Separate JVM process | Good isolation, simple | Process overhead | ✅ **Selected** |

## Design Decisions

### 1. Why SpringDoc Over Swagger Core?

**Decision**: Use SpringDoc OpenAPI as the spec generation library.

**Rationale**:
- **Native Spring Boot 3.x support**: Built for modern Spring
- **Auto-configuration**: Works out-of-the-box with minimal config
- **Active maintenance**: Regular updates and bug fixes
- **Better integration**: Seamless with Spring WebMVC/WebFlux
- **Cleaner API**: More intuitive annotations and configuration

**Trade-offs**:
- Requires SpringDoc dependency in target application
- Less control over spec generation details
- Limited to SpringDoc's capabilities

### 2. Why Process-Based Execution?

**Decision**: Start application as separate JVM process rather than embedded execution.

**Rationale**:
- **Classpath isolation**: Prevents dependency conflicts between plugin and application
- **Lifecycle control**: Easy to start, wait, fetch, and stop
- **Profile activation**: Natural way to activate Spring profiles
- **Resource loading**: Application resources load from correct locations
- **Simplicity**: Easier to understand and debug

**Trade-offs**:
- Slower than embedded execution (JVM startup overhead)
- Requires compiled classes
- Higher memory usage (two JVMs)

### 3. Why Jackson for Conversion?

**Decision**: Use Jackson ObjectMapper for JSON/YAML conversion.

**Rationale**:
- **Standard library**: Already used by Spring Boot
- **Reliable**: Battle-tested, well-maintained
- **Feature-rich**: Extensive configuration options
- **Performance**: Fast parsing and generation
- **Format support**: JSON, YAML, XML, etc.

**Alternative Considered**:
- SnakeYAML: Lower-level, more control but more complex
- Manual conversion: Not feasible for complex OpenAPI schemas

### 4. Why Apache HttpClient 5?

**Decision**: Use Apache HttpClient 5 for HTTP operations.

**Rationale**:
- **Modern API**: Improved API over version 4
- **Standards compliant**: Full HTTP/1.1 and HTTP/2 support
- **Connection management**: Built-in pooling and retry logic
- **Actively maintained**: Regular updates

**Alternative Considered**:
- `java.net.HttpURLConnection`: Too low-level, manual connection management
- Spring RestTemplate: Would require Spring dependency in plugin
- OkHttp: Additional dependency, less standard in Maven world

### 5. Why H2 Database in Test Profile?

**Decision**: Use H2 in-memory database for test profile.

**Rationale**:
- **Zero configuration**: Works out-of-the-box
- **Fast startup**: In-memory, no disk I/O
- **Lightweight**: Small dependency
- **Compatible**: Works with most JPA entities
- **No external dependencies**: No database server required

**Trade-offs**:
- May not catch database-specific issues
- Limited SQL dialect support
- Not suitable for complex database features

## Implementation Details

### Configuration System

**Configuration Precedence** (highest to lowest):

1. Command-line parameters: `-Dserver.port=8081`
2. Plugin configuration in POM: `<configuration><serverPort>8081</serverPort>`
3. Default values: `@Parameter(defaultValue = "8080")`

**Example**:

```xml
<plugin>
    <groupId>uk.co.sainsburys</groupId>
    <artifactId>openapi-spec-generator-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- These override defaults -->
        <outputFormat>both</outputFormat>
        <serverPort>8081</serverPort>
        <startupTimeout>90</startupTimeout>
        <springProfile>custom-profile</springProfile>
        
        <!-- Custom API docs path (useful for different SpringDoc configs) -->
        <apiDocsPath>/custom/api-docs</apiDocsPath>
        
        <!-- Optional: specify main class -->
        <mainClass>com.example.MyApplication</mainClass>
        
        <!-- Optional: control error handling -->
        <failOnError>false</failOnError>
    </configuration>
</plugin>
```

**Common Use Cases for Custom `apiDocsPath`**:

1. **Different SpringDoc Configuration**:
   ```yaml
   # application.yml
   springdoc:
     api-docs:
       path: /api/documentation
   ```
   ```xml
   <!-- pom.xml -->
   <apiDocsPath>/api/documentation</apiDocsPath>
   ```

2. **Application with Context Path**:
   ```yaml
   # application.yml
   server:
     servlet:
       context-path: /myapp
   ```
   ```xml
   <!-- pom.xml -->
   <apiDocsPath>/myapp/v3/api-docs</apiDocsPath>
   ```

3. **Multiple API Doc Versions**:
   Different executions for different versions:
   ```xml
   <executions>
       <execution>
           <id>generate-v3</id>
           <configuration>
               <apiDocsPath>/v3/api-docs</apiDocsPath>
           </configuration>
       </execution>
       <execution>
           <id>generate-custom</id>
           <configuration>
               <apiDocsPath>/internal/api-docs</apiDocsPath>
           </configuration>
       </execution>
   </executions>
   ```

**Spring Profile Activation**:

The plugin activates the configured Spring profile via system property:

```java
command.add("-Dspring.profiles.active=" + springProfile);
```

This is equivalent to running:

```bash
java -Dspring.profiles.active=openapi-generation -jar app.jar
```

### Main Class Detection

**Strategy** (in order):

1. **Explicit configuration**: Use `<mainClass>` from POM
2. **start-class property**: Check for `start-class` property in POM
3. **Convention-based guess**: `${groupId}.${artifactId}.Application`

**Code**:

```java
private String findMainClass() throws MojoExecutionException {
    // Try project property
    Object mainClassProp = project.getProperties().get("start-class");
    if (mainClassProp != null) {
        return mainClassProp.toString();
    }
    
    // Fallback to convention
    String basePackage = project.getGroupId() + "." + 
                        project.getArtifactId().replace("-", "");
    String possibleMainClass = basePackage + ".Application";
    
    getLog().warn("Main class not specified. Attempting: " + possibleMainClass);
    return possibleMainClass;
}
```

**Best Practice**: Always specify main class explicitly in production use.

### Classpath Construction

**Components** (in order):

1. **Project output**: `target/classes/`
2. **Test output**: `target/test-classes/` (for MockConfiguration)
3. **All Maven dependencies**: Runtime and compile scope

**Why Include Test Classes?**

Test classes contain `MockConfiguration` that provides mocked beans for spec generation. This is the recommended pattern for mocking external dependencies.

**Code**:

```java
private String buildClasspath() {
    StringBuilder classpath = new StringBuilder();
    
    // Add main output directory
    classpath.append(project.getBuild().getOutputDirectory());
    classpath.append(File.pathSeparator);
    
    // Add test output directory (for mocks)
    classpath.append(project.getBuild().getTestOutputDirectory());
    
    // Add all dependencies
    project.getArtifacts().forEach(artifact -> {
        if (artifact.getFile() != null) {
            classpath.append(File.pathSeparator);
            classpath.append(artifact.getFile().getAbsolutePath());
        }
    });
    
    return classpath.toString();
}
```

### Application Readiness Detection

**Strategy**: Poll `/v3/api-docs` endpoint until it returns HTTP 200.

**Algorithm**:

```
START
    │
    ├─> Calculate timeout deadline (now + startupTimeout)
    │
    └─> LOOP until deadline
        │
        ├─> Try HTTP GET to /v3/api-docs
        │   │
        │   ├─> SUCCESS (200) → Return
        │   └─> FAILURE → Continue
        │
        ├─> Sleep 1 second
        │
        └─> Check deadline
            │
            ├─> Time remaining → Continue loop
            └─> Timeout → Throw MojoExecutionException
```

**Why Poll the Endpoint?**

- **Accurate**: Ensures SpringDoc is fully initialized
- **Simple**: No need to parse application logs
- **Reliable**: Works regardless of logging configuration
- **Standard**: Uses HTTP, the same protocol for fetching

**Alternative Approaches**:

- **Log parsing**: Fragile, depends on log format
- **Spring Boot Actuator**: Extra dependency, may not be present
- **Fixed delay**: Unreliable, wastes time or fails prematurely

### Error Handling

**Error Categories**:

1. **Configuration Errors**
   - Invalid output format
   - Invalid paths
   - Missing dependencies

2. **Startup Errors**
   - Application fails to start
   - Main class not found
   - Port already in use

3. **Runtime Errors**
   - Timeout waiting for ready
   - HTTP errors fetching spec
   - Conversion errors

4. **I/O Errors**
   - Cannot create output directory
   - Cannot write files
   - Disk full

**Error Handling Strategy**:

```java
try {
    // Main execution logic
    execute();
} catch (Exception e) {
    String message = "Failed to generate OpenAPI spec: " + e.getMessage();
    
    if (failOnError) {
        // Fail the build (default)
        throw new MojoExecutionException(message, e);
    } else {
        // Log and continue (optional)
        getLog().error(message, e);
    }
}
```

**Cleanup on Error**:

The `finally` block ensures application shutdown even on error:

```java
try {
    waitForApplicationReady();
    fetchOpenApiSpec();
    // ... conversion ...
} finally {
    // Always stop the application
    stopApplication(appProcess);
}
```

### Graceful Shutdown

**Shutdown Process**:

```
stopApplication(process)
    │
    ├─> Check if process is alive
    │
    ├─> Call Process.destroy() // Send SIGTERM
    │
    ├─> Wait up to 10 seconds
    │   │
    │   ├─> Process exits → SUCCESS
    │   └─> Timeout → Continue
    │
    └─> Force kill
        └─> Process.destroyForcibly() // Send SIGKILL
```

**Why Two-Stage Shutdown?**

1. **Graceful first**: Allows Spring Boot to shutdown cleanly
   - Close database connections
   - Flush buffers
   - Release resources

2. **Force if needed**: Prevents hanging builds
   - Ensures process terminates
   - Frees system resources

**Code**:

```java
private void stopApplication(Process process) {
    if (process != null && process.isAlive()) {
        getLog().info("Stopping application...");
        process.destroy();  // SIGTERM
        
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                getLog().warn("Application did not stop gracefully, forcing...");
                process.destroyForcibly();  // SIGKILL
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
```

## Extension Points

### 1. Custom Format Converters

**Current**: JSON to YAML conversion only.

**Extension**: Support additional formats (JSON Schema, Markdown, HTML).

**Implementation**:

```java
public interface SpecConverter {
    void convert(File input, File output) throws IOException;
    String getSupportedFormat();
}

// Plugin configuration
<converters>
    <converter>com.example.JsonToMarkdownConverter</converter>
</converters>
```

### 2. Custom Health Checks

**Current**: HTTP GET to `/v3/api-docs`.

**Extension**: Support Spring Boot Actuator health endpoint or custom endpoints.

**Implementation**:

```java
public interface HealthCheck {
    boolean isReady(String baseUrl) throws IOException;
}

// Configuration
<healthCheckClass>com.example.ActuatorHealthCheck</healthCheckClass>
```

### 3. Custom Main Class Detection

**Current**: Property-based and convention-based detection.

**Extension**: JAR manifest reading, bytecode scanning, custom strategies.

**Implementation**:

```java
public interface MainClassDetector {
    String findMainClass(MavenProject project) throws MojoExecutionException;
}

// Configuration
<mainClassDetector>com.example.ManifestBasedDetector</mainClassDetector>
```

### 4. Pre/Post Generation Hooks

**Current**: No hooks.

**Extension**: Allow custom logic before/after spec generation.

**Implementation**:

```java
public interface GenerationHook {
    void beforeGeneration(MavenProject project) throws MojoExecutionException;
    void afterGeneration(File specFile) throws MojoExecutionException;
}

// Configuration
<hooks>
    <hook>com.example.ValidationHook</hook>
    <hook>com.example.S3UploadHook</hook>
</hooks>
```

### 5. Spec Post-Processing

**Current**: No post-processing.

**Extension**: Modify spec after generation (add metadata, remove internal endpoints, etc.).

**Implementation**:

```java
public interface SpecPostProcessor {
    void process(JsonNode spec) throws IOException;
}

// Configuration
<postProcessors>
    <processor>com.example.MetadataEnricher</processor>
    <processor>com.example.InternalEndpointRemover</processor>
</postProcessors>
```

## Testing Strategy

### Unit Tests

**Scope**: Individual components in isolation.

**Test Cases**:

1. **JsonToYamlConverter**
   - Valid JSON to YAML conversion
   - Invalid JSON handling
   - Empty input handling
   - Special characters and escaping
   - Large documents

2. **Configuration Validation**
   - Valid configurations
   - Invalid output formats
   - Missing required parameters
   - Edge cases (empty strings, null values)

**Example**:

```java
@Test
public void testJsonToYamlConversion() throws IOException {
    JsonToYamlConverter converter = new JsonToYamlConverter();
    
    String json = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Test API\"}}";
    String yaml = converter.convertJsonToYaml(json);
    
    assertThat(yaml).contains("openapi: 3.0.1");
    assertThat(yaml).contains("title: Test API");
}
```

### Integration Tests

**Scope**: Full plugin execution with real Spring Boot application.

**Setup**:

1. Create test Spring Boot application
2. Configure plugin in test POM
3. Execute Maven goal
4. Verify generated spec

**Test Cases**:

1. **Successful Generation**
   - Application starts correctly
   - Spec is generated
   - YAML format is valid

2. **Timeout Handling**
   - Application takes too long to start
   - Plugin times out appropriately
   - Error message is clear

3. **Port Conflict**
   - Port already in use
   - Plugin fails with helpful message

4. **Profile Activation**
   - Correct profile is activated
   - Mocked beans are used
   - Real dependencies not required

**Example**:

```java
@Test
public void testPluginExecution() throws Exception {
    // Setup test project
    File projectDir = new File("src/test/resources/test-project");
    File targetDir = new File(projectDir, "target");
    
    // Execute plugin
    executeMaven(projectDir, "clean", "compile", "openapi-generator:generate");
    
    // Verify output
    File specFile = new File(targetDir, "openapi.yaml");
    assertThat(specFile).exists();
    
    String content = Files.readString(specFile.toPath());
    assertThat(content).contains("openapi: 3.0");
}
```

### Mock Configuration Testing

**Scope**: Verify mock configurations work correctly.

**Test Cases**:

1. **Database Mocking**
   - H2 in-memory database
   - JPA entities work
   - No real database needed

2. **External Service Mocking**
   - Kafka not required
   - Redis not required
   - HTTP clients mocked

3. **Spring Factories**
   - MockConfiguration loaded correctly
   - Mocked beans override real beans

## Performance Considerations

### Startup Time

**Factors**:
- Application size and complexity
- Number of dependencies
- JVM initialization
- Spring context creation
- Database initialization (even H2)

**Typical Times**:
- Simple app: 5-10 seconds
- Medium app: 10-20 seconds
- Complex app: 20-30 seconds

**Optimization Strategies**:

1. **Minimal Profile**
   - Exclude unnecessary auto-configurations
   - Disable unused features
   - Use lightweight database (H2)

2. **Classpath Optimization**
   - Only include necessary dependencies
   - Use `provided` scope where possible
   - Minimize test dependencies

3. **JVM Tuning**
   - Adjust heap size
   - Enable class data sharing
   - Use appropriate GC

### Memory Usage

**Peak Memory**:
- Plugin JVM: ~100-200 MB
- Application JVM: Depends on app (typically 200-500 MB)
- Total: ~300-700 MB

**Memory Optimization**:

```xml
<configuration>
    <jvmArgs>
        <arg>-Xms128m</arg>
        <arg>-Xmx256m</arg>
    </jvmArgs>
</configuration>
```

### Build Impact

**Typical Impact**:
- Clean build: +10-30 seconds
- Incremental build: +10-30 seconds (if classes changed)
- No-op build: 0 seconds (plugin skipped)

**When to Generate**:
- CI/CD pipelines: Every build
- Local development: Only when needed
- PR validation: Every PR

## Security Considerations

### 1. Profile Isolation

**Risk**: Production secrets/credentials in generated spec.

**Mitigation**:
- Use dedicated `openapi-generation` profile
- No real credentials in profile configuration
- Mock external services
- Override security configuration

### 2. Endpoint Exposure

**Risk**: Internal/admin endpoints exposed in spec.

**Mitigation**:
- Use `@Hidden` annotation for internal endpoints
- Configure SpringDoc to exclude patterns
- Post-process spec to remove sensitive endpoints

**Example**:

```yaml
# application-openapi-generation.yml
springdoc:
  paths-to-exclude:
    - /internal/**
    - /admin/**
```

### 3. Sensitive Data Leakage

**Risk**: API keys, tokens, or PII in examples or descriptions.

**Mitigation**:
- Review generated specs
- Use sanitized example data
- Configure SpringDoc to exclude examples
- Automated scanning for secrets

### 4. Process Isolation

**Risk**: Application process accesses production resources.

**Mitigation**:
- Use test profile with mocked dependencies
- No network access to production
- Use in-memory databases
- Override all external service configurations

### 5. Dependency Vulnerabilities

**Risk**: Plugin or application dependencies have security issues.

**Mitigation**:
- Regular dependency updates
- Automated vulnerability scanning
- Use Dependabot or similar tools
- Pin dependency versions

**Example CI Check**:

```yaml
- name: Security Scan
  run: mvn dependency-check:check
```

## Troubleshooting

### Common Issues

**Issue 1: Application Won't Start**

**Symptoms**: Timeout error, no /v3/api-docs endpoint.

**Diagnosis**:
```bash
# Check application logs
mvn clean compile openapi-generator:generate -X
```

**Solutions**:
- Increase `startupTimeout`
- Check MockConfiguration
- Verify all required beans are mocked

**Issue 2: Port Already in Use**

**Symptoms**: "Address already in use" error.

**Solutions**:
```xml
<configuration>
    <serverPort>8081</serverPort>
</configuration>
```

**Issue 3: Main Class Not Found**

**Symptoms**: "ClassNotFoundException" for main class.

**Solutions**:
```xml
<configuration>
    <mainClass>com.example.MyApplication</mainClass>
</configuration>
```

**Issue 4: Conversion Errors**

**Symptoms**: Invalid YAML generated.

**Diagnosis**: Check JSON format from SpringDoc.

**Solutions**:
- Verify SpringDoc version compatibility
- Check for custom serializers
- Validate JSON before conversion

**Issue 5: Custom API Docs Path Not Found**

**Symptoms**: 404 error, "Failed to fetch OpenAPI spec".

**Diagnosis**:
```bash
# Check if custom path is accessible
curl http://localhost:8080/your-custom-path

# Verify SpringDoc configuration
grep -r "springdoc.api-docs.path" src/main/resources/
```

**Solutions**:
```xml
<!-- Match the path in your application.yml -->
<configuration>
    <apiDocsPath>/your-custom-path</apiDocsPath>
</configuration>
```

**Common scenarios**:
- Application has custom SpringDoc path
- Application has servlet context path (e.g., `/myapp`)
- Using different SpringDoc version with different default path

## Conclusion

The OpenAPI Spec Generator Maven Plugin is built on solid architectural principles with clear separation of concerns, robust error handling, and extensive configuration options. The process-based approach provides excellent isolation while remaining simple to understand and maintain.

Key architectural strengths:
- Clean separation between plugin and application
- Flexible configuration system
- Robust lifecycle management
- Extensible design
- Comprehensive error handling

The design is production-ready but has clear extension points for future enhancements while maintaining backward compatibility.

