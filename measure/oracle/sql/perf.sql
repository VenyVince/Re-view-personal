WHENEVER SQLERROR EXIT FAILURE

INSERT INTO baumann (baumann_id, type, first, second, third, fourth)
VALUES (1, 'DSPW', 'D', 'S', 'P', 'W');

BEGIN
    FOR i IN 1..20 LOOP
        INSERT INTO user_table (id, password, name, email, nickname, phone_number, baumann_id, role, point)
        VALUES (
            'perf' || i,
            'measure-not-a-login',
            '성능' || i,
            'perf' || i || '@measure.local',
            '성능유저' || i,
            '019' || LPAD(i, 8, '0'),
            1,
            'USER',
            0
        );
    END LOOP;

    FOR i IN 1..200 LOOP
        INSERT INTO product (
            prd_name, prd_brand, ingredient, description, price, category,
            stock, rating, review_count, baumann_id, is_sold_out, created_at
        ) VALUES (
            '성능상품' || i,
            'PerfBrand',
            '성능성분',
            '성능설명',
            1000 + i,
            'skincare',
            10,
            4.0,
            10,
            1,
            '0',
            DATE '2026-01-01'
        );
    END LOOP;

    -- Identity on this schema starts at 1000. One session inserts users then products with no gaps.
    FOR i IN 1..2000 LOOP
        INSERT INTO review (product_id, user_id, content, rating, created_at, like_count)
        VALUES (
            1000 + MOD(i - 1, 200),
            1000 + MOD(i - 1, 20),
            '성능리뷰' || i,
            4.0,
            DATE '2026-01-01',
            0
        );
    END LOOP;
END;
/

DECLARE
    v_products NUMBER;
    v_reviews NUMBER;
    v_users NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_products FROM product;
    SELECT COUNT(*) INTO v_reviews FROM review;
    SELECT COUNT(*) INTO v_users FROM user_table;
    IF v_products <> 200 OR v_reviews <> 2000 OR v_users <> 20 THEN
        RAISE_APPLICATION_ERROR(-20002,
            'perf users=' || v_users || ' products=' || v_products || ' reviews=' || v_reviews);
    END IF;
END;
/

EXIT
