package com.beetloop.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Stateless JWT resource server.
 * ROLE_VENDOR owns /vendor/**; ROLE_QC_REVIEWER owns /qc/**; /masters/** needs only a session.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ROLE_VENDOR = "VENDOR";
    public static final String ROLE_QC_REVIEWER = "QC_REVIEWER";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, TenantContextFilter tenantContextFilter)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/openapi/**", "/swagger/**", "/swagger-ui/**",
                                "/v3/api-docs/**", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/qc/**").hasRole(ROLE_QC_REVIEWER)
                        .requestMatchers("/vendor/**").hasRole(ROLE_VENDOR)
                        .requestMatchers("/masters/**", "/marketplace/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .addFilterAfter(tenantContextFilter, BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * `local` profile only: HS256 so the API is runnable without an IdP.
     * When beetloop.security.local-hs256-secret is absent, Spring Boot builds the decoder from
     * spring.security.oauth2.resourceserver.jwt.jwk-set-uri instead.
     */
    @Bean
    @ConditionalOnProperty(name = "beetloop.security.local-hs256-secret")
    JwtDecoder localJwtDecoder(@Value("${beetloop.security.local-hs256-secret}") String secret) {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build();
    }
}
