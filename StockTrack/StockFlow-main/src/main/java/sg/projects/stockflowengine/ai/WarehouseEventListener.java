package sg.projects.stockflowengine.ai;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sg.projects.stockflowengine.service.MaterialVariantStockService;

/**
 * Central event listener responsible for keeping PGVector synchronized
 * with warehouse data changes in real time.
 *
 * <p>Listens for Spring Application Events published by the service layer
 * and delegates all vector embedding operations to {@link VectorStoreService}.
 *
 * <p>This class is the cornerstone of the AI decoupling architecture:
 * <ul>
 *   <li>Service layer publishes domain events — no AI knowledge required</li>
 *   <li>This listener handles ALL embedding concerns in one place</li>
 *   <li>Adding or removing AI functionality never touches business services</li>
 * </ul>
 *
 * <p>Handled events and their embedding actions:
 * <ul>
 *   <li>{@link InwardCreatedEvent} → embeds new inward record</li>
 *   <li>{@link InwardStatusChangedEvent} → re-embeds inward + updates stock level</li>
 *   <li>{@link OutwardCreatedEvent} → embeds new outward record</li>
 *   <li>{@link OutwardCompletedEvent} → re-embeds outward + updates stock level</li>
 *   <li>{@link MaterialCreatedOrUpdatedEvent} → embeds material record</li>
 *   <li>{@link VariantCreatedOrUpdatedEvent} → embeds variant record</li>
 * </ul>
 *
 * <p>Stock re-embedding on status changes ensures AI always reflects
 * accurate current stock levels after approvals and shipments.
 *
 * @see VectorStoreService
 * @see StockDataInitializer
 */
@Component
public class WarehouseEventListener {

    private final VectorStoreService vectorStoreService;
    private final MaterialVariantStockService stockService;

    /**
     * Constructs WarehouseEventListener with vector store and stock services.
     *
     * @param vectorStoreService service for embedding documents into PGVector
     * @param stockService       service for calculating current stock levels per variant
     */
    public WarehouseEventListener(VectorStoreService vectorStoreService,
                                  MaterialVariantStockService stockService) {
        this.vectorStoreService = vectorStoreService;
        this.stockService = stockService;
    }

    /**
     * Handles new inward creation — embeds the inward record immediately.
     *
     * @param event contains the newly created inward transaction
     */
    @EventListener
    public void handleInwardCreated(InwardCreatedEvent event) {
        vectorStoreService.embedInward(event.getInward());
    }

    /**
     * Handles inward status change — re-embeds inward and updates stock level.
     *
     * <p>Stock re-embedding is critical here since ACTIVE status means
     * goods are now approved and count towards available stock.
     *
     * @param event contains the inward transaction with updated status
     */
    @EventListener
    public void handleInwardStatusChanged(InwardStatusChangedEvent event) {
        vectorStoreService.embedInward(event.getInward());
        // Re-embed stock since approval changes stock calculation
        var stock = stockService.getStockForMaterialVariant(
                event.getInward().getMaterialVariantId());
        vectorStoreService.embedStockForVariant(stock);
    }

    /**
     * Handles new outward creation — embeds the outward record immediately.
     *
     * @param event contains the newly created outward transaction
     */
    @EventListener
    public void handleOutwardCreated(OutwardCreatedEvent event) {
        vectorStoreService.embedOutward(event.getOutward());
    }

    /**
     * Handles outward completion — re-embeds outward and updates stock level.
     *
     * <p>Stock re-embedding is critical here since COMPLETED status confirms
     * goods have physically left the warehouse, reducing available stock.
     *
     * @param event contains the outward transaction marked as COMPLETED
     */
    @EventListener
    public void handleOutwardCompleted(OutwardCompletedEvent event) {
        vectorStoreService.embedOutward(event.getOutward());
        // Re-embed stock since completion changes stock calculation
        var stock = stockService.getStockForMaterialVariant(
                event.getOutward().getMaterialVariantId());
        vectorStoreService.embedStockForVariant(stock);
    }

    /**
     * Handles material create, update or deactivation — embeds material record.
     *
     * @param event contains the material that was created, updated or deactivated
     */
    @EventListener
    public void handleMaterialCreatedOrUpdated(MaterialCreatedOrUpdatedEvent event) {
        vectorStoreService.embedMaterial(event.getMaterial());
    }

    /**
     * Handles variant create, update or deactivation — embeds variant record.
     *
     * @param event contains the variant that was created, updated or deactivated
     */
    @EventListener
    public void handleVariantCreatedOrUpdated(VariantCreatedOrUpdatedEvent event) {
        vectorStoreService.embedVariant(event.getVariant());
    }
}