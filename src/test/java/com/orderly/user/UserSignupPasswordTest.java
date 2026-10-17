package com.orderly.user;

import com.orderly.global.exception.GlobalExceptionHandler;
import com.orderly.user.dto.UserSignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserSignupPasswordTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(repository, encoder);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void invalidPasswordReturns400WithoutHashingOrSaving() throws Exception {
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"abcdefgh","name":"테스트"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.message").value(PasswordPolicy.MESSAGE));
        verifyNoInteractions(encoder);
        verify(repository, never()).save(any());
    }

    @Test
    void missingPasswordUsesSamePolicyError() throws Exception {
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","name":"테스트"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
        verifyNoInteractions(encoder);
        verify(repository, never()).save(any());
    }

    @Test
    void validPasswordIsPassedUnchangedToEncoderAndOnlyHashIsSaved() {
        when(encoder.encode("abcdef1!")).thenReturn("encoded-password");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.signup(new UserSignupRequest("test@example.com", "abcdef1!", "테스트"));
        verify(encoder).encode("abcdef1!");
        verify(repository).save(argThat(user -> "encoded-password".equals(user.getPassword())));
    }
}
