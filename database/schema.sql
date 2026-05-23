CREATE DATABASE IF NOT EXISTS style_sphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'fashion_app'@'localhost' IDENTIFIED BY 'fashion_app_password';
ALTER USER 'fashion_app'@'localhost' IDENTIFIED BY 'fashion_app_password';
GRANT SELECT, INSERT, UPDATE, DELETE, ALTER, CREATE, INDEX ON style_sphere.* TO 'fashion_app'@'localhost';
FLUSH PRIVILEGES;
USE style_sphere;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address_line VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    role ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    parent_id INT UNSIGNED,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_categories_slug (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(20) PRIMARY KEY,
    category_id INT UNSIGNED NOT NULL,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    mrp DECIMAL(10,2) NOT NULL,
    description TEXT NOT NULL,
    image VARCHAR(700) NOT NULL,
    images JSON NOT NULL,
    colors JSON NOT NULL,
    tags JSON NOT NULL,
    rating DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    review_count INT UNSIGNED NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_sizes (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    product_id VARCHAR(20) NOT NULL,
    size_label VARCHAR(20) NOT NULL,
    sku VARCHAR(50),
    stock_quantity INT UNSIGNED NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_product_size (product_id, size_label),
    CONSTRAINT fk_product_sizes_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(20) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    subtotal DECIMAL(12,2) NOT NULL,
    shipping_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    shipping_name VARCHAR(100) NOT NULL,
    shipping_phone VARCHAR(20) NOT NULL,
    shipping_email VARCHAR(255) NOT NULL,
    shipping_address TEXT NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_state VARCHAR(100) NOT NULL,
    shipping_pincode VARCHAR(10) NOT NULL,
    payment_id VARCHAR(120),
    payment_status ENUM('PENDING', 'CREATED', 'PAID', 'FAILED') NOT NULL DEFAULT 'PENDING',
    razorpay_order_id VARCHAR(120),
    payment_method VARCHAR(40),
    placed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_orders_order_number (order_number),
    KEY idx_orders_payment_status (payment_status),
    KEY idx_orders_razorpay_order_id (razorpay_order_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    product_id VARCHAR(20),
    product_size_id BIGINT UNSIGNED,
    product_name VARCHAR(200) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    size_label VARCHAR(20) NOT NULL,
    color VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cart_user (user_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT UNSIGNED NOT NULL,
    product_id VARCHAR(20) NOT NULL,
    product_size_id BIGINT UNSIGNED NOT NULL,
    color VARCHAR(50) NOT NULL,
    quantity INT UNSIGNED NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cart_line (cart_id, product_id, product_size_id, color),
    KEY fk_cart_items_product (product_id),
    KEY fk_cart_items_product_size (product_size_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product_size FOREIGN KEY (product_size_id) REFERENCES product_sizes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO categories (name, slug, parent_id, display_order, is_active) VALUES
('Men', 'men', NULL, 1, TRUE),
('Women', 'women', NULL, 2, TRUE),
('Accessories', 'accessories', NULL, 3, TRUE),
('Footwear', 'footwear', NULL, 4, TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name), display_order=VALUES(display_order), is_active=TRUE;

INSERT INTO products (id, category_id, name, brand, price, mrp, description, image, images, colors, tags, rating, review_count, is_active)
VALUES
('p1', (SELECT id FROM categories WHERE slug='women'), 'Ivory Pleated Co-ord Set', 'Studio Aster', 2899.00, 4299.00, 'Airy resort tailoring with a draped pleated shirt and wide-leg trousers.', 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Ivory'), JSON_ARRAY('coord', 'premium'), 4.6, 84, TRUE),
('p2', (SELECT id FROM categories WHERE slug='men'), 'Midnight Bomber Jacket', 'North Loom', 3499.00, 5299.00, 'Satin bomber with ribbed trims, tonal lining, and a city-ready cropped shape.', 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Navy'), JSON_ARRAY('jacket', 'men'), 4.7, 112, TRUE),
('p3', (SELECT id FROM categories WHERE slug='accessories'), 'Sculpted Leather Handbag', 'Mira Edit', 2199.00, 3299.00, 'Structured top-handle bag with polished hardware and everyday compartments.', 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Tan'), JSON_ARRAY('handbag'), 4.6, 73, TRUE),
('p4', (SELECT id FROM categories WHERE slug='women'), 'Rose Printed Midi Dress', 'Label Cove', 2599.00, 3899.00, 'Soft georgette dress with a gathered waist, tiered hem, and statement florals.', 'https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Rose'), JSON_ARRAY('dress'), 4.4, 65, TRUE),
('p5', (SELECT id FROM categories WHERE slug='men'), 'Utility Overshirt', 'Thread District', 1899.00, 2799.00, 'Cotton twill overshirt with clean pockets and a relaxed layering fit.', 'https://images.unsplash.com/photo-1506629905607-d9c297d3e19f?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1506629905607-d9c297d3e19f?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Olive'), JSON_ARRAY('shirt'), 4.2, 51, TRUE),
('p6', (SELECT id FROM categories WHERE slug='footwear'), 'Minimal Court Sneakers', 'Form Run', 2999.00, 4499.00, 'Low-profile sneakers with cushioned footbed and contrast heel tab.', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('White'), JSON_ARRAY('sneakers'), 4.8, 144, TRUE),
('p7', (SELECT id FROM categories WHERE slug='men'), 'Merino Blend Hoodie', 'North Loom', 2399.00, 3599.00, 'Premium mid-weight hoodie with a brushed interior, structured hood, and clean rib finishes.', 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1556821840-3a63f95609a7?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Grey'), JSON_ARRAY('hoodie'), 4.6, 97, TRUE),
('p8', (SELECT id FROM categories WHERE slug='women'), 'Tailored Linen Blazer', 'Studio Aster', 3299.00, 4999.00, 'Breathable linen blazer with a shaped waist, sharp lapels, and a polished everyday drape.', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Black'), JSON_ARRAY('jacket', 'blazer'), 4.7, 88, TRUE),
('p9', (SELECT id FROM categories WHERE slug='footwear'), 'Suede Runner Sneakers', 'Stride Atelier', 3799.00, 5599.00, 'Layered suede and mesh runners with a cushioned sole and refined streetwear profile.', 'https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Beige'), JSON_ARRAY('sneakers'), 4.8, 156, TRUE),
('p10', (SELECT id FROM categories WHERE slug='accessories'), 'Quilted Crossbody Handbag', 'Mira Edit', 2799.00, 4199.00, 'Compact quilted handbag with a chain strap, magnetic flap, and organized inner pockets.', 'https://images.unsplash.com/photo-1594223274512-ad4803739b7c?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1594223274512-ad4803739b7c?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Black'), JSON_ARRAY('handbag'), 4.5, 70, TRUE),
('p11', (SELECT id FROM categories WHERE slug='accessories'), 'Chronograph Steel Watch', 'Vesper Time', 4999.00, 7499.00, 'Polished steel chronograph with a sapphire-look dial, date window, and adjustable bracelet.', 'https://images.unsplash.com/photo-1524592094714-0f0654e20314?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1524592094714-0f0654e20314?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Silver'), JSON_ARRAY('watch'), 4.7, 119, TRUE),
('p12', (SELECT id FROM categories WHERE slug='men'), 'Oxford Slim Shirt', 'Thread District', 1699.00, 2499.00, 'Crisp cotton oxford shirt with a slim fit, button-down collar, and premium stitch detailing.', 'https://images.unsplash.com/photo-1598033129183-c4f50c736f10?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1598033129183-c4f50c736f10?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Blue'), JSON_ARRAY('shirt'), 4.4, 62, TRUE),
('p13', (SELECT id FROM categories WHERE slug='women'), 'Satin Wrap Dress', 'Label Cove', 3199.00, 4799.00, 'Liquid satin wrap dress with a flattering waist tie, midi length, and evening-ready sheen.', 'https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Emerald'), JSON_ARRAY('dress'), 4.6, 91, TRUE),
('p14', (SELECT id FROM categories WHERE slug='men'), 'Selvedge Straight Jeans', 'Denim House', 2899.00, 4299.00, 'Deep indigo straight jeans cut from sturdy selvedge-inspired denim with clean hardware.', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Indigo'), JSON_ARRAY('jeans'), 4.5, 76, TRUE),
('p15', (SELECT id FROM categories WHERE slug='women'), 'High Rise Wide Jeans', 'Denim House', 2699.00, 3999.00, 'High-rise wide-leg jeans with a soft vintage wash and a clean elongated silhouette.', 'https://images.unsplash.com/photo-1582418702059-97ebafb35d09?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1582418702059-97ebafb35d09?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Washed Blue'), JSON_ARRAY('jeans'), 4.5, 80, TRUE),
('p16', (SELECT id FROM categories WHERE slug='footwear'), 'Leather Chelsea Boots', 'Form Run', 4299.00, 6499.00, 'Premium ankle boots with elastic side panels, stacked heel, and polished leather finish.', 'https://images.unsplash.com/photo-1608256246200-53e635b5b65f?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1608256246200-53e635b5b65f?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Brown'), JSON_ARRAY('boots', 'footwear'), 4.6, 68, TRUE),
('p17', (SELECT id FROM categories WHERE slug='accessories'), 'Italian Leather Belt', 'Vesper Goods', 1499.00, 2299.00, 'Full-grain leather belt with a brushed metal buckle and refined edge finishing.', 'https://images.unsplash.com/photo-1624222247344-550fb60583dc?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1624222247344-550fb60583dc?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Cognac'), JSON_ARRAY('belt'), 4.3, 49, TRUE),
('p18', (SELECT id FROM categories WHERE slug='women'), 'Cashmere Rib Knit Cardigan', 'Studio Aster', 3699.00, 5499.00, 'Soft rib knit cardigan with tonal buttons, relaxed sleeves, and a premium layered feel.', 'https://images.unsplash.com/photo-1618244972963-dbee1a7edc95?auto=format&fit=crop&w=900&q=80', JSON_ARRAY('https://images.unsplash.com/photo-1618244972963-dbee1a7edc95?auto=format&fit=crop&w=900&q=80'), JSON_ARRAY('Cream'), JSON_ARRAY('knitwear'), 4.7, 83, TRUE)
ON DUPLICATE KEY UPDATE
category_id=VALUES(category_id), name=VALUES(name), brand=VALUES(brand), price=VALUES(price), mrp=VALUES(mrp),
description=VALUES(description), image=VALUES(image), images=VALUES(images), colors=VALUES(colors), tags=VALUES(tags),
rating=VALUES(rating), review_count=VALUES(review_count), is_active=TRUE;

INSERT INTO product_sizes (product_id, size_label, sku, stock_quantity, is_active)
SELECT id, 'Free Size', CONCAT(id, '-FREE'), 24, TRUE FROM products
ON DUPLICATE KEY UPDATE stock_quantity=GREATEST(stock_quantity, VALUES(stock_quantity)), is_active=TRUE;
