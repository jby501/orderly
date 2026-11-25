ALTER TABLE products
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE';

ALTER TABLE products
    ADD CONSTRAINT chk_products_status
        CHECK (status IN ('ON_SALE', 'SUSPENDED'));