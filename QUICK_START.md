# Quick Start Guide

Get up and running with OpenAPI spec generation in 5 minutes!

## Prerequisites Check

```bash
# Java 17+
java -version

# Maven 3.8+
mvn -version
```

## Step 1: Install the Plugin (1 minute)

```bash
cd openapi-spec-generator-maven-plugin
mvn clean install
cd ..
```

## Step 2: Add to Your Project (2 minutes)

### Add SpringDoc Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### Add Plugin Configuration

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
        </execution>
    </executions>
</plugin>
```

## Step 3: Create Profile (1 minute)

Create `src/main/resources/application-openapi-generation.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
springdoc:
  api-docs:
    enabled: true
```

## Step 4: Generate! (1 minute)

```bash
mvn clean compile openapi-spec-generator:generate
```

✅ Your spec is now in `target/openapi.yaml`!

## Try the Example

```bash
cd example-app
mvn clean compile openapi-spec-generator:generate
cat target/openapi.yaml
```

## Next Steps

- 📖 Read the [User Guide](docs/USER_GUIDE.md) for detailed configuration
- 🧪 Check the [Mocking Guide](docs/MOCKING_GUIDE.md) if you have dependencies
- ✅ Review [Spectral Integration](docs/SPECTRAL_INTEGRATION.md) for linting

## Common First-Time Issues

### Issue: Application won't start
**Solution**: Check you have H2 dependency and mocked any external services

### Issue: 404 on /v3/api-docs
**Solution**: Verify SpringDoc dependency is added

### Issue: Port 8080 in use
**Solution**: Add `<serverPort>8081</serverPort>` to plugin config

## Help

- See [Troubleshooting Guide](docs/TROUBLESHOOTING.md)
- Review the [Example Application](example-app/)
- Contact API Platform team

---

**That's it! You're ready to automate your OpenAPI spec generation! 🚀**

