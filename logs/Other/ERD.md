# Re_View ERD Summary

이 문서는 현재 백엔드 mapper에서 확인되는 테이블 사용 흐름을 기준으로 핵심 ERD를 정리한다. 원본 문서 저장소의 Oracle DDL은 `"USER"`, `"ORDER"` 같은 레거시 테이블명을 포함하지만, 현재 애플리케이션 mapper는 `USER_TABLE`, `ORDERS`를 기준으로 동작한다.

## 전체 ERD
< 이미지 삽입 예정 >


## 주요 테이블

| 테이블 | 역할 | 주요 관계 |
| --- | --- | --- |
| `USER_TABLE` | 회원, 관리자, 포인트, 피부 타입 정보를 보관 | `BAUMANN`, `ADDRESS`, `ORDERS`, `REVIEW`, `POINT_HISTORY` |
| `BAUMANN` | Baumann 피부 타입 코드와 4개 속성 보관 | `USER_TABLE`, `PRODUCT` 추천 기준 |
| `PRODUCT` | 상품 기본 정보, 가격, 재고, 평점, 판매 상태 보관 | `PRODUCT_IMAGE`, `ORDER_ITEM`, `REVIEW`, `QNA` |
| `PRODUCT_IMAGE` | 상품 이미지 object key 또는 URL 보관 | `PRODUCT` |
| `ORDERS` | 주문 헤더, 총액, 상태, 배송 정보 보관 | `USER_TABLE`, `ADDRESS`, `PAYMENT_METHODS`, `ORDER_ITEM` |
| `ORDER_ITEM` | 주문 상품 스냅샷과 수량 보관 | `ORDERS`, `PRODUCT`, `REVIEW` |
| `REVIEW` | 상품 리뷰, 평점, 좋아요/싫어요 수, 선정 상태 보관 | `USER_TABLE`, `PRODUCT`, `ORDER_ITEM` |
| `REVIEW_IMAGES` | 리뷰 이미지 object key 또는 URL 보관 | `REVIEW` |
| `REVIEW_COMMENT` | 리뷰 댓글 보관 | `REVIEW`, `USER_TABLE` |
| `REVIEW_LIKE` | 리뷰 좋아요/싫어요 반응 보관 | `REVIEW`, `USER_TABLE` |
| `REVIEW_REPORT` | 리뷰 신고와 처리 상태 보관 | `REVIEW`, 신고자 `USER_TABLE` |
| `POINT_HISTORY` | 포인트 적립/사용/회수 이력 보관 | `USER_TABLE`, `ORDERS`, `REVIEW` |
| `ADDRESS` | 배송지 보관 | `USER_TABLE`, `ORDERS` |
| `PAYMENT_METHODS` | 결제수단 보관 | `USER_TABLE`, `ORDERS` |
| `CART_ITEMS` | 장바구니 상품 보관 | `USER_TABLE`, `PRODUCT` |
| `WISH_ITEM` | 위시리스트 상품 보관 | `USER_TABLE`, `PRODUCT` |
| `QNA` | 상품 QnA와 관리자 답변 보관 | `USER_TABLE`, `PRODUCT` |
| `BAN_LIST` | 밴 사용자와 사유 보관 | `USER_TABLE` |
| `BANNER_IMAGE` | 메인 배너 이미지 보관 | 메인 화면 조회 |

## 현재 코드 기준 주의사항

- 현재 mapper는 `USER_TABLE`, `ORDERS`를 사용한다. 원본 DDL의 `"USER"`, `"ORDER"` 이름을 그대로 적용하면 mapper와 충돌한다.
- `PRODUCT`는 현재 추천 mapper에서 `baumann_id`를 직접 참조한다. 원본 DDL의 `product_baumann` 중간 테이블과 실제 코드 기준이 다를 수 있다.
- `POINT_HISTORY`는 주문 포인트뿐 아니라 리뷰 작성, 리뷰 삭제 회수, 베스트 리뷰, 관리자 선정 리뷰 보상에도 사용된다.
- 리뷰 이미지는 DB에 저장되는 영속 값과 presigned URL 응답 값이 다르다. DB에는 object key 또는 저장 URL을 보관하고, 응답 시 MinIO presigned URL로 변환한다.
- ERD는 포트폴리오 설명용 핵심 관계도이며, 실제 운영 DDL은 현재 mapper와 한 번 더 대조한 뒤 확정해야 한다.
