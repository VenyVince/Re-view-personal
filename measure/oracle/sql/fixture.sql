WHENEVER SQLERROR EXIT FAILURE

-- Hand-checkable fixture. Ids below 1000 are explicit. Identity columns start at 1000.
-- User 1 is the recommendation subject: axes D,S,P,W.
-- Product 1 matches all 4 axes. Product 2 matches D,S only. Product 16 matches none.
-- Product 17 has THIRD null. Product 3 is deleted and shares the keyword 측정토너.
-- Product 4 stores review_count 40 with 7 review rows, for the scheduler formula.
-- Product 18 stock is 0. Reviews 1 and 2 on product 1 are the 1.2x rank pair.

INSERT INTO baumann (baumann_id, type, first, second, third, fourth) VALUES (1, 'DSPW', 'D', 'S', 'P', 'W');
INSERT INTO baumann (baumann_id, type, first, second, third, fourth) VALUES (2, 'ORNT', 'O', 'R', 'N', 'T');
INSERT INTO baumann (baumann_id, type, first, second, third, fourth) VALUES (3, 'DSNT', 'D', 'S', 'N', 'T');
INSERT INTO baumann (baumann_id, type, first, second, third, fourth) VALUES (4, 'DSPT', 'D', 'S', 'P', 'T');
INSERT INTO baumann (baumann_id, type, first, second, third, fourth) VALUES (5, 'DSNW', 'D', 'S', NULL, 'W');

INSERT INTO user_table (user_id, id, password, name, email, nickname, phone_number, baumann_id, role, point)
VALUES (1, 'measure_user', 'measure-not-a-login', '김측정', 'm1@measure.local', '측정유저', '01000000001', 1, 'USER', 10000);
INSERT INTO user_table (user_id, id, password, name, email, nickname, phone_number, baumann_id, role, point)
VALUES (2, 'other_user', 'measure-not-a-login', '김다른', 'm2@measure.local', '다른유저', '01000000002', 2, 'USER', 500);
INSERT INTO user_table (user_id, id, password, name, email, nickname, phone_number, baumann_id, role, point)
VALUES (3, 'empty_user', 'measure-not-a-login', '김없음', 'm3@measure.local', '주문이없는유저', '01000000003', 1, 'USER', 0);
INSERT INTO user_table (user_id, id, password, name, email, nickname, phone_number, baumann_id, role, point)
VALUES (4, 'author_user', 'measure-not-a-login', '김작성', 'm4@measure.local', '작성유저', '01000000004', 1, 'USER', 0);
INSERT INTO user_table (user_id, id, password, name, email, nickname, phone_number, baumann_id, role, point)
VALUES (5, 'admin_user', 'measure-not-a-login', '김관리', 'm5@measure.local', '관리자유저', '01000000005', 2, 'ADMIN', 0);

INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (1, '측정토너', 'MeasureBrand', '토너성분', '토너설명', 12000, 'skincare', 20, 4.5, 2, 1, '0', DATE '2026-03-01');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (2, '측정로션', 'OtherBrand', '로션성분', '로션설명', 8000, 'skincare', 15, 3.0, 1, 3, '0', DATE '2026-02-01');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at, deleted_at)
VALUES (3, '측정토너-단종', 'MeasureBrand', '단종성분', '단종설명', 9000, 'skincare', 5, 4.0, 0, 1, '0', DATE '2026-04-01', DATE '2026-04-02');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (4, '스케줄세럼', 'SchedBrand', '세럼성분', '세럼설명', 15000, 'skincare', 30, 4.2, 40, 1, '0', DATE '2026-01-15');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (5, '저가고평점', 'SortBrand', '정렬성분', '정렬설명', 5000, 'skincare', 10, 4.9, 2, 1, '0', DATE '2026-01-01');

BEGIN
    FOR i IN 6..15 LOOP
        INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
        VALUES (i, '필러상품' || LPAD(i, 2, '0'), 'FillerBrand', '필러성분', '필러설명', 20000 + i, 'skincare', 10, 3.0, 2, 1, '0', DATE '2025-01-01');
    END LOOP;
END;
/

INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (16, '불일치상품', 'ZeroBrand', '불일치성분', '불일치설명', 30000, 'skincare', 10, 1.0, 0, 2, '0', DATE '2025-06-01');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (17, '널축상품', 'NullBrand', '널성분', '널설명', 11000, 'skincare', 10, 4.0, 0, 5, '0', DATE '2025-07-01');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (18, '재고없음', 'StockBrand', '재고성분', '재고설명', 7000, 'skincare', 0, 3.5, 0, 1, '0', DATE '2025-08-01');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (19, '필러상품19', 'FillerBrand', '필러성분', '필러설명', 20019, 'skincare', 10, 3.0, 2, 1, '0', DATE '2025-01-01');
INSERT INTO product (product_id, prd_name, prd_brand, ingredient, description, price, category, stock, rating, review_count, baumann_id, is_sold_out, created_at)
VALUES (20, '필러상품20', 'FillerBrand', '필러성분', '필러설명', 20020, 'skincare', 10, 3.0, 0, 1, '0', DATE '2025-01-01');

BEGIN
    FOR i IN 1..5 LOOP
        INSERT INTO product_image (product_id, image_url, is_thumbnail)
        VALUES (i, 'products/fixture/p' || i || '.jpg', 'Y');
    END LOOP;
END;
/

INSERT INTO orders (order_id, order_no, user_id, total_price, used_point) VALUES (1, 'M-1', 1, 12000, 0);
INSERT INTO orders (order_id, order_no, user_id, total_price, used_point) VALUES (2, 'M-2', 1, 12000, 0);
INSERT INTO orders (order_id, order_no, user_id, total_price, used_point) VALUES (3, 'M-3', 2, 8000, 0);
INSERT INTO orders (order_id, order_no, user_id, total_price, used_point) VALUES (4, 'M-4', 1, 12000, 0);

INSERT INTO order_item (order_item_id, order_id, product_id, product_name, product_price, quantity)
VALUES (1, 1, 1, '측정토너', 12000, 1);
INSERT INTO order_item (order_item_id, order_id, product_id, product_name, product_price, quantity)
VALUES (2, 2, 1, '측정토너', 12000, 1);
INSERT INTO order_item (order_item_id, order_id, product_id, product_name, product_price, quantity)
VALUES (3, 3, 2, '측정로션', 8000, 1);
INSERT INTO order_item (order_item_id, order_id, product_id, product_name, product_price, quantity)
VALUES (4, 4, 1, '측정토너', 12000, 1);

INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, order_item_id, like_count, is_checked)
VALUES (1, 1, 1, '풀매치 기본', 4.5, DATE '2026-03-02', 2, 5, 0);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, order_item_id, like_count, is_checked)
VALUES (2, 1, 1, '풀매치 베스트가중', 2.0, DATE '2026-03-03', 4, 1, 1);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count, is_checked)
VALUES (3, 2, 1, '리뷰검색어 부분일치', 3.0, DATE '2026-02-02', 0, 0);

INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count)
VALUES (4, 4, 4, '스케줄후보1', 4.0, DATE '2026-01-16', 30);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count)
VALUES (5, 4, 4, '스케줄후보2', 4.0, DATE '2026-01-17', 28);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count)
VALUES (6, 4, 4, '스케줄후보3', 4.0, DATE '2026-01-18', 26);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count)
VALUES (7, 4, 4, '스케줄후보4', 4.0, DATE '2026-01-19', 24);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count)
VALUES (8, 4, 4, '스케줄후보5', 4.0, DATE '2026-01-20', 22);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count)
VALUES (9, 4, 4, '스케줄제외좋아요', 4.0, DATE '2026-01-21', 10);
INSERT INTO review (review_id, product_id, user_id, content, rating, created_at, like_count)
VALUES (10, 4, 4, '스케줄제외하위', 4.0, DATE '2026-01-22', 5);

DECLARE
    v_product NUMBER;
BEGIN
    FOR i IN 11..40 LOOP
        v_product := 5 + FLOOR((i - 11) / 2);
        INSERT INTO review (review_id, product_id, user_id, content, rating, created_at)
        VALUES (i, v_product, 4, '필러리뷰' || i, 3.0, DATE '2025-12-01');
    END LOOP;
END;
/

INSERT INTO review_images (review_id, image_url) VALUES (1, 'reviews/fixture/review-1.jpg');
INSERT INTO review_comment (review_id, user_id, content) VALUES (4, 4, '스케줄댓글');

INSERT INTO point_history (user_id, amount, type, description)
VALUES (1, 10000, 'EARN', '시드 잔액');
INSERT INTO point_history (user_id, amount, type, description)
VALUES (2, 500, 'EARN', '시드 잔액');

DECLARE
    v_users NUMBER;
    v_products NUMBER;
    v_reviews NUMBER;
    v_items NUMBER;
    v_point NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_users FROM user_table;
    SELECT COUNT(*) INTO v_products FROM product;
    SELECT COUNT(*) INTO v_reviews FROM review;
    SELECT COUNT(*) INTO v_items FROM order_item;
    SELECT point INTO v_point FROM user_table WHERE user_id = 1;
    IF v_users <> 5 OR v_products <> 20 OR v_reviews <> 40 OR v_items <> 4 OR v_point <> 10000 THEN
        RAISE_APPLICATION_ERROR(-20001,
            'fixture users=' || v_users || ' products=' || v_products ||
            ' reviews=' || v_reviews || ' items=' || v_items || ' point=' || v_point);
    END IF;
END;
/

EXIT
