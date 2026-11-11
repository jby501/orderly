package com.orderly.product;

import com.orderly.product.dto.ProductCreateRequest;
import com.orderly.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {

        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity()
        );

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }
}