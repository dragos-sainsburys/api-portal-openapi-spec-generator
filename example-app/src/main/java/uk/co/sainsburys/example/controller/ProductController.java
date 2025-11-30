package uk.co.sainsburys.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.sainsburys.example.model.ErrorResponse;
import uk.co.sainsburys.example.model.Product;
import uk.co.sainsburys.example.repository.ProductRepository;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Operation(summary = "Get all products", description = "Retrieves a list of all products in the catalogue")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of products")
    })
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Operation(summary = "Get product by ID", description = "Retrieves a single product by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved product"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getProductById(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable Long id) {
        return productRepository.findById(id)
                .map(product -> ResponseEntity.ok((Object) product))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(404, "Not Found", "Product with ID " + id + " not found")));
    }

    @Operation(summary = "Get product by SKU", description = "Retrieves a single product by its SKU")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved product"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/sku/{sku}")
    public ResponseEntity<Object> getProductBySku(
            @Parameter(description = "Product SKU", required = true, example = "SKU-12345")
            @PathVariable String sku) {
        return productRepository.findBySku(sku)
                .map(product -> ResponseEntity.ok((Object) product))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(404, "Not Found", "Product with SKU " + sku + " not found")));
    }

    @Operation(summary = "Create a new product", description = "Creates a new product in the catalogue")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product savedProduct = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @Operation(summary = "Update an existing product", description = "Updates product information by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateProduct(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody Product product) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", "Product with ID " + id + " not found"));
        }
        product.setId(id);
        Product updatedProduct = productRepository.save(product);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Delete a product", description = "Deletes a product from the catalogue by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProduct(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", "Product with ID " + id + " not found"));
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

