package com.review.shop.exception;

// 밴 처리된 사용자가 로그인을 시도할 때 발생
public class BannedUserException extends RuntimeException {
    public BannedUserException(String message) {
        super(message);
    }
}
