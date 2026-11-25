package com.orderly.config;

import com.orderly.auth.jwt.JwtRoleAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtRoleAuthenticationConverter jwtRoleAuthenticationConverter
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // 회원가입 및 로그인은 인증 없이 접근 가능
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/users",
                                "/api/v1/auth/login"
                        ).permitAll()

                        // 상품 조회는 누구나 가능
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/products",
                                "/api/v1/products/**"
                        ).permitAll()

                        // 상품 등록은 관리자만 가능
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/products"
                        ).hasRole("ADMIN")

                        // 상품 수정은 관리자만 가능
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/products/**"
                        ).hasRole("ADMIN")

                        // 상품 부분 수정도 관리자만 가능
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/products/**"
                        ).hasRole("ADMIN")

                        // 상품 삭제도 관리자만 가능
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/products/**"
                        ).hasRole("ADMIN")

                        // 그 외 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtRoleAuthenticationConverter
                                )
                        )
                )

                .build();
    }
}