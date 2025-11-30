package uk.co.sainsburys.example.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Error message", example = "Product not found")
    private String message;

    @Schema(description = "Detailed error description", example = "No product found with SKU: SKU-12345")
    private String details;

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String message, String details) {
        this.status = status;
        this.message = message;
        this.details = details;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

