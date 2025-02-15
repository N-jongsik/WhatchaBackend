package com.example.whatcha.domain.user.constant;

import lombok.Getter;

@Getter
public enum UserType {
    ROLE_USER("일반회원"),
    ROLE_ADMIN("관리자회원");

    private String value;

    UserType(String value) {
        this.value = value;
    }

    public static UserType getUserType(String value) {
        if (value.contains("관리자")) {
            return ROLE_ADMIN;
        }else {
            return ROLE_USER;
        }
    }
}
