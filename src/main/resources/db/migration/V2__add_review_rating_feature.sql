-- Add rating columns to products table
ALTER TABLE products ADD COLUMN average_rating DOUBLE DEFAULT 0.0;
ALTER TABLE products ADD COLUMN review_count INTEGER DEFAULT 0;

-- Update reviews table to add constraints
ALTER TABLE reviews ADD CONSTRAINT uk_reviews_user_product UNIQUE (user_id, product_id);
ALTER TABLE reviews MODIFY COLUMN content VARCHAR(1000) NOT NULL;
ALTER TABLE reviews MODIFY COLUMN rating INTEGER NOT NULL;
ALTER TABLE reviews ADD CONSTRAINT chk_rating_range CHECK (rating >= 1 AND rating <= 5);
ALTER TABLE reviews MODIFY COLUMN product_id BIGINT NOT NULL;
ALTER TABLE reviews MODIFY COLUMN user_id BIGINT NOT NULL;

-- Create indexes for better performance
CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);
CREATE INDEX idx_products_average_rating ON products(average_rating);

-- Update existing products to calculate current ratings (if any reviews exist)
UPDATE products p 
SET average_rating = COALESCE((
    SELECT AVG(r.rating) 
    FROM reviews r 
    WHERE r.product_id = p.id
), 0.0),
review_count = COALESCE((
    SELECT COUNT(*) 
    FROM reviews r 
    WHERE r.product_id = p.id
), 0);
