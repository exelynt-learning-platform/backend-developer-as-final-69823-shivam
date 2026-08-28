package com.exelynt.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The password encoder lives in its own configuration class rather than inside
 * {@code SecurityConfig}. Declaring it alongside the security filter chain — which itself
 * depends on a UserDetailsService that depends on the encoder — is a well known way to
 * create a bean initialisation cycle. Keeping it separate avoids that entirely.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
