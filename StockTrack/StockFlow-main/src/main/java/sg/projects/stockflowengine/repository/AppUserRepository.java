package sg.projects.stockflowengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.projects.stockflowengine.model.AppUser;

import java.util.Optional;

/**
 * Repository interface for {@link AppUser} persistence operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations.
 * Spring Data JPA automatically generates the implementation at runtime.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link sg.projects.stockflowengine.service.AppUserService} — user management</li>
 *   <li>{@link sg.projects.stockflowengine.config.DataSeeder} — admin bootstrapping</li>
 * </ul>
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by their unique username.
     *
     * <p>Used by Spring Security's {@code loadUserByUsername()} during
     * authentication to retrieve the user from the database for
     * credential validation.
     *
     * <p>Spring Data JPA automatically generates:
     * <pre>
     * SELECT * FROM user_master WHERE user_name = ?
     * </pre>
     *
     * @param userName the username to search for
     * @return Optional containing the user if found, empty if not found
     */
    Optional<AppUser> findByUserName(String userName);
}