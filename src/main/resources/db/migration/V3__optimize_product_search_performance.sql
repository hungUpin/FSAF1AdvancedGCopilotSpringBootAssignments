-- Migration for optimizing product search performance
-- Adds functional index for case-insensitive name searches

-- Create functional index for case-insensitive name searches (PostgreSQL/MySQL specific)
-- This dramatically improves LIKE query performance with LOWER() function
CREATE INDEX IF NOT EXISTS idx_product_name_lower_func ON products (LOWER(name));

-- Create partial index for description searches (only non-null descriptions)
CREATE INDEX IF NOT EXISTS idx_product_description_lower_func ON products (LOWER(description)) 
WHERE description IS NOT NULL;

-- Additional composite index for complex search scenarios
CREATE INDEX IF NOT EXISTS idx_product_search_composite ON products (category_id, price, stock);
