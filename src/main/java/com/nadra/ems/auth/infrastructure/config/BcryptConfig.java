package com.nadra.ems.auth.infrastructure.config;

import com.nadra.ems.auth.domain.port.out.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuration for password encoding.
 * <p>
 * Exposes a {@link BCryptPasswordEncoder} bean and a {@link PasswordEncoderPort} adapter
 * that bridges Spring Security's encoder to the domain port.
 */
@Configuration
public class BcryptConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public PasswordEncoderPort passwordEncoderPort(BCryptPasswordEncoder encoder) {
        return new PasswordEncoderPort() {
            @Override
            public String encode(String rawPassword) {
                return encoder.encode(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return encoder.matches(rawPassword, encodedPassword);
            }
        };
    }
}
