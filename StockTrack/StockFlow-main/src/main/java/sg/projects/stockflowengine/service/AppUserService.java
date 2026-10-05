package sg.projects.stockflowengine.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sg.projects.stockflowengine.dto.AppUserResponseDto;
import sg.projects.stockflowengine.model.AppUser;
import sg.projects.stockflowengine.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service responsible for user management and Spring Security integration.
 *
 * <p>Implements {@link UserDetailsService} to bridge Spring Security with
 * the application's user database, allowing the security framework to
 * load and authenticate users from {@code user_master} table.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>User CRUD operations with BCrypt password hashing</li>
 *   <li>Role validation on user creation</li>
 *   <li>Soft delete (INACTIVE status) instead of hard delete</li>
 *   <li>Returns {@link AppUserResponseDto} — password never exposed in responses</li>
 * </ul>
 *
 * @see AppUserRepository
 * @see AppUserResponseDto
 * @see sg.projects.stockflowengine.security.SecurityConfig
 */
@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs AppUserService with repository and password encoder.
     *
     * @param appUserRepository repository for user persistence operations
     * @param passwordEncoder   BCrypt encoder from {@link sg.projects.stockflowengine.security.PasswordConfig}
     */
    public AppUserService(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Converts an AppUser entity to a response DTO, excluding the password field.
     *
     * @param user the AppUser entity to convert
     * @return AppUserResponseDto with all fields except password
     */
    private AppUserResponseDto toResponseDto(AppUser user) {
        return new AppUserResponseDto(
                user.getUserId(),
                user.getFullName(),
                user.getDesignation(),
                user.getUserName(),
                user.getRole(),
                user.getStatus()
        );
    }

    /**
     * Loads a user by username for Spring Security authentication.
     *
     * <p>Called automatically by Spring Security's {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}
     * during login to retrieve user details for credential validation.
     * Never called directly by application code.
     *
     * @param username the username to look up
     * @return UserDetails containing user credentials and authorities
     * @throws UsernameNotFoundException if no user exists with the given username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));
    }

    /**
     * Creates a new system user with BCrypt hashed password.
     *
     * <p>Validates role against allowed values before saving.
     * Forces null on userId to prevent client-supplied ID upsert attacks.
     * Defaults status to ACTIVE if not provided.
     *
     * @param user the user to create (userId ignored if provided)
     * @return created user as DTO without password field
     * @throws IllegalArgumentException if role is not TECHNICAL_STAFF, MANAGER or ADMIN
     */
    public AppUserResponseDto saveAppUser(AppUser user) {

        // Force null to prevent client-supplied ID upsert attacks
        user.setUserId(null);

        // Validate role against allowed values
        List<String> validRoles = List.of("TECHNICAL_STAFF", "MANAGER", "ADMIN");
        if (user.getRole() == null || !validRoles.contains(user.getRole())) {
            throw new IllegalArgumentException(
                    "Invalid role! Allowed values: TECHNICAL_STAFF, MANAGER, ADMIN");
        }

        // Hash password before saving — never store plain text
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("ACTIVE");
        }

        return toResponseDto(appUserRepository.save(user));
    }

    /**
     * Retrieves all system users.
     *
     * @return list of all users as DTOs without password fields
     */
    public List<AppUserResponseDto> getAllAppUsers() {
        return appUserRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific user by ID.
     *
     * @param id the user ID to look up
     * @return Optional containing user DTO if found, empty if not found
     */
    public Optional<AppUserResponseDto> getAppUserById(Long id) {
        return appUserRepository.findById(id)
                .map(this::toResponseDto);
    }

    /**
     * Updates an existing user's details.
     *
     * <p>Password is only re-hashed and updated if a new non-empty
     * password is provided — preserves existing password otherwise.
     *
     * @param id          the ID of the user to update
     * @param updatedUser the new user details to apply
     * @return updated user as DTO without password field
     * @throws RuntimeException if no user found with the given ID
     */
    public AppUserResponseDto updateAppUser(Long id, AppUser updatedUser) {
        AppUser existing = appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + id));

        existing.setFullName(updatedUser.getFullName());
        existing.setUserName(updatedUser.getUserName());
        existing.setDesignation(updatedUser.getDesignation());
        existing.setRole(updatedUser.getRole());

        // Only hash and update password if a new one is provided
        if (updatedUser.getPassword() != null &&
                !updatedUser.getPassword().isEmpty()) {
            existing.setPassword(
                    passwordEncoder.encode(updatedUser.getPassword()));
        }

        if (updatedUser.getStatus() != null) {
            existing.setStatus(updatedUser.getStatus());
        }
        return toResponseDto(appUserRepository.save(existing));
    }

    /**
     * Soft deletes a user by setting their status to INACTIVE.
     *
     * <p>INACTIVE users are automatically blocked from login via
     * {@link AppUser#isEnabled()} returning false.
     * User record is preserved for audit trail — no hard delete performed.
     *
     * @param id the ID of the user to deactivate
     * @return deactivated user as DTO with INACTIVE status
     * @throws RuntimeException if no user found with the given ID
     */
    public AppUserResponseDto softDeleteAppUser(Long id) {
        AppUser existing = appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + id));
        existing.setStatus("INACTIVE");
        return toResponseDto(appUserRepository.save(existing));
    }
}