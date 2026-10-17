package com.orderly.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"abcdef1!", "ABCDEF1!", "123456!a"})
    void acceptsEitherLetterCaseAndEightCharacters(String password) {
        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }

    @Test
    void acceptsSeventyTwoCharactersAndRejectsSeventyThree() {
        assertThatCode(() -> PasswordPolicy.validate("a1!" + "a".repeat(69)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PasswordPolicy.validate("a1!" + "a".repeat(70)))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void acceptsEveryAsciiPunctuationCharacter() {
        for (char c = '!'; c <= '~'; c++) {
            if (!Character.isLetterOrDigit(c)) {
                String password = "abcdef1" + c;
                assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
            }
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abcde1!", "abcdefgh", "abcdef12", "1234567!",
            "        ", "abcdef1! ", "abcdef1!\t", "abcdef1!\n", "abcdef1!\r\n",
            "abcdef1!한", "abcdef1!😀", "abcdef1!é", "abcdef1!\u00a0"})
    void rejectsInvalidPasswords(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage(PasswordPolicy.MESSAGE);
    }

    @Test
    void rejectsEveryAsciiControlCharacterAndSpace() {
        for (char c = 0; c <= 32; c++) {
            String password = "abcdef1!" + c;
            assertThatThrownBy(() -> PasswordPolicy.validate(password))
                    .isInstanceOf(InvalidPasswordException.class);
        }
        assertThatThrownBy(() -> PasswordPolicy.validate("abcdef1!" + (char) 127))
                .isInstanceOf(InvalidPasswordException.class);
    }
}
