package com.orderly.user;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final String MESSAGE =
            "비밀번호는 영문, 숫자, ASCII 특수문자를 각각 하나 이상 포함한 8~72자여야 하며 공백과 비ASCII 문자는 사용할 수 없습니다.";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9])[!-~]{8,72}$"
    );

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidPasswordException(MESSAGE);
        }
    }
}