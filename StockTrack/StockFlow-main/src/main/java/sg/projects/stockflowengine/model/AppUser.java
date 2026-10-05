package sg.projects.stockflowengine.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing a system user in the warehouse management system.
 *
 * <p>Implements {@link UserDetails} to integrate directly with Spring Security,
 * allowing the entity to serve as both a JPA entity and a security principal.
 *
 * <p>Three roles are supported:
 * <ul>
 *   <li>TECHNICAL_STAFF — warehouse floor staff, data entry operations</li>
 *   <li>MANAGER — supervisors with approval and completion authority</li>
 *   <li>ADMIN — operations director with full system access</li>
 * </ul>
 *
 * <p>Passwords are always stored as BCrypt hashes — never plain text.
 * INACTIVE users are automatically blocked from login via {@link #isEnabled()}.
 *
 * <p>Mapped to the {@code user_master} table in PostgreSQL.
 * User data is intentionally excluded from PGVector embeddings —
 * credentials and roles must never be exposed to the AI layer.
 *
 * @see sg.projects.stockflowengine.service.AppUserService
 * @see sg.projects.stockflowengine.dto.AppUserResponseDto
 */
@Entity
@Table(name="UserMaster")
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank(message = "Full name is required.")
    private String fullName;

    /** Job title or department (e.g. Electrical Engineer, MIS Executive) */
    private String designation;

    @NotBlank(message = "Username is required.")
    private String userName;

    /** BCrypt hashed password — never stored as plain text */
    @NotBlank(message = "Password is required.")
    private String password;

    @NotBlank(message = "Role is required.")
    private String role;

    /** ACTIVE or INACTIVE — INACTIVE users cannot log in */
    private String status;

    /**
     * Default no-args constructor required by JPA.
     */
    public AppUser() {}

    /**
     * Constructs an AppUser with all fields.
     *
     * @param userId      auto-generated user ID
     * @param fullName    user's full name
     * @param designation job title or department
     * @param userName    unique login username
     * @param password    BCrypt hashed password
     * @param role        system role (TECHNICAL_STAFF, MANAGER, ADMIN)
     * @param status      account status (ACTIVE or INACTIVE)
     */
    public AppUser(Long userId, String fullName, String designation, String userName,
                   String password, String role, String status) {
        this.userId = userId;
        this.fullName = fullName;
        this.designation = designation;
        this.userName = userName;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    /**
     * Returns the user's granted authorities for Spring Security.
     *
     * <p>Role is prefixed with "ROLE_" as required by Spring Security
     * for {@code hasRole()} checks in {@link sg.projects.stockflowengine.security.SecurityConfig}.
     *
     * @return single-element list containing the user's role authority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * Returns the username used by Spring Security as the unique identifier.
     *
     * @return the login username
     */
    @Override
    public String getUsername() {
        return userName;
    }

    /**
     * Returns the BCrypt hashed password for Spring Security validation.
     *
     * @return BCrypt hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Account expiry is not implemented — always returns true.
     *
     * @return true
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account locking is not implemented — always returns true.
     *
     * @return true
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Credential expiry is not implemented — always returns true.
     *
     * @return true
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Determines if the user account is enabled for login.
     *
     * <p>INACTIVE users are automatically blocked from authentication
     * by Spring Security without requiring additional filter logic.
     *
     * @return true if status is ACTIVE, false if INACTIVE
     */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    // Getters and Setters

    /** @return the auto-generated user ID */
    public Long getUserId() { return userId; }

    /** @param userId the user ID to set */
    public void setUserId(Long userId) { this.userId = userId; }

    /** @return the user's full name */
    public String getFullName() { return fullName; }

    /** @param fullName the full name to set */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return the user's job title or department */
    public String getDesignation() { return designation; }

    /** @param designation the designation to set */
    public void setDesignation(String designation) { this.designation = designation; }

    /** @return the unique login username */
    public String getUserName() { return userName; }

    /** @param userName the username to set */
    public void setUserName(String userName) { this.userName = userName; }

    /** @param password the BCrypt hashed password to set */
    public void setPassword(String password) { this.password = password; }

    /** @return the user's system role */
    public String getRole() { return role; }

    /** @param role the role to set */
    public void setRole(String role) { this.role = role; }

    /** @return the account status (ACTIVE or INACTIVE) */
    public String getStatus() { return status; }

    /** @param status the status to set */
    public void setStatus(String status) { this.status = status; }
}
