package sg.projects.stockflowengine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class providing the BCrypt password encoder bean.
 *
 * <p>Extracted into a separate class to resolve the circular dependency
 * that would occur if {@link PasswordEncoder} were defined in
 * {@link SecurityConfig}:
 * <pre>
 * SecurityConfig → JwtAuthFilter → AppUserService → PasswordEncoder (in SecurityConfig) ← circular!
 * </pre>
 *
 * <p>By moving {@link PasswordEncoder} to its own configuration class,
 * the dependency chain becomes linear:
 * <pre>
 * AppUserService → PasswordEncoder (from PasswordConfig) ✅
 * SecurityConfig → JwtAuthFilter → AppUserService ✅
 * </pre>
 *
 * <p>BCrypt characteristics:
 * <ul>
 *   <li>One-way hashing — passwords can never be decrypted</li>
 *   <li>Salted — same password produces different hash each time</li>
 *   <li>Adaptive — work factor can be increased as hardware improves</li>
 *   <li>Industry standard for password storage</li>
 * </ul>
 *
 * @see SecurityConfig
 * @see sg.projects.stockflowengine.service.AppUserService
 */
@Configuration
public class PasswordConfig {

    /**
     * Creates a BCryptPasswordEncoder bean for secure password hashing.
     *
     * <p>Used by {@link sg.projects.stockflowengine.service.AppUserService}
     * to hash passwords on user creation and update, and by
     * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}
     * to verify passwords during login.
     *
     * @return BCryptPasswordEncoder instance with default strength (10 rounds)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}