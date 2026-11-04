# Re-View 프로젝트 도메인 분석 리포트

Re-View 프로젝트의 전체 소스코드를 분석하여 도메인별로 분류한 결과입니다. 본 프로젝트는 **Spring Boot (MVC 패턴) + MyBatis (Oracle)** 기반의 백엔드와 **React** 기반의 프론트엔드로 구성되어 있으며, JPA 대신 MyBatis를 사용하여 SQL 쿼리를 Repository 인터페이스와 XML 매퍼 파일로 분리하여 관리하고 있습니다.

---

## 1. 회원 및 인증 도메인 (User & Auth)
회원의 회원가입, 로그인, 로그아웃, 비밀번호 재설정 및 세션 관리를 담당하는 핵심 도메인입니다. Spring Security와 HttpSession 기반의 세션 방식을 사용하여 사용자 권한(Role)을 관리합니다.

*   **백엔드 (Backend)**
    *   **Controller**: 
        *   [UserController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/user/UserController.java) (회원가입, 로그인, 비밀번호 재설정, 세션 정보 조회)
        *   [UserUtilController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/user/UserUtilController.java) (아이디 중복 확인, 임시 비밀번호 발송 등)
    *   **DTO**: 
        *   [LoginRequestDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/user/LoginRequestDTO.java)
        *   [PasswordUpdateDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/user/PasswordUpdateDTO.java)
        *   [TemPasswordDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/user/TemPasswordDTO.java)
        *   [UserInfoDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/user/UserInfoDTO.java)
        *   [UserSummaryDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/user/UserSummaryDTO.java)
    *   **Service**: 
        *   [UserService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/user/UserService.java)
    *   **Repository (Mapper Interface)**: 
        *   [UserMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/user/UserMapper.java)
    *   **MyBatis XML Mapper**: 
        *   [UserMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/user/UserMapper.xml)

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [RegisterPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/register/RegisterPage.jsx) (회원가입 화면)
        *   [RegisterComplete.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/register/RegisterComplete.jsx) (가입 완료 페이지)
        *   [UserProtectedRoute.jsx](file:///c:/Code/Re_View_Rematser/View/src/components/user/UserProtectedRoute.jsx) (일반 유저 접근 제한 Route)

---

## 2. 회원 상세 및 마이페이지 부가 기능 도메인 (User Profile & MyPage)
마이페이지에서 동작하는 회원 상세 정보 수정, 배송지 관리, 장바구니, 찜 목록, 포인트 히스토리 및 결제 수단 관리 등을 포함하는 도메인군입니다.

### 2-1. 회원 상세 정보 (User Info & Skin Type)
회원 프로필 조회/수정 및 피부 타입(바우만 피부 타입 테스트) 설문 결과를 처리합니다.
*   **백엔드**:
    *   [UserInfoController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/userinfo/UserInfoController.java)
    *   [UserInfoService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/userinfo/UserInfoService.java)
    *   [UserInfoMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/userinfo/UserInfoMapper.java)
    *   [UserInfoMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/userinfo/userinfos/UserInfoMapper.xml)
    *   **DTO**: [GetUserInfoDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/userinfo/userinfo/GetUserInfoDTO.java), [UpdateUserInfoDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/userinfo/userinfo/UpdateUserInfoDTO.java) 등
*   **프론트엔드**:
    *   [UserProfileEdit.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/profile/UserProfileEdit.jsx) (프로필 수정)
    *   [UserSkinTestPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/skin/UserSkinTestPage.jsx) / [MyPageBaumannSurvey.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/skin/MyPageBaumannSurvey.jsx) / [SurveyPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/survey/SurveyPage.jsx) (피부타입 설문/진단)

### 2-2. 배송지 관리 (Address)
사용자의 배송지 목록을 조회하고 추가, 수정, 삭제하는 기능을 담당합니다.
*   **백엔드**:
    *   [AddressController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/address/AddressController.java)
    *   [AddressService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/address/AddressService.java)
    *   [AddressMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/address/AddressMapper.java)
    *   [AddressMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/address/AddressMapper.xml)
    *   **DTO**: [AddressDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/address/AddressDTO.java)
*   **프론트엔드**:
    *   [UserAddressPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/address/UserAddressPage.jsx) (배송지 관리 목록)

### 2-3. 장바구니 (Cart)
구매 예정인 상품들을 보관하고 수량을 수정하는 기능을 제공합니다.
*   **백엔드**:
    *   [CartController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/userinfo/CartController.java)
    *   [CartService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/userinfo/other/CartService.java)
    *   [CartMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/userinfo/user_related/CartMapper.java)
    *   [CartMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/userinfo/other/cart/CartMapper.xml)
    *   **DTO**: [CartitemRequestDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/cart/CartitemRequestDTO.java), [CartitemResponseDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/cart/CartitemResponseDTO.java)
*   **프론트엔드**:
    *   [UserCartPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/cart/UserCartPage.jsx)

### 2-4. 찜 목록 (Wishlist)
관심 있는 상품을 보관하는 관심목록(위시리스트) 기능입니다.
*   **백엔드**:
    *   [WishlistController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/userinfo/WishlistController.java)
    *   [WishlistService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/userinfo/other/WishlistService.java)
    *   [WishlistMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/userinfo/user_related/WishlistMapper.java)
    *   [WishlistMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/userinfo/other/wishlist/WishlistMapper.xml)
    *   **DTO**: [WishlistDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/userinfo/user_related/wishlist/WishlistDTO.java) 등
*   **프론트엔드**:
    *   [UserWishPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/wish/UserWishPage.jsx)

### 2-5. 포인트 내역 및 결제 수단 (Point & Payment Method)
유저의 포인트 적립/차감 히스토리 및 간편 결제 수단 등록 등을 담당합니다.
*   **백엔드**:
    *   **Point**: [PointController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/userinfo/PointController.java), [PointService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/userinfo/other/PointService.java), [PointMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/userinfo/user_related/PointMapper.java), [PointMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/userinfo/other/point/PointMapper.xml)
    *   **Payment**: [Payment_MethodController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/userinfo/Payment_MethodController.java), [Payment_MethodService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/userinfo/other/Payment_MethodService.java), [Payment_MethodMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/userinfo/user_related/Payment_MethodMapper.java), [Payment_MethodMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/userinfo/other/payment/Payment_MethodMapper.xml)
*   **프론트엔드**:
    *   [UserPointHistoryPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/skin/UserSkinTestPage.jsx) (포인트 히스토리 조회)

---

## 3. 상품 도메인 (Product)
쇼핑몰 상품 목록 조회, 특정 상품 상세 정보 조회, 재고 관리 등을 담당합니다.
*   **백엔드 (Backend)**
    *   **Controller**: 
        *   [ProductController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/product/ProductController.java) (상품 목록 조회, 상세 조회)
    *   **DTO**: 
        *   [ProductDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/product/ProductDTO.java)
        *   [ProductDetailDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/product/ProductDetailDTO.java)
        *   [ProductStockDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/product/ProductStockDTO.java)
    *   **Service**: 
        *   [ProductService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/product/ProductService.java)
        *   [ProductDetailService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/product/ProductDetailService.java)
    *   **Repository (Mapper Interface)**: 
        *   [ProductMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/product/ProductMapper.java)
    *   **MyBatis XML Mapper**: 
        *   [ProductMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/product/ProductMapper.xml)

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [ProductPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/products/ProductPage.jsx) (상품 리스트 페이지)
        *   [ProductDetailPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/productDetail/ProductDetailPage.jsx) (상품 상세 정보 페이지)
        *   [ProductCard.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/products/components/ProductCard.jsx)

---

## 4. 주문 및 결제 도메인 (Order)
상품 장바구니 또는 상세페이지에서 결제 페이지로 이어지는 체크아웃, 주문 생성 및 주문 목록/상세 조회 기능을 담당합니다.
*   **백엔드 (Backend)**
    *   **Controller**: 
        *   [OrderController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/orders/OrderController.java) (주문 생성, 주문 상세 확인)
        *   [OrderListController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/orders/OrderListController.java) (주문 이력 리스트 조회)
    *   **DTO**: 
        *   [OrderCreateDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/orders/OrderCreateDTO.java)
        *   [OrderSaveDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/orders/OrderSaveDTO.java)
        *   [OrderDetailResponseDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/orders/OrderDetailResponseDTO.java)
        *   [OrderCheckoutResponse.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/orders/OrderCheckoutResponse.java)
    *   **Service**: 
        *   [OrderService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/order/OrderService.java)
        *   [OrderListService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/order/OrderListService.java)
        *   [OrderPreviewService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/order/OrderPreviewService.java)
    *   **Repository (Mapper Interface)**: 
        *   [OrderMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/Orders/OrderMapper.java)
        *   [OrderListMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/Orders/OrderListMapper.java)
        *   [OrderItemIdMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/OrderItemIdMapper.java)
    *   **MyBatis XML Mapper**: 
        *   [OrdersMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/order/OrdersMapper.xml)
        *   [OrderListMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/order/OrderListMapper.xml)
        *   [OrderItemIdMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/order/OrderItemIdMapper.xml)

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [OrderPaymentPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/order/OrderPaymentPage.jsx) (주문 결제 화면)
        *   [OrderCompletePage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/order/OrderCompletePage.jsx) (주문 완료 완료 페이지)
        *   [UserDeliveryPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/delivery/UserDeliveryPage.jsx) (배송/주문 목록)
        *   [UserOrderDetailPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/user/delivery/UserOrderDetailPage.jsx) (주문 상세 보기)

---

## 5. 리뷰 및 커뮤니티 도메인 (Review & Community)
핵심 비즈니스 로직 중 하나로, 유저들이 구매한 상품에 대해 텍스트와 이미지가 포함된 리뷰를 작성하고 다른 사람의 리뷰에 '좋아요', '댓글', '신고'를 남길 수 있는 커뮤니티 기능을 관리합니다.
리뷰 작성 시 리워드 포인트가 적립되며, 리뷰 삭제 시 해당 포인트가 회수되는 페널티 로직 등이 구현되어 있습니다.

*   **백엔드 (Backend)**
    *   **Controller**: 
        *   [ProductReviewController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/review/ProductReviewController.java) (특정 상품의 리뷰 CRUD 및 작성 권한 확인)
        *   [ReviewActionController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/review/ReviewActionController.java) (리뷰 좋아요, 댓글 달기, 리뷰 신고 등 커뮤니티 액션)
        *   [ReviewController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/review/ReviewController.java) (전체 리뷰 목록 및 베스트 리뷰 조회)
    *   **DTO**: 
        *   [ProductReviewDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/review/ProductReviewDTO.java)
        *   [ReviewDetailResponseDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/review/ReviewDetailResponseDTO.java)
        *   [CreateReviewRequestDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/review/review_create/CreateReviewRequestDTO.java)
        *   [CommentRequestDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/review/community/CommentRequestDTO.java)
    *   **Service**: 
        *   [ProductReviewService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/review/ProductReviewService.java)
        *   [ReviewActionService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/review/ReviewActionService.java)
        *   [ReviewService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/review/ReviewService.java)
    *   **Repository (Mapper Interface)**: 
        *   [ProductReviewMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/review/ProductReviewMapper.java)
        *   [ReviewActionMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/review/ReviewActionMapper.java)
        *   [ReviewMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/review/ReviewMapper.java)
    *   **MyBatis XML Mapper**: 
        *   [ProductreviewMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/review/ProductreviewMapper.xml)
        *   [ReviewActionMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/review/ReviewActionMapper.xml)
        *   [ReviewMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/review/ReviewMapper.xml)

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [ReviewPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/reviews/ReviewPage.jsx) (리뷰 피드 목록)
        *   [ReviewDetailPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/reviewDetail/ReviewDetailPage.jsx) (리뷰 상세 페이지 - 댓글/대댓글 확인 가능)
        *   [ReviewWrite.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/review/ReviewWrite.jsx) (리뷰 쓰기/수정)
        *   [ReviewCommentList.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/reviewDetail/components/ReviewCommentList.jsx)

---

## 6. 스킨타입 기반 상품 추천 도메인 (Recommendations)
사용자의 바우만 피부 타입(Baumann Skin Type) 진단 결과에 맞추어 맞춤 상품을 추천하거나, 관리자가 선정한 픽(Admin Pick) 리뷰/상품을 추천 목록으로 보여주는 데이터 매칭 도메인입니다.
*   **백엔드 (Backend)**
    *   **Controller**: 
        *   [RecommendationsController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/recommendations/RecommendationsController.java) (피부 타입 맞춤 추천 상품 목록 조회)
    *   **DTO**: 
        *   [RecommendationDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/recommendations/RecommendationDTO.java)
        *   [RecommendProductDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/recommendations/RecommendProductDTO.java)
        *   [RecommendationResponseDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/recommendations/RecommendationResponseDTO.java)
    *   **Service**: 
        *   [RecommendationsService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/recommendations/RecommendationsService.java)
    *   **Repository (Mapper Interface)**: 
        *   [RecommendationsMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/recommendations/RecommendationsMapper.java)
    *   **MyBatis XML Mapper**: 
        *   [RecommendationsMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/recommendations/RecommendationsMapper.xml)

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [AdminPickReview.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/main/components/adminPickReview/AdminPickReview.jsx) (관리자 픽 리뷰 컴포넌트)
        *   [BaumannProduct.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/main/components/BaumannProduct/BaumannProduct.jsx) (스킨타입 맞춤 상품 메인 노출 컴포넌트)

---

## 7. 고객 문의 도메인 (QnA)
상품 페이지나 마이페이지에서 상품 및 주문에 대해 문의글을 남기고 답변을 관리하는 Q&A 도메인입니다.
*   **백엔드 (Backend)**
    *   **Controller**: 
        *   [QnaController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/qna/QnaController.java) (QnA 작성 및 조회)
    *   **DTO**: 
        *   [QnaDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/qna/QnaDTO.java)
        *   [QnAListDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/qna/QnAListDTO.java)
        *   [QnaListResponseDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/qna/QnaListResponseDTO.java)
    *   **Service**: 
        *   [QnaService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/qna/QnaService.java)
    *   **Repository (Mapper Interface)**: 
        *   [QnaMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/qna/QnaMapper.java)
    *   **MyBatis XML Mapper**: 
        *   [QnaMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/qna/QnaMapper.xml)

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [QnaPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/qna/QnaPage.jsx) (QnA 목록/작성)
        *   [QnaSection.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/productDetail/components/QnaSection/QnaSection.jsx) (상품 상세페이지 하단 QnA 탭)

---

## 8. 신고 도메인 (Report)
가이드라인을 위배하는 리뷰 혹은 댓글에 대해 사용자가 신고를 접수하고 상태를 변경하는 시스템입니다.
*   **백엔드 (Backend)**
    *   **Controller**: 관리자 컨트롤러인 [AdminReportController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/admin/AdminReportController.java)에서 신고 상태 변경 API를 핸들링합니다.
    *   **DTO**: 
        *   [ReportResponseDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/report/ReportResponseDTO.java)
        *   [ReportStatusDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/report/ReportStatusDTO.java)
    *   **Service**: 
        *   [ReportService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/report/ReportService.java)
    *   **Repository (Mapper Interface)**: 
        *   [ReportMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/report/ReportMapper.java)
    *   **MyBatis XML Mapper**: 
        *   [reportMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/report/reportMapper.xml)

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [ReviewReportModal.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/reviewDetail/components/ReviewReportModal.jsx) (신고 팝업창)

---

## 9. 통합 검색 도메인 (Search)
헤더의 전역 검색창을 통한 상품/리뷰 동시 검색, 마이페이지의 리뷰 검색, 상품 상세 내 리뷰 필터 및 정렬 등 다양하게 데이터 조회를 서포트합니다.
*   **백엔드 (Backend)**
    *   **Controller**: 
        *   [HeaderSearchController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/search/HeaderSearchController.java) (헤더 글로벌 검색)
        *   [MyPageReviewController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/search/MyPageReviewController.java) (마이페이지 리뷰 검색)
        *   [ProductReview_SearchController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/search/ProductReview_SearchController.java) (상품 상세페이지의 리뷰 검색)
    *   **DTO**: 
        *   [HeaderSearchDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/search/header/HeaderSearchDTO.java)
        *   [MyPageReviewDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/search/MyPageProductPage/MyPage/MyPageReviewDTO.java)
        *   [ProductReview_SearchDTO.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/dto/search/MyPageProductPage/Product/ProductReview_SearchDTO.java)
    *   **Service**: 
        *   [HeaderSearchService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/search/HeaderSearchService.java)
        *   [MyPageReviewService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/search/MyPageReviewService.java)
        *   [ProductReview_SearchService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/search/ProductReview_SearchService.java)
    *   **Repository (Mapper Interface)**: 
        *   [HeaderSearchProductMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/search/header/HeaderSearchProductMapper.java)
        *   [HeaderSearchReviewMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/search/header/HeaderSearchReviewMapper.java)
        *   [MyPageReviewMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/search/MyPageProductPage/MyPageReviewMapper.java)
        *   [ProductReview_SearchMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/search/MyPageProductPage/ProductReview_SearchMapper.java)
    *   **MyBatis XML Mapper**: 
        *   XML 매퍼들이 각각 `mapper/search/header/` 및 `mapper/search/pages/` 하위에 위치하여 복잡한 Dynamic SQL 조건(정렬 조건, 스킨타입 필터 등)을 처리합니다.

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [SearchPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/search/SearchPage.jsx) (통합 검색 결과 및 상세 필터 화면)

---

## 10. 이미지 및 파일 스토리지 (Image Storage - Minio)
Minio 오브젝트 스토리지를 연동하여 리뷰 사진, 상품 썸네일 등의 이미지를 업로드하고 스토리지 경로(URL)를 관리하는 유틸리티 겸 파일 도메인입니다.
*   **백엔드 (Backend)**
    *   **Controller / Service**: 
        *   [ImageUploadController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/image/ImageUploadController.java) / [ImageController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/image/ImageController.java)
        *   [ImageService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/image/ImageService.java) (Minio 클라이언트를 이용한 파일 Put/Delete)
        *   [MinioConfig.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/image/minio/MinioConfig.java) (스토리지 연결 빈 설정)
    *   **Repository (Mapper Interface) / XML**: 
        *   [ImageMapper.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/repository/ImageMapper.java)
        *   [ImageMapper.xml](file:///c:/Code/Re_View_Rematser/src/main/resources/mapper/ImageMapper.xml) (DB 상의 이미지 URL 매핑 테이블 관리)

---

## 11. 어드민/백오피스 도메인 (Admin Dashboard)
웹사이트 운영진 전용 도메인으로, 유저 밴(Ban)/밴 해제, 상품 CRUD, 주문 상태 변경, QnA 답변 달기, 신고된 리뷰 승인/반려 등을 종합 제어합니다.
*   **백엔드 (Backend)**
    *   **Controller**:
        *   [AdminOrderController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/admin/AdminOrderController.java)
        *   [AdminProductController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/admin/AdminProductController.java)
        *   [AdminQnAController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/admin/AdminQnAController.java)
        *   [AdminUserController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/admin/AdminUserController.java)
        *   [AdminReviewController.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/controller/admin/AdminReviewController.java)
    *   **Service**: 
        *   [AdminUserService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/admin/AdminUserService.java), [AdminProductService.java](file:///c:/Code/Re_View_Rematser/src/main/java/com/review/shop/service/admin/AdminProductService.java) 등
    *   **Repository / XML**: 
        *   `repository/admin/` 하위 Mapper 인터페이스 및 `resources/mapper/admin/` 하위 매퍼 XML 파일들을 통해 관리자 전용 일괄 조인 쿼리 및 상태 Update 구문을 처리합니다.

*   **프론트엔드 (Frontend)**
    *   **Pages / Components**:
        *   [AdminLayout.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/admin/AdminLayout.jsx) (어드민 메인 대시보드 사이드바/레이아웃)
        *   [AdminUserPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/admin/AdminUserPage.jsx) (회원 제재 관리)
        *   [AdminProductPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/admin/AdminProductPage.jsx) / [AdminProductNew.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/admin/AdminProductNew.jsx) (상품 등록/관리)
        *   [AdminReviewReportPage.jsx](file:///c:/Code/Re_View_Rematser/View/src/pages/mypage/admin/AdminReviewReportPage.jsx) (신고 접수 내역 모니터링 및 처리)
        *   [ProtectedRoute.jsx](file:///c:/Code/Re_View_Rematser/View/src/components/admin/ProtectedRoute.jsx) (어드민 권한 체크 가드)

---

## 요약 및 설계 구조적 특징

1.  **순수 MyBatis 활용**: JPA를 배제하고 MyBatis Mapper를 1:1 대응하여 사용하므로, 복잡하고 자유도 높은 Oracle SQL(페이징, 추천 가중치 계산, 카테고리 필터 등)이 XML 파일에 분리 기술되어 있어 DB 튜닝 및 쿼리 수정이 용이합니다.
2.  **DTO 세분화**: 각 화면단 and API의 쓰임새에 맞게 DTO 클래스가 상세하게 세분화되어 있어 오버헤드를 낮추고 가독성을 올렸습니다 (예: `ProductDetailWithThumbnailDTO`, `RecommendationResponseWrapper`).
3.  **바우만 스킨 테스트와의 결합**: `com.review.shop.dto.recommendations` 및 `controller/recommendations/`를 통해 화장품 리뷰 쇼핑몰만의 핵심 도메인인 **"유저 피부 맞춤 추천"**이 프론트엔드의 설문 컴포넌트(`survey/`)와 밀접하게 상호작용합니다.
