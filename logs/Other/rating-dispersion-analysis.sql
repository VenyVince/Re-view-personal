-- 리뷰 추천 별점 유사도: 표준편차 적용 최소 리뷰 수(N) / 구간 설계용 분석 쿼리
-- 임의로 N을 정하지 않고, 아래 결과로 안정성을 본 뒤 사용자가 결정한다.
-- Oracle 기준. 유효 별점 = deleted_at IS NULL AND rating BETWEEN 1 AND 5

-- 1) 상품별 유효 리뷰 집계
WITH product_stats AS (
    SELECT
        r.product_id,
        COUNT(*) AS valid_review_count,
        AVG(r.rating) AS valid_avg_rating,
        STDDEV_SAMP(r.rating) AS valid_stddev_rating,
        MIN(r.rating) AS min_rating,
        MAX(r.rating) AS max_rating
    FROM review r
    WHERE r.deleted_at IS NULL
      AND r.rating IS NOT NULL
      AND r.rating >= 1
      AND r.rating <= 5
    GROUP BY r.product_id
)
SELECT *
FROM product_stats
ORDER BY valid_review_count DESC, product_id;

-- 2) 리뷰 개수 버킷별 상품 수 / 표준편차 요약
--    (버킷 경계는 탐색용. N 후보를 고르기 위한 관측이며 점수 정책이 아님)
WITH product_stats AS (
    SELECT
        r.product_id,
        COUNT(*) AS valid_review_count,
        STDDEV_SAMP(r.rating) AS valid_stddev_rating
    FROM review r
    WHERE r.deleted_at IS NULL
      AND r.rating IS NOT NULL
      AND r.rating >= 1
      AND r.rating <= 5
    GROUP BY r.product_id
),
bucketed AS (
    SELECT
        CASE
            WHEN valid_review_count = 1 THEN '01'
            WHEN valid_review_count = 2 THEN '02'
            WHEN valid_review_count BETWEEN 3 AND 4 THEN '03-04'
            WHEN valid_review_count BETWEEN 5 AND 9 THEN '05-09'
            WHEN valid_review_count BETWEEN 10 AND 19 THEN '10-19'
            WHEN valid_review_count BETWEEN 20 AND 49 THEN '20-49'
            ELSE '50+'
        END AS review_count_bucket,
        valid_review_count,
        valid_stddev_rating
    FROM product_stats
)
SELECT
    review_count_bucket,
    COUNT(*) AS product_count,
    ROUND(AVG(valid_stddev_rating), 4) AS avg_stddev,
    ROUND(MEDIAN(valid_stddev_rating), 4) AS median_stddev,
    ROUND(MIN(valid_stddev_rating), 4) AS min_stddev,
    ROUND(MAX(valid_stddev_rating), 4) AS max_stddev,
    ROUND(STDDEV_SAMP(valid_stddev_rating), 4) AS stddev_of_stddev
FROM bucketed
GROUP BY review_count_bucket
ORDER BY review_count_bucket;

-- 3) N 후보 결정 가이드
-- - 버킷별 median_stddev / stddev_of_stddev가 급격히 안정되는 지점을 N 후보로 둔다.
-- - 표본표준편차(STDDEV_SAMP)는 n=1에서 NULL, n=2에서 변동이 클 수 있다.
-- - 상품 수(product_count)가 너무 적은 버킷의 N은 채택하지 않는다.

-- 4) 표준편차 구간 후보(점수표는 미확정: 경계만 관측)
-- 예: 사분위수 기반
WITH product_stats AS (
    SELECT
        STDDEV_SAMP(r.rating) AS valid_stddev_rating
    FROM review r
    WHERE r.deleted_at IS NULL
      AND r.rating IS NOT NULL
      AND r.rating >= 1
      AND r.rating <= 5
    GROUP BY r.product_id
    HAVING COUNT(*) >= 2   -- N 확정 전 탐색용. 확정 N으로 교체
)
SELECT
    ROUND(PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY valid_stddev_rating), 4) AS p25,
    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY valid_stddev_rating), 4) AS p50,
    ROUND(PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY valid_stddev_rating), 4) AS p75
FROM product_stats;
