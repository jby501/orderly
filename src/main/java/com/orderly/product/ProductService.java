package com.orderly.product;

import com.orderly.product.dto.ProductCreateRequest;
import com.orderly.product.dto.ProductResponse;
import com.orderly.product.dto.ProductUpdateRequest;
import com.orderly.product.dto.ProductStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findByIdAndStatus(
                        productId,
                        ProductStatus.ON_SALE
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "상품을 찾을 수 없습니다."
                        )
                );

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = findProduct(productId);
        product.updateDetails(request.name(), request.description(), request.price());

        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository
                .findAllByStatus(ProductStatus.ON_SALE, pageable)
                .map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse updateStatus(
            Long productId,
            ProductStatusUpdateRequest request
    ) {
        Product product = findProduct(productId);

        product.changeStatus(request.status());

        return ProductResponse.from(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "상품을 찾을 수 없습니다."
                        )
                );
    }


}