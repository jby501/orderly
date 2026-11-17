package com.orderly.product;

import com.orderly.product.dto.ProductCreateRequest;
import com.orderly.product.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse response = productService.createProduct(request);

        URI location = URI.create("/api/v1/products/" + response.id());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long productId
    ) {
        ProductResponse response = productService.getProduct(productId);

        return ResponseEntity.ok(response);
    }
}