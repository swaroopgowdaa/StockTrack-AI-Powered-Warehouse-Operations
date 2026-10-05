package sg.projects.stockflowengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the StockFlowEngine application.
 *
 * <p>StockFlowEngine is a production-grade warehouse inventory management system
 * built with Spring Boot 4.1.0 as a monolithic architecture, featuring:
 * <ul>
 *   <li>JWT-secured REST API with role-based access control</li>
 *   <li>Dynamic stock calculation engine</li>
 *   <li>AI-powered natural language inventory queries via Spring AI + Ollama</li>
 *   <li>Semantic search using PGVector and nomic-embed-text embeddings</li>
 *   <li>Event-driven architecture via Spring Application Events</li>
 * </ul>
 *
 * <p>Designed as a monolith with clean package separation by concern,
 * providing a solid foundation for future extraction into microservices
 * if scale demands it.
 *
 * <p>On startup:
 * <ul>
 *   <li>{@link sg.projects.stockflowengine.config.DataSeeder} seeds the default
 *       ADMIN user if not already present</li>
 *   <li>{@link sg.projects.stockflowengine.ai.StockDataInitializer} embeds all
 *       existing warehouse data into PGVector if vector store is empty</li>
 * </ul>
 *
 * @see sg.projects.stockflowengine.config.DataSeeder
 * @see sg.projects.stockflowengine.ai.StockDataInitializer
 */
@SpringBootApplication
public class StockFlowEngineApplication {

    /**
     * Launches the StockFlowEngine Spring Boot application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(StockFlowEngineApplication.class, args);
    }
}