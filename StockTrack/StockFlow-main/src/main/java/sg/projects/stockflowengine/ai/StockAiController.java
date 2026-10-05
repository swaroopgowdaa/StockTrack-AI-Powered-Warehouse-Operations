package sg.projects.stockflowengine.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;

/**
 * REST controller exposing the StockFlow AI conversational query endpoint.
 *
 * <p>Provides natural language querying of warehouse inventory data using
 * RAG (Retrieval Augmented Generation) powered by Spring AI, Ollama and PGVector.
 *
 * <p>Accessible by all authenticated roles (TECHNICAL_STAFF, MANAGER, ADMIN).
 * Each role receives a tailored AI persona and response style via
 * {@link StockAiService}.
 *
 * <p>Username and role are extracted from the JWT token on every request
 * via {@link SecurityContextHolder} — no client-supplied identity is trusted.
 *
 * @see StockAiService
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class StockAiController {

    private final StockAiService stockAiService;

    /**
     * Constructs StockAiController with the AI query service.
     *
     * @param stockAiService service handling RAG-based warehouse queries
     */
    public StockAiController(StockAiService stockAiService) {
        this.stockAiService = stockAiService;
    }

    /**
     * Processes a natural language warehouse inventory query.
     *
     * <p>Extracts the authenticated user's role and username from the JWT token,
     * then delegates to {@link StockAiService} for RAG-based response generation.
     *
     * <p>Example questions:
     * <ul>
     *   <li>"What is the current stock for all variants?"</li>
     *   <li>"What inwards are pending inspection?"</li>
     *   <li>"What did I dispatch today?"</li>
     *   <li>"Which variants are running low on stock?"</li>
     * </ul>
     *
     * @param request JSON body containing "question" key with the user's query
     * @return JSON response with "answer" key containing the AI generated response,
     *         or 400 Bad Request if question is empty
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> query(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question cannot be empty."));
        }

        // Extract role and username from JWT via SecurityContext
        // Never trust client-supplied identity — always read from token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next()
                .getAuthority()
                .replace("ROLE_", "");
        String username = auth.getName();

        String answer = stockAiService.query(question, role, username);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}