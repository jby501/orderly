package com.orderly.user;

public final class PasswordPolicy {

    public static final String MESSAGE =
            "비밀번호는 영문, 숫자, ASCII 특수문자를 각각 하나 이상 포함한 8~72자여야 하며 공백과 비ASCII 문자는 사용할 수 없습니다.";

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new InvalidPasswordException(MESSAGE);
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            // 출력 가능한 ASCII만 허용하므로 문자 수와 UTF-8 바이트 수가 같다.
            if (character < '!' || character > '~') {
                throw new InvalidPasswordException(MESSAGE);
            }
            if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')) {
                hasLetter = true;
            } else if (character >= '0' && character <= '9') {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        if (!hasLetter || !hasDigit || !hasSpecial) {
            throw new InvalidPasswordException(MESSAGE);
        }
    }
}
