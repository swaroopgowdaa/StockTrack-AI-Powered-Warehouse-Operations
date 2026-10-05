package sg.projects.stockflowengine.dto;

/**
 * Data Transfer Object for AppUser API responses.
 *
 * <p>Intentionally excludes the password field — BCrypt hashed passwords
 * must never be exposed in API responses, even as hashes.
 *
 * <p>Used by {@link sg.projects.stockflowengine.controller.AppUserController}
 * for all user-related responses (create, read, update, soft delete).
 *
 * @see sg.projects.stockflowengine.model.AppUser
 * @see sg.projects.stockflowengine.service.AppUserService
 */
public class AppUserResponseDto {

    private Long userId;
    private String fullName;
    private String designation;
    private String userName;
    private String role;
    private String status;

    /**
     * Constructs AppUserResponseDto with all safe user fields.
     *
     * @param userId      the auto-generated user ID
     * @param fullName    the user's full name
     * @param designation the user's job title or department
     * @param userName    the unique username used for login
     * @param role        the user's system role (TECHNICAL_STAFF, MANAGER, ADMIN)
     * @param status      the account status (ACTIVE or INACTIVE)
     */
    public AppUserResponseDto(Long userId, String fullName, String designation,
                              String userName, String role, String status) {
        this.userId = userId;
        this.fullName = fullName;
        this.designation = designation;
        this.userName = userName;
        this.role = role;
        this.status = status;
    }

    /**
     * Returns the user's auto-generated ID.
     *
     * @return user ID
     */
    public Long getUserId() { return userId; }

    /**
     * Returns the user's full name.
     *
     * @return full name
     */
    public String getFullName() { return fullName; }

    /**
     * Returns the user's job title or department designation.
     *
     * @return designation
     */
    public String getDesignation() { return designation; }

    /**
     * Returns the unique username used for authentication.
     *
     * @return username
     */
    public String getUserName() { return userName; }

    /**
     * Returns the user's system role.
     *
     * @return role (TECHNICAL_STAFF, MANAGER, or ADMIN)
     */
    public String getRole() { return role; }

    /**
     * Returns the account status.
     *
     * @return status (ACTIVE or INACTIVE)
     */
    public String getStatus() { return status; }
}