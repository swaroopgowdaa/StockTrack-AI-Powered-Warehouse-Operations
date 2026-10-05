package sg.projects.stockflowengine.ai;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sg.projects.stockflowengine.service.*;

import java.util.List;

/**
 * Startup configuration that seeds the PGVector store with existing warehouse data.
 *
 * <p>Runs once on application startup via {@link CommandLineRunner}.
 * Checks if the vector store is already populated — if empty, embeds all
 * current warehouse data to ensure AI queries are immediately functional.
 *
 * <p>Embedding order:
 * <ol>
 *   <li>Materials — master data for material discoverability</li>
 *   <li>Variants — master data for variant queries</li>
 *   <li>Stock levels — current inventory counts per variant</li>
 *   <li>Inwards — all inward transaction history</li>
 *   <li>Outwards — all outward transaction history</li>
 * </ol>
 *
 * <p>After startup, all subsequent changes are handled in real time by
 * {@link WarehouseEventListener} via Spring Application Events — this
 * initializer only runs when the vector store is empty (first startup
 * or after a vector store reset).
 *
 * <p>Note: AppUser data is intentionally excluded from vector store embedding.
 * User credentials, roles and personal data must never be exposed to the AI layer.
 *
 * @see WarehouseEventListener
 * @see VectorStoreService
 */
@Configuration
public class StockDataInitializer {

    /**
     * Seeds PGVector with all existing warehouse data on application startup.
     *
     * <p>Skips embedding if vector store is already populated to avoid
     * duplicate embeddings on every restart.
     *
     * @param vectorStoreService    service for embedding documents into PGVector
     * @param stockService          service for retrieving current stock levels
     * @param inwardService         service for retrieving all inward records
     * @param outwardService        service for retrieving all outward records
     * @param materialService       service for retrieving all materials
     * @param materialVariantService service for retrieving all variants
     * @return CommandLineRunner that executes embedding logic on startup
     */
    @Bean
    public CommandLineRunner embedStockData(VectorStoreService vectorStoreService,
                                            MaterialVariantStockService stockService,
                                            InwardService inwardService,
                                            OutwardService outwardService,
                                            MaterialService materialService,
                                            MaterialVariantService materialVariantService) {
        return args -> {
            List<?> existing = vectorStoreService.search("stock");

            if (existing.isEmpty()) {
                System.out.println(">>> Embedding warehouse data into PGVector...");

                // Embed materials
                materialService.getAllMaterials()
                        .forEach(vectorStoreService::embedMaterial);

                // Embed variants
                materialVariantService.getAllMaterialVariants()
                        .forEach(vectorStoreService::embedVariant);

                // Embed stock
                stockService.getAllStock()
                        .forEach(vectorStoreService::embedStockForVariant);

                // Embed inwards
                inwardService.getAllInwards()
                        .forEach(vectorStoreService::embedInward);

                // Embed outwards
                outwardService.getAllOutwards()
                        .forEach(vectorStoreService::embedOutward);

                System.out.println(">>> Warehouse data embedded successfully!");
            } else {
                System.out.println(">>> Vector store already populated, skipping embedding.");
            }
        };
    }
}