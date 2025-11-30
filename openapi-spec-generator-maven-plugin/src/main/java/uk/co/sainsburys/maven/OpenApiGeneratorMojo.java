package uk.co.sainsburys.maven;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Maven Mojo for generating OpenAPI specifications from Spring Boot applications.
 * This plugin starts the Spring Boot application, fetches the OpenAPI spec from the
 * /v3/api-docs endpoint, and optionally converts it to YAML format.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class OpenApiGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The directory where the OpenAPI spec files will be generated.
     */
    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}")
    private File outputDir;

    /**
     * The API docs endpoint path.
     */
    @Parameter(property = "apiDocsPath", defaultValue = "/v3/api-docs")
    private String apiDocsPath;

    /**
     * The output format: json, yaml, or both.
     */
    @Parameter(property = "outputFormat", defaultValue = "yaml")
    private String outputFormat;

    /**
     * The Spring profile to activate when starting the application.
     */
    @Parameter(property = "springProfile", defaultValue = "openapi-generation")
    private String springProfile;

    /**
     * Whether to fail the build on error.
     */
    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * The port on which the application will run.
     */
    @Parameter(property = "serverPort", defaultValue = "8080")
    private int serverPort;

    /**
     * Maximum time to wait for application startup (in seconds).
     */
    @Parameter(property = "startupTimeout", defaultValue = "60")
    private int startupTimeout;

    /**
     * The main class to execute (Spring Boot application class).
     */
    @Parameter(property = "mainClass")
    private String mainClass;

    private static final String JSON_FILENAME = "openapi.json";
    private static final String YAML_FILENAME = "openapi.yaml";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("Starting OpenAPI spec generation...");
            
            // Validate configuration
            validateConfiguration();
            
            // Ensure output directory exists
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory: " + outputDir);
            }

            // Find main class if not specified
            if (mainClass == null || mainClass.isEmpty()) {
                mainClass = findMainClass();
            }

            getLog().info("Using main class: " + mainClass);
            getLog().info("Spring profile: " + springProfile);
            getLog().info("Output directory: " + outputDir.getAbsolutePath());

            // Start Spring Boot application
            Process appProcess = startApplication();
            
            try {
                // Wait for application to be ready
                waitForApplicationReady();
                
                // Fetch OpenAPI spec
                String jsonSpec = fetchOpenApiSpec();
                
                // Save JSON if requested
                if ("json".equalsIgnoreCase(outputFormat) || "both".equalsIgnoreCase(outputFormat)) {
                    File jsonFile = new File(outputDir, JSON_FILENAME);
                    Files.writeString(jsonFile.toPath(), jsonSpec, StandardCharsets.UTF_8);
                    getLog().info("Generated JSON spec: " + jsonFile.getAbsolutePath());
                }
                
                // Convert to YAML if requested
                if ("yaml".equalsIgnoreCase(outputFormat) || "both".equalsIgnoreCase(outputFormat)) {
                    File jsonFile = new File(outputDir, JSON_FILENAME);
                    File yamlFile = new File(outputDir, YAML_FILENAME);
                    
                    // Save JSON temporarily if not already saved
                    if (!"both".equalsIgnoreCase(outputFormat)) {
                        Files.writeString(jsonFile.toPath(), jsonSpec, StandardCharsets.UTF_8);
                    }
                    
                    JsonToYamlConverter converter = new JsonToYamlConverter();
                    converter.convert(jsonFile, yamlFile);
                    getLog().info("Generated YAML spec: " + yamlFile.getAbsolutePath());
                    
                    // Clean up temp JSON if only YAML was requested
                    if ("yaml".equalsIgnoreCase(outputFormat)) {
                        jsonFile.delete();
                    }
                }
                
                getLog().info("OpenAPI spec generation completed successfully!");
                
            } finally {
                // Always stop the application
                stopApplication(appProcess);
            }
            
        } catch (Exception e) {
            String message = "Failed to generate OpenAPI spec: " + e.getMessage();
            if (failOnError) {
                throw new MojoExecutionException(message, e);
            } else {
                getLog().error(message, e);
            }
        }
    }

    private void validateConfiguration() throws MojoExecutionException {
        if (!outputFormat.matches("(?i)json|yaml|both")) {
            throw new MojoExecutionException("Invalid outputFormat: " + outputFormat + ". Must be json, yaml, or both.");
        }
    }

    private String findMainClass() throws MojoExecutionException {
        // Try to find Spring Boot main class from project properties
        Object mainClassProp = project.getProperties().get("start-class");
        if (mainClassProp != null) {
            return mainClassProp.toString();
        }
        
        // Check for common main class location
        String basePackage = project.getGroupId() + "." + project.getArtifactId().replace("-", "");
        String possibleMainClass = basePackage + ".Application";
        
        getLog().warn("Main class not specified. Attempting to use: " + possibleMainClass);
        getLog().warn("If this is incorrect, please specify the mainClass parameter.");
        
        return possibleMainClass;
    }

    private Process startApplication() throws IOException, MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        command.add(buildClasspath());
        command.add("-Dspring.profiles.active=" + springProfile);
        command.add("-Dserver.port=" + serverPort);
        command.add("-Dspringdoc.api-docs.enabled=true");
        command.add("-Dspringdoc.api-docs.path=" + apiDocsPath);
        command.add(mainClass);

        getLog().info("Starting application with command: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Start a thread to consume the output
        Thread outputConsumer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().debug("[APP] " + line);
                }
            } catch (IOException e) {
                getLog().error("Error reading application output", e);
            }
        });
        outputConsumer.setDaemon(true);
        outputConsumer.start();

        return process;
    }

    private String buildClasspath() {
        String buildDirectory = project.getBuild().getOutputDirectory();
        String testDirectory = project.getBuild().getTestOutputDirectory();
        
        StringBuilder classpath = new StringBuilder();
        classpath.append(buildDirectory);
        classpath.append(File.pathSeparator);
        classpath.append(testDirectory);
        
        // Add all dependencies
        if (project.getArtifacts() != null) {
            project.getArtifacts().forEach(artifact -> {
                if (artifact.getFile() != null) {
                    classpath.append(File.pathSeparator);
                    classpath.append(artifact.getFile().getAbsolutePath());
                }
            });
        }
        
        getLog().debug("Classpath: " + classpath.toString());
        return classpath.toString();
    }

    private void waitForApplicationReady() throws MojoExecutionException, InterruptedException {
        String url = "http://localhost:" + serverPort + apiDocsPath;
        getLog().info("Waiting for application to be ready at: " + url);

        long startTime = System.currentTimeMillis();
        long timeoutMillis = TimeUnit.SECONDS.toMillis(startupTimeout);

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (response.getCode() == 200) {
                        getLog().info("Application is ready!");
                        return;
                    }
                }
            } catch (IOException e) {
                // Application not ready yet, continue waiting
                getLog().debug("Application not ready yet: " + e.getMessage());
            }

            Thread.sleep(1000);
        }

        throw new MojoExecutionException("Application failed to start within " + startupTimeout + " seconds");
    }

    private String fetchOpenApiSpec() throws IOException, MojoExecutionException {
        String url = "http://localhost:" + serverPort + apiDocsPath;
        getLog().info("Fetching OpenAPI spec from: " + url);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    throw new MojoExecutionException("Failed to fetch OpenAPI spec. Status: " + response.getCode());
                }
                try {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } catch (org.apache.hc.core5.http.ParseException e) {
                    throw new MojoExecutionException("Failed to parse OpenAPI spec response", e);
                }
            }
        }
    }

    private void stopApplication(Process process) {
        if (process != null && process.isAlive()) {
            getLog().info("Stopping application...");
            process.destroy();
            try {
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    getLog().warn("Application did not stop gracefully, forcing shutdown...");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}

