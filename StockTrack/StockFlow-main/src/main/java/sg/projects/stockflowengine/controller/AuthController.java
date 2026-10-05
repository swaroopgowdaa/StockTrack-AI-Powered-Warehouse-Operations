package sg.projects.stockflowengine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.security.JwtService;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller handling user authentication and JWT token issuance.
 *
 * <p>The only public endpoint in the system — no JWT token required to access.
 * All other endpoints require a valid Bearer token in the Authorization header.
 *
 * <p>Authentication flow:
 * <ol>
 *   <li>Client sends username and password in request body</li>
 *   <li>{@link AuthenticationManager} validates credentials via
 *       {@link sg.projects.stockflowengine.service.AppUserService#loadUserByUsername}</li>
 *   <li>BCrypt compares incoming password with stored hash</li>
 *   <li>{@link JwtService} generates a signed JWT token on success</li>
 *   <li>Token and role returned to client for subsequent requests</li>
 * </ol>
 *
 * <p>On failure, {@link org.springframework.security.core.AuthenticationException}
 * is caught by {@link sg.projects.stockflowengine.exception.GlobalExceptionHandler}
 * and returns 401 Unauthorized.
 *
 * @see JwtService
 * @see sg.projects.stockflowengine.security.SecurityConfig
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Constructs AuthController with authentication manager and JWT service.
     *
     * @param authenticationManager Spring Security manager for credential validation
     * @param jwtService            service for JWT token generation and validation
     */
    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates a user and returns a JWT token with their role.
     *
     * <p>The returned token must be included in subsequent requests as:
     * {@code Authorization: Bearer <token>}
     *
     * @param request JSON body containing "userName" and "password" fields
     * @return JWT token and role on success, 401 Unauthorized on invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {

        // Step 1: Extract username and password from request body
        String username = request.get("userName");
        String password = request.get("password");

        // Step 2: AuthenticationManager validates credentials
        // Internally calls loadUserByUsername() then BCrypt password check
        // Throws AuthenticationException if invalid
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // Step 3: Generate JWT token for the authenticated user
        String token = jwtService.generateToken(username);

        // Step 4: Return token + role to client
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("role", authentication.getAuthorities()
                .iterator().next().getAuthority()
                .replace("ROLE_", "")); // strip ROLE_ prefix for cleaner response

        return ResponseEntity.ok(response);
    }
}