package sg.projects.stockflowengine.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import sg.projects.stockflowengine.model.AppUser;
import sg.projects.stockflowengine.repository.AppUserRepository;

/**
 * Startup configuration that seeds the default ADMIN user on first launch.
 *
 * <p>Solves the bootstrap problem — without an initial ADMIN account,
 * no users can be created since user management requires ADMIN role.
 *
 * <p>Runs on every startup via {@link CommandLineRunner} but only creates
 * the admin user if one does not already exist, making it idempotent
 * and safe to run on every application restart.
 *
 * <p>The seeded admin account:
 * <ul>
 *   <li>Username: admin</li>
 *   <li>Role: ADMIN</li>
 *   <li>Password: BCrypt hashed at runtime </li>
 * </ul>
 *
 * <p>In production, the admin password should be changed immediately after
 * first login and the default credentials should be rotated.
 */
@Configuration
public class DataSeeder {

    /**
     * Seeds the default ADMIN user if no admin account exists.
     *
     * <p>Password is encoded at runtime using {@link PasswordEncoder} (BCrypt).
     *
     * @param appUserRepository repository for user persistence and lookup
     * @param passwordEncoder   BCrypt encoder for secure password hashing
     * @return CommandLineRunner that executes admin seeding logic on startup
     */
    @Bean
    public CommandLineRunner seedAdmin(AppUserRepository appUserRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            if (appUserRepository.findByUserName("admin").isEmpty()) {
                AppUser admin = new AppUser();
                admin.setFullName("System Admin");
                admin.setDesignation("Operations Director");
                admin.setUserName("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setStatus("ACTIVE");
                appUserRepository.save(admin);
            }
        };
    }
}