package com.orderly.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndStatus(
            Long id,
            ProductStatus status
    );

    Page<Product> findAllByStatus(
            ProductStatus status,
            Pageable pageable
    );
}