package sg.projects.stockflowengine.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Service responsible for JWT token generation, parsing and validation.
 *
 * <p>Implements stateless authentication using JSON Web Tokens (JWT).
 * Tokens are signed with HMAC SHA-256 using a secret key stored as
 * an environment variable — never hardcoded in source.
 *
 * <p>JWT token structure:
 * <pre>
 * Header  → algorithm (HS256)
 * Payload → subject (username), issuedAt, expiration
 * Signature → HMAC SHA-256 of header + payload using secret key
 * </pre>
 *
 * <p>Token validation is purely mathematical — no database lookup required.
 * The signature proves the token was issued by this server and hasn't been tampered with.
 * A database lookup is only performed to confirm the user still exists and is active.
 *
 * <p>Configuration via {@code application.properties}:
 * <ul>
 *   <li>{@code jwt.secret} → loaded from JWT_SECRET environment variable</li>
 *   <li>{@code jwt.expiration} → token lifetime in milliseconds (default 86400000 = 24hrs)</li>
 * </ul>
 *
 * @see JwtAuthFilter
 * @see sg.projects.stockflowengine.controller.AuthController
 */
@Service
public class JwtService {

    /** Secret key loaded from JWT_SECRET environment variable */
    @Value("${jwt.secret}")
    private String secretKey;

    /** Token expiration time in milliseconds — configured in application.properties */
    @Value("${jwt.expiration}")
    private long expirationTime;

    /**
     * Returns the HMAC SHA-256 signing key derived from the secret.
     *
     * @return SecretKey for token signing and verification
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generates a signed JWT token for the authenticated user.
     *
     * <p>Token payload contains:
     * <ul>
     *   <li>subject — the username</li>
     *   <li>issuedAt — current timestamp</li>
     *   <li>expiration — current timestamp + expirationTime</li>
     * </ul>
     *
     * @param username the authenticated username to embed in the token
     * @return signed JWT token string in format: header.payload.signature
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token string to parse
     * @return the username embedded in the token payload
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates a JWT token against the expected username.
     *
     * <p>Checks both:
     * <ul>
     *   <li>Username in token matches the expected username</li>
     *   <li>Token has not expired</li>
     * </ul>
     *
     * @param token    the JWT token to validate
     * @param username the expected username to match against
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean isTokenValid(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }

    /**
     * Checks if a JWT token has passed its expiration time.
     *
     * @param token the JWT token to check
     * @return true if token is expired, false if still valid
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token the JWT token to parse
     * @return the expiration date embedded in the token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor — parses token and applies a claims resolver function.
     *
     * <p>{@link Claims} is the decoded JWT payload as a Java object.
     * The resolver function extracts a specific field:
     * <ul>
     *   <li>{@code Claims::getSubject} → extracts username</li>
     *   <li>{@code Claims::getExpiration} → extracts expiry date</li>
     * </ul>
     *
     * <p>Token signature is verified during parsing — tampered tokens throw an exception.
     *
     * @param token          the JWT token to parse
     * @param claimsResolver function to extract a specific claim from the payload
     * @param <T>            the type of the claim to extract
     * @return the extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}