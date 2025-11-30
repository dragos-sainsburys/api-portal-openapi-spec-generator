# OpenAPI Spec Generator for Java Spring Boot

A Maven plugin and tooling suite for automatically generating OpenAPI specifications from Spring Boot applications at build time, with seamless integration into Sainsbury's API standards and Spectral linting.

## 🎯 Overview

This project provides a complete solution for Java Spring Boot teams to:

- **Automatically generate** OpenAPI specs during Maven builds
- **Convert JSON to YAML** format automatically
- **Mock dependencies** easily for spec generation
- **Integrate with Spectral** for Sainsbury's API standard validation
- **Minimize manual maintenance** of API documentation

## 📋 Table of Contents

- [Features](#-features)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [Documentation](#-documentation)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Usage Examples](#-usage-examples)
- [CI/CD Integration](#-cicd-integration)
- [How It Works](#-how-it-works)
- [Contributing](#-contributing)
- [Support](#-support)

## ✨ Features

### Maven Plugin
- 🔄 Automatic spec generation during build
- 📝 JSON to YAML conversion
- ⚙️ Configurable output formats and paths
- 🚀 Auto-detects Spring Boot main class
- ⏱️ Configurable startup timeout
- 🎛️ Profile-based configuration

### Mocking Support
- 🧪 Dedicated test profile for spec generation
- 🔌 Easy mocking of databases, Kafka, Redis, etc.
- 📚 Comprehensive mocking guide
- 🎯 Minimal configuration required

### Integration
- ✅ Spectral linting support
- 🔗 CI/CD pipeline ready
- 📊 GitHub Actions examples
- 🏗️ Maven lifecycle integration

## 🚀 Quick Start

### 1. Add SpringDoc Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 2. Install the Plugin

```bash
cd openapi-spec-generator-maven-plugin
mvn clean install
```

### 3. Configure Your Project

Add to your Spring Boot application's `pom.xml`:

```xml
<plugin>
    <groupId>uk.co.sainsburys</groupId>
    <artifactId>openapi-spec-generator-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <outputFormat>yaml</outputFormat>
                <springProfile>openapi-generation</springProfile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 4. Create OpenAPI Generation Profile

Create `src/main/resources/application-openapi-generation.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration

springdoc:
  api-docs:
    enabled: true
```

### 5. Generate Your Spec

```bash
mvn clean compile openapi-spec-generator:generate
```

Your OpenAPI spec will be in `target/openapi.yaml` 🎉

## 📁 Project Structure

```
api-portal-openapi-spec-generator/
├── openapi-spec-generator-maven-plugin/   # Maven plugin source code
│   ├── src/
│   │   ├── main/java/uk/co/sainsburys/maven/
│   │   │   ├── OpenApiGeneratorMojo.java
│   │   │   └── JsonToYamlConverter.java
│   │   └── test/java/
│   └── pom.xml
│
├── example-app/                            # Example Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/uk/co/sainsburys/example/
│   │   │   │   ├── ExampleApplication.java
│   │   │   │   ├── controller/
│   │   │   │   ├── model/
│   │   │   │   └── repository/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── application-openapi-generation.yml
│   │   └── test/
│   │       ├── java/uk/co/sainsburys/example/config/
│   │       │   └── MockConfiguration.java
│   │       └── resources/META-INF/
│   │           └── spring.factories
│   └── pom.xml
│
├── docs/                                   # Documentation
│   ├── USER_GUIDE.md
│   ├── MOCKING_GUIDE.md
│   └── SPECTRAL_INTEGRATION.md
│
├── .github/workflows/                      # CI/CD examples
│   ├── build.yml
│   └── openapi-validation.yml
│
├── pom.xml                                 # Parent POM
└── README.md
```

## 📚 Documentation

Comprehensive guides are available:

### User Documentation
- **[Quick Start](QUICK_START.md)** - Get up and running in 5 minutes
- **[User Guide](docs/USER_GUIDE.md)** - Plugin installation, configuration, and usage
- **[Mocking Guide](docs/MOCKING_GUIDE.md)** - How to mock dependencies for spec generation
- **[Spectral Integration](docs/SPECTRAL_INTEGRATION.md)** - Linting specs against Sainsbury's standards
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions

### Implementation Documentation
- **[Technical Architecture](docs/ARCHITECTURE.md)** - Deep-dive into plugin architecture and design decisions
- **[Production Readiness](docs/PRODUCTION_READINESS.md)** - Roadmap for moving from POC to production

### Developer Documentation
- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute to the project
- **[Presentation Overview](docs/PRESENTATION.md)** - POC overview and key takeaways
- **[Changelog](docs/CHANGELOG.md)** - Version history and release notes

## 🔧 Prerequisites

- **Java**: 17 or later
- **Maven**: 3.8.0 or later
- **Spring Boot**: 3.x
- **Node.js**: 22 or later (for Spectral linting)

## 💿 Installation

### For Plugin Development

```bash
# Clone the repository
git clone <repository-url>
cd api-portal-openapi-generator-poc

# Build and install the plugin
mvn clean install
```

### For Using in Your Project

1. Install the plugin locally (as above)
2. Add plugin configuration to your `pom.xml` (see Quick Start)
3. Create the `openapi-generation` profile
4. Run `mvn openapi-spec-generator:generate`

## 📖 Usage Examples

### Basic Generation

```bash
# Generate OpenAPI spec
mvn openapi-spec-generator:generate
```

### Generate with Custom Configuration

```xml
<configuration>
    <outputFormat>both</outputFormat>
    <serverPort>8081</serverPort>
    <startupTimeout>90</startupTimeout>
    
    <!-- Custom API docs path (if SpringDoc configured differently) -->
    <apiDocsPath>/api/documentation</apiDocsPath>
</configuration>
```

**Common Configuration Scenarios**:

```xml
<!-- Scenario 1: Custom SpringDoc path -->
<apiDocsPath>/custom-path/api-docs</apiDocsPath>

<!-- Scenario 2: Application with context path -->
<apiDocsPath>/myapp/v3/api-docs</apiDocsPath>

<!-- Scenario 3: Different port -->
<serverPort>8081</serverPort>
```

### Generate as Part of Build

```bash
# Plugin runs automatically during build
mvn clean package
```

### Validate with Spectral

```bash
# Generate and lint
mvn clean compile openapi-generator:generate
api-lint target/openapi.yaml --mode strict
```

## 🔄 CI/CD Integration

### GitHub Actions

See [`.github/workflows/openapi-validation.yml`](.github/workflows/openapi-validation.yml) for a complete example:

```yaml
- name: Generate OpenAPI Spec
  run: mvn clean compile openapi-generator:generate

- name: Lint OpenAPI Spec
  run: api-lint target/openapi.yaml --mode strict
```

### GitLab CI

```yaml
build:
  script:
    - mvn clean compile openapi-generator:generate
  artifacts:
    paths:
      - target/openapi.yaml
```

## ⚙️ How It Works

### Plugin Execution Flow

1. **Compilation**: Maven compiles your application classes
2. **Plugin Activation**: `openapi-generator:generate` goal executes
3. **Profile Setup**: Activates `openapi-generation` Spring profile
4. **Application Start**: Launches your Spring Boot app with mocked dependencies
5. **Endpoint Fetch**: Retrieves OpenAPI spec from `/v3/api-docs`
6. **Format Conversion**: Converts JSON to YAML (if configured)
7. **File Output**: Writes spec to `target/openapi.yaml`
8. **Cleanup**: Shuts down the application

### Mocking Strategy

```
Your Application
     ↓
Test Profile (openapi-generation)
     ↓
MockConfiguration (mocked beans)
     ↓
Minimal Dependencies
     ↓
Successful Startup → Spec Generation
```

### Key Design Decisions

- **SpringDoc**: Uses standard Spring Boot integration
- **Test Profile**: Isolated profile prevents production config interference
- **H2 Database**: Lightweight in-memory database for JPA entities
- **Mockito**: Simple mocking without complex test frameworks
- **Jackson**: Standard library for JSON/YAML conversion

## 🎯 Example Application

The `example-app` module demonstrates:

- ✅ Full REST API with OpenAPI annotations
- ✅ JPA entities with schema documentation
- ✅ Multiple endpoints with proper documentation
- ✅ Error response models
- ✅ Mock configuration for dependencies
- ✅ Test profile setup
- ✅ Generated spec examples

### Running the Example

```bash
cd example-app

# Generate the spec
mvn clean compile openapi-generator:generate

# View the generated spec
cat target/openapi.yaml

# Run the application normally
mvn spring-boot:run
```

## 🏗️ Architecture

### Plugin Components

**OpenApiGeneratorMojo**
- Maven goal implementation
- Application lifecycle management
- Configuration handling

**JsonToYamlConverter**
- Format conversion utility
- Jackson-based transformation
- Preserves structure and formatting

### Configuration Hierarchy

```
pom.xml (plugin config)
    ↓
application-openapi-generation.yml
    ↓
MockConfiguration.java
    ↓
spring.factories
```

## 🤝 Contributing

Contributions are welcome! Please:

1. Review existing documentation
2. Test changes with the example application
3. Update documentation as needed
4. Follow existing code style
5. Add tests for new features

## 🐛 Troubleshooting

### Application Won't Start

- Check application logs for errors
- Verify all required beans are mocked
- Increase `startupTimeout` if needed
- Review [Mocking Guide](docs/MOCKING_GUIDE.md)

### Port Already in Use

```xml
<configuration>
    <serverPort>8081</serverPort>
</configuration>
```

### Spec Not Generated

- Ensure SpringDoc dependency is present
- Verify `/v3/api-docs` endpoint is accessible
- Check `springdoc.api-docs.enabled=true`

### Linting Failures

- Review [Spectral Integration Guide](docs/SPECTRAL_INTEGRATION.md)
- Check your OpenAPI annotations
- Validate against Sainsbury's standards

## 📊 Benefits

### For Development Teams

- 📉 Reduced manual documentation effort
- 🔒 Guaranteed spec accuracy (generated from code)
- ⚡ Fast iteration cycles
- 🎯 Automated quality checks

### For API Consumers

- 📖 Always up-to-date documentation
- ✅ Standards compliance
- 🔍 Better discoverability
- 📝 Consistent API definitions

### For Organization

- 📏 Enforced API standards
- 🔄 Consistent tooling across teams
- 📈 Improved API governance
- 🚀 Faster onboarding

## 📜 License

This project is proprietary to Sainsbury's Tech.

## 🆘 Support

For questions or issues:

1. Check the [documentation](docs/)
2. Review the [example application](example-app/)
3. Contact the API Platform team
4. Raise an issue in the repository

## 🔗 Related Resources

- [Sainsbury's OpenAPI Standards](https://engineering.sainsburys.co.uk/policies/interface/openapi/)
- [Sainsbury's API Style Guide](https://github.com/sainsburys-tech/api-style-guide)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- [Spectral Linting](https://stoplight.io/open-source/spectral)

---

**Built with ❤️ by Sainsbury's Tech**

