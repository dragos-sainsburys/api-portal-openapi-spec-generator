# Contributing to OpenAPI Spec Generator

Thank you for your interest in contributing to the OpenAPI Spec Generator project!

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Git
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

### Setting Up Development Environment

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd api-portal-openapi-spec-generator
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run tests:
   ```bash
   mvn test
   ```

4. Try the example app:
   ```bash
   cd example-app
   mvn openapi-generator:generate
   ```

## Project Structure

- `openapi-spec-generator-maven-plugin/` - The Maven plugin
- `example-app/` - Example Spring Boot application
- `docs/` - Documentation
- `.github/workflows/` - CI/CD pipelines

## Making Changes

### Code Style

- Follow Java coding conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods focused and concise

### Testing

- Add unit tests for new functionality
- Update existing tests if behavior changes
- Ensure all tests pass before submitting

### Documentation

- Update relevant documentation in `docs/`
- Add examples for new features
- Update README.md if needed

## Submitting Changes

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and commit:
   ```bash
   git add .
   git commit -m "Description of your changes"
   ```

3. Push to your branch:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request

## Pull Request Guidelines

- Provide a clear description of the changes
- Reference any related issues
- Ensure CI/CD checks pass
- Keep PRs focused on a single feature/fix
- Update documentation as needed

## Reporting Issues

When reporting issues, please include:

- Java and Maven versions
- Spring Boot version
- Steps to reproduce
- Expected vs actual behavior
- Error messages and stack traces
- Sample code if applicable

## Questions?

If you have questions, feel free to:
- Open an issue for discussion
- Contact the API Platform team
- Review existing documentation

Thank you for contributing! 🎉

