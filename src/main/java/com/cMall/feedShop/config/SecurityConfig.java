package com.cMall.feedShop.config;

import com.cMall.feedShop.user.infrastructure.security.JwtAuthenticationFilter;
import com.cMall.feedShop.user.infrastructure.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("prod")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 예외 처리 (인증되지 않은 사용자가 보호된 리소스에 접근 시 처리)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        // 인증되지 않은 사용자가 접근 시 HTTP 401 Unauthorized 응답을 반환하도록 설정
                        // React는 이 401 응답을 받아서 자체적으로 로그인 페이지로 리다이렉트하거나 메시지를 표시합니다.
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                    .requestMatchers(
                      "/api/auth/**",
                      "/public/**",
                      "/swagger-ui/**",
                      "/v3/api-docs/**",
                      "/swagger-resources/**",
                      "/api/products/**",
                      "/api/events/all",
                      "/api/events/search",
                      "/api/events/{eventId}",
                      "/api/reviews/products/**",
                      "/api/reviews/{reviewId}"
                    ).permitAll()
                    .requestMatchers("/api/users/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/seller/**").hasRole("SELLER")
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.asList(
                "https://feedshop-frontend.vercel.app",
                "https://www.feedshop.store"
        ));

        // 2. 반드시 필요한 HTTP 메서드만 허용
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. 와일드카드(*) 대신 필요한 헤더만 명시적으로 나열하여 보안 강화
        //    (Authorization, Content-Type, Accept 등)
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));

        // 4. 인증 정보(쿠키, 인증 헤더 등)를 포함한 요청을 허용
        config.setAllowCredentials(true);

        // 5. preflight 요청의 결과를 캐시할 시간(초) 설정
        //    잦은 preflight 요청으로 인한 성능 저하 방지
        config.setMaxAge(3600L); // 1시간 캐시

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
