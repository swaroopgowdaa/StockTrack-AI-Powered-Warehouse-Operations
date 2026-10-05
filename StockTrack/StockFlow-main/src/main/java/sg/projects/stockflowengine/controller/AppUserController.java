package sg.projects.stockflowengine.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.dto.AppUserResponseDto;
import sg.projects.stockflowengine.model.AppUser;
import sg.projects.stockflowengine.service.AppUserService;
import java.util.List;

/**
 * REST controller for managing system users.
 *
 * <p>Provides CRUD operations for warehouse staff accounts.
 * All endpoints are restricted to ADMIN role only via
 * {@link sg.projects.stockflowengine.security.SecurityConfig}.
 *
 * <p>Returns {@link AppUserResponseDto} on all responses — password hash
 * is never exposed in API responses by design.
 *
 * <p>Supported roles that can be assigned:
 * <ul>
 *   <li>TECHNICAL_STAFF — warehouse floor staff, data entry</li>
 *   <li>MANAGER — supervisors, approval authority</li>
 *   <li>ADMIN — operations director, full system access</li>
 * </ul>
 *
 * @see AppUserService
 * @see AppUserResponseDto
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class AppUserController {

    private final AppUserService appUserService;

    /**
     * Constructs AppUserController with the user management service.
     *
     * @param appUserService service handling user business logic and persistence
     */
    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * Creates a new system user with BCrypt hashed password.
     *
     * <p>Validates role against allowed values (TECHNICAL_STAFF, MANAGER, ADMIN).
     * Defaults status to ACTIVE if not provided.
     * Client-supplied userId is ignored — ID is always auto-generated.
     *
     * @param user the user details to create (userId ignored if provided)
     * @return created user details without password field
     */
    @PostMapping
    public ResponseEntity<AppUserResponseDto> createUser(@Valid @RequestBody AppUser user) {
        return ResponseEntity.ok(appUserService.saveAppUser(user));
    }

    /**
     * Retrieves all system users.
     *
     * @return list of all users without password fields
     */
    @GetMapping
    public ResponseEntity<List<AppUserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(appUserService.getAllAppUsers());
    }

    /**
     * Retrieves a specific user by ID.
     *
     * @param id the user ID to look up
     * @return user details without password, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> getUserById(@PathVariable Long id) {
        return appUserService.getAppUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing user's details.
     *
     * <p>Password is only re-hashed and updated if a new password is provided.
     * If password field is empty or null, existing password is preserved.
     *
     * @param id   the ID of the user to update
     * @param user the updated user details
     * @return updated user details without password field
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> updateUser(@PathVariable Long id,
                                                         @RequestBody AppUser user) {
        return ResponseEntity.ok(appUserService.updateAppUser(id, user));
    }

    /**
     * Soft deletes a user by setting their status to INACTIVE.
     *
     * <p>INACTIVE users cannot log in ({@link AppUser#isEnabled()} returns false).
     * User record is preserved for audit trail — no hard delete performed.
     *
     * @param id the ID of the user to deactivate
     * @return deactivated user details with INACTIVE status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(appUserService.softDeleteAppUser(id));
    }
}