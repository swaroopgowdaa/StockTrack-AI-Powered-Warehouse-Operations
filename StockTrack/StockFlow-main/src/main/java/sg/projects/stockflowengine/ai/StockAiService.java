package sg.projects.stockflowengine.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core AI service implementing RAG (Retrieval Augmented Generation) for
 * natural language warehouse inventory queries.
 *
 * <p>Integrates three key components:
 * <ul>
 *   <li>{@link VectorStoreService} — semantic search via PGVector</li>
 *   <li>Spring AI {@link ChatClient} — communication with Ollama LLM</li>
 *   <li>Role-aware prompt engineering — tailored responses per user role</li>
 * </ul>
 *
 * <p>RAG query flow:
 * <ol>
 *   <li>User question converted to vector embedding via nomic-embed-text</li>
 *   <li>PGVector searches for semantically similar warehouse records (HNSW index)</li>
 *   <li>Relevant records assembled as structured context</li>
 *   <li>Role-specific persona + strict anti-hallucination rules applied to prompt</li>
 *   <li>Ollama (llama3.1:8b) generates grounded natural language response</li>
 * </ol>
 *
 * <p>Data privacy: all inference runs locally via Ollama — no warehouse data
 * is sent to external APIs.
 *
 * @see VectorStoreService
 * @see StockAiController
 * @see WarehouseEventListener
 */
@Service
public class StockAiService {

    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;

    /**
     * Constructs StockAiService with Spring AI ChatClient builder and vector store service.
     *
     * @param chatClientBuilder Spring AI builder auto-configured from application.properties
     *                          with Ollama base URL and model settings
     * @param vectorStoreService service for PGVector embedding and similarity search
     */
    public StockAiService(ChatClient.Builder chatClientBuilder,
                          VectorStoreService vectorStoreService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Processes a natural language warehouse query using the RAG pattern.
     *
     * <p>Retrieves semantically relevant warehouse records from PGVector,
     * constructs a role-aware prompt with strict anti-hallucination rules,
     * and returns a natural language response grounded in actual warehouse data.
     *
     * <p>Role personas:
     * <ul>
     *   <li>TECHNICAL_STAFF — operational assistant for stock and inward/outward queries</li>
     *   <li>MANAGER — managerial oversight for approvals and stock management</li>
     *   <li>ADMIN — comprehensive system-wide visibility</li>
     * </ul>
     *
     * <p>Anti-hallucination rules enforced in prompt:
     * <ul>
     *   <li>Only answers from provided context — never invents data</li>
     *   <li>Refuses out-of-domain questions (weather, news, general knowledge)</li>
     *   <li>Never associates other users' records with the requesting user</li>
     *   <li>States clearly when information is absent rather than guessing</li>
     * </ul>
     *
     * @param userQuestion the natural language question from the authenticated user
     * @param role         the user's role extracted from JWT token
     * @param username     the authenticated username extracted from JWT token
     * @return natural language response grounded in semantically retrieved warehouse data
     */
    public String query(String userQuestion, String role, String username) {

        // Step 1: Search PGVector for semantically similar records
        List<Document> relevantDocs = vectorStoreService.search(userQuestion);

        // Step 2: Build context from only relevant documents
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // Step 3: Role specific persona
        String roleInstruction = switch (role) {
            case "TECHNICAL_STAFF" -> String.format("""
                    You are StockFlow AI, a warehouse assistant for operational staff.
                    You are currently assisting: %s
                    Help with stock levels, inward and outward coordination queries.
                    """, username);
            case "MANAGER" -> String.format("""
                    You are StockFlow AI, a warehouse assistant for managers.
                    You are currently assisting: %s
                    Help with approvals, operational oversight and stock management.
                    """, username);
            case "ADMIN" -> String.format("""
                    You are StockFlow AI, a warehouse assistant for the operations director.
                    You are currently assisting: %s
                    Provide comprehensive system wide responses.
                    """, username);
            default -> "You are a warehouse inventory assistant.";
        };

        // Step 4: Strict anti-hallucination prompt
        String prompt = roleInstruction + """
        
        STRICT RULES — YOU MUST FOLLOW THESE:
        1. ONLY answer using the data provided below. Nothing else.
        2. Answer directly and confidently from the provided data. Only say "I don't have that information" if the topic is completely absent from the data.
        3. NEVER make up quantities, dates, names, or statuses.
        4. NEVER answer questions unrelated to warehouse inventory management.
        5. If asked about weather, news, general knowledge or anything outside warehouse operations, respond: "I am only able to assist with warehouse inventory queries."
        6. Always refer to data exactly as provided — do not interpret or assume.
        7. If data is ambiguous, say so clearly rather than guessing.
        
        === RELEVANT WAREHOUSE DATA ===
        """ + context + "\n\n=== QUESTION FROM " + username + " (Role: " + role + ") ===\n" +
                userQuestion +
                "\n\nNOTE: Only reference records where 'By' field matches '" + username +
                "' when the user asks about their own actions. Do not associate other users' records with this user.";

        // Step 5: Send to Ollama and return response
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}