-- V4__add_fulltext_search_optimization.sql
-- Database optimization for product search performance

-- For MySQL: Create full-text indexes for natural language search
-- This dramatically improves performance for LIKE '%keyword%' queries
CREATE FULLTEXT INDEX idx_product_name_fulltext ON products (name);
CREATE FULLTEXT INDEX idx_product_description_fulltext ON products (description);
CREATE FULLTEXT INDEX idx_product_name_description_fulltext ON products (name, description);

-- For case-insensitive searches without function calls, ensure proper collation
-- This allows the database to use indexes even for case-insensitive comparisons
ALTER TABLE products MODIFY COLUMN name VARCHAR(255) COLLATE utf8mb4_unicode_ci;
ALTER TABLE products MODIFY COLUMN description TEXT COLLATE utf8mb4_unicode_ci;

-- Composite index optimization for common search + filter patterns
-- This supports queries that filter by category and search by name simultaneously
CREATE INDEX idx_product_search_category ON products (category_id, name);
CREATE INDEX idx_product_search_price_range ON products (price, name);

-- Alternative for PostgreSQL (commented out - uncomment if using PostgreSQL):
-- CREATE INDEX idx_product_name_gin ON products USING gin(to_tsvector('english', name));
-- CREATE INDEX idx_product_description_gin ON products USING gin(to_tsvector('english', description));
-- CREATE INDEX idx_product_combined_gin ON products USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
