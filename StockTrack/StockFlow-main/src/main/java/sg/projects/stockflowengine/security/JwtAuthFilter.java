package sg.projects.stockflowengine.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sg.projects.stockflowengine.service.AppUserService;

import java.io.IOException;

/**
 * JWT authentication filter that intercepts every HTTP request to validate
 * Bearer tokens before they reach any controller.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee exactly one execution
 * per request, regardless of the filter chain configuration.
 *
 * <p>Filter execution flow:
 * <ol>
 *   <li>Extract Authorization header from request</li>
 *   <li>Skip filter if no Bearer token present (public endpoints)</li>
 *   <li>Extract username from JWT token payload via {@link JwtService}</li>
 *   <li>Load user from database via {@link AppUserService}</li>
 *   <li>Validate token signature and expiry via {@link JwtService}</li>
 *   <li>Set authentication in {@link SecurityContextHolder} if valid</li>
 *   <li>Continue filter chain regardless of outcome</li>
 * </ol>
 *
 * <p>If token is invalid or missing, {@link SecurityContextHolder} remains
 * unauthenticated and Spring Security rejects the request with 403 Forbidden.
 *
 * <p>Registered before {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}
 * in the filter chain via {@link SecurityConfig}.
 *
 * @see JwtService
 * @see SecurityConfig
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserService appUserService;

    /**
     * Constructs JwtAuthFilter with JWT service and user details service.
     *
     * @param jwtService     service for token extraction and validation
     * @param appUserService service for loading user details from database
     */
    public JwtAuthFilter(JwtService jwtService, AppUserService appUserService) {
        this.jwtService = jwtService;
        this.appUserService = appUserService;
    }

    /**
     * Intercepts each request to validate JWT token and set authentication context.
     *
     * <p>Requests without a valid Bearer token pass through unauthenticated —
     * Spring Security's authorization rules then determine if access is allowed.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Get Authorization header
        String authHeader = request.getHeader("Authorization");

        // Step 2: Check if header exists and starts with "Bearer "
        // Skip filter for requests without token (e.g. /api/auth/login)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract token (remove "Bearer " prefix — 7 characters)
        String token = authHeader.substring(7);

        // Step 4: Extract username from token payload (Claims.getSubject())
        String username = jwtService.extractUsername(token);

        // Step 5: Only process if username extracted and no auth set yet
        // Prevents re-processing on already authenticated requests
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Step 6: Load user from DB to get authorities and enabled status
            UserDetails userDetails = appUserService.loadUserByUsername(username);

            // Step 7: Validate token signature and expiry
            if (jwtService.isTokenValid(token, username)) {

                // Step 8: Create auth token with user details and authorities
                // Null credentials — password not needed post-authentication
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // Set in SecurityContext — marks request as authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 9: Continue filter chain regardless of authentication outcome
        filterChain.doFilter(request, response);
    }
}