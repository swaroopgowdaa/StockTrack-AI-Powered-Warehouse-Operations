package sg.projects.stockflowengine.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import sg.projects.stockflowengine.ai.InwardCreatedEvent;
import sg.projects.stockflowengine.ai.InwardStatusChangedEvent;
import sg.projects.stockflowengine.model.Inward;
import sg.projects.stockflowengine.model.MaterialVariant;
import sg.projects.stockflowengine.repository.InwardRepository;
import sg.projects.stockflowengine.repository.MaterialVariantRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for inward transaction business logic.
 *
 * <p>Inward transactions represent the receipt of goods into the warehouse.
 * This service enforces the inward status workflow and validates all
 * business rules before persisting transactions.
 *
 * <p>Status workflow enforced:
 * <pre>
 * PENDING_INSPECTION → ACTIVE   (approved by MANAGER/ADMIN)
 * PENDING_INSPECTION → REJECTED (rejected by MANAGER/ADMIN)
 * </pre>
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Variant must exist and be ACTIVE before inwarding</li>
 *   <li>Quantity must be greater than zero</li>
 *   <li>Only PENDING_INSPECTION inwards can have status updated</li>
 *   <li>Only ACTIVE and REJECTED are valid status transitions</li>
 *   <li>materialId auto-populated from variant — never client-supplied</li>
 *   <li>inwardDate defaults to today if not provided</li>
 *   <li>inwardId forced null to prevent upsert attacks</li>
 * </ul>
 *
 * <p>AI integration via Spring Events :
 * <ul>
 *   <li>On create → publishes {@link InwardCreatedEvent}</li>
 *   <li>On status change → publishes {@link InwardStatusChangedEvent}</li>
 * </ul>
 *
 * @see InwardRepository
 * @see sg.projects.stockflowengine.ai.WarehouseEventListener
 */
@Service
public class InwardService {

    private final InwardRepository inwardRepository;
    private final MaterialVariantRepository materialVariantRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs InwardService with required dependencies.
     *
     * @param inwardRepository          repository for inward persistence
     * @param materialVariantRepository repository for variant validation
     * @param eventPublisher            Spring event publisher for AI embedding decoupling
     */
    public InwardService(InwardRepository inwardRepository,
                         MaterialVariantRepository materialVariantRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.inwardRepository = inwardRepository;
        this.materialVariantRepository = materialVariantRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new inward transaction for received goods.
     *
     * <p>Validates variant existence and ACTIVE status before saving.
     * Publishes {@link InwardCreatedEvent} after successful save to
     * trigger immediate PGVector embedding via {@link sg.projects.stockflowengine.ai.WarehouseEventListener}.
     *
     * @param inward the inward transaction to create
     * @return saved inward with PENDING_INSPECTION status and auto-populated materialId
     * @throws IllegalArgumentException if variant doesn't exist, is INACTIVE,
     *                                  or quantity is invalid
     */
    public Inward saveInward(Inward inward) {

        // Force null to prevent client-supplied ID upsert attacks
        inward.setInwardId(null);

        if (inward.getQuantity() == null || inward.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be specified and greater than zero!");
        }

        // Single DB call — validates existence and retrieves materialId in one query
        MaterialVariant variant = materialVariantRepository
                .findById(inward.getMaterialVariantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Variant! You cannot inward a material that doesn't exist."));

        if ("INACTIVE".equals(variant.getStatus())) {
            throw new IllegalArgumentException(
                    "Cannot inward against an INACTIVE variant!");
        }

        // Default inward date to today if not provided
        if (inward.getInwardDate() == null) {
            inward.setInwardDate(LocalDate.now());
        }

        // Auto-populate materialId from variant — never trust client-supplied value
        inward.setMaterialId(variant.getMaterialId());

        if (inward.getStatus() == null || inward.getStatus().isEmpty()) {
            inward.setStatus("PENDING_INSPECTION");
        }

        Inward saved = inwardRepository.save(inward);

        // Publish event — WarehouseEventListener handles AI embedding
        eventPublisher.publishEvent(new InwardCreatedEvent(saved));
        return saved;
    }

    /**
     * Retrieves all inward transactions in the system.
     *
     * @return list of all inwards regardless of status
     */
    public List<Inward> getAllInwards() {
        return inwardRepository.findAll();
    }

    /**
     * Retrieves a specific inward transaction by ID.
     *
     * @param id the inward transaction ID to look up
     * @return Optional containing the inward if found, empty if not found
     */
    public Optional<Inward> getInwardById(Long id) {
        return inwardRepository.findById(id);
    }

    /**
     * Updates the status of a PENDING_INSPECTION inward transaction.
     *
     * <p>Publishes {@link InwardStatusChangedEvent} after successful update to
     * trigger re-embedding of the inward record and stock level recalculation
     * in PGVector via {@link sg.projects.stockflowengine.ai.WarehouseEventListener}.
     *
     * @param id        the inward transaction ID to update
     * @param newStatus the new status — must be ACTIVE or REJECTED
     * @return updated inward with new status applied
     * @throws IllegalArgumentException if inward not found, not in PENDING_INSPECTION
     *                                  status, or new status is invalid
     */
    public Inward updateInwardStatus(Long id, String newStatus) {
        Inward inward = inwardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inward transaction not found."));

        // Only PENDING_INSPECTION inwards can be transitioned
        if (!"PENDING_INSPECTION".equals(inward.getStatus())) {
            throw new IllegalArgumentException(
                    "Only PENDING_INSPECTION inwards can be approved or rejected.");
        }

        // Validate status transition value
        List<String> validStatuses = List.of("ACTIVE", "REJECTED");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status! Allowed values: ACTIVE, REJECTED");
        }

        inward.setStatus(newStatus);
        Inward updated = inwardRepository.save(inward);

        // Publish event — WarehouseEventListener re-embeds inward and updates stock
        eventPublisher.publishEvent(new InwardStatusChangedEvent(updated));
        return updated;
    }
}