package sg.projects.stockflowengine.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import sg.projects.stockflowengine.ai.VariantCreatedOrUpdatedEvent;
import sg.projects.stockflowengine.model.MaterialVariant;
import sg.projects.stockflowengine.repository.InwardRepository;
import sg.projects.stockflowengine.repository.MaterialRepository;
import sg.projects.stockflowengine.repository.MaterialVariantRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for material variant business logic.
 *
 * <p>Variants represent specific types or specifications of a parent material.
 * They sit in the middle of the inventory hierarchy:
 * <pre>
 * Material → MaterialVariant → Inward/Outward transactions
 * </pre>
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Variant must belong to an existing parent material</li>
 *   <li>Cannot deactivate a variant with PENDING_INSPECTION or ACTIVE inwards</li>
 *   <li>REJECTED inwards do not block deactivation — dead records, no stock impact</li>
 *   <li>Soft delete (INACTIVE) preserves historical transaction records</li>
 *   <li>materialVariantId forced null to prevent client-supplied ID upsert attacks</li>
 * </ul>
 *
 * <p>AI integration via Spring Events :
 * <ul>
 *   <li>On create → publishes {@link VariantCreatedOrUpdatedEvent}</li>
 *   <li>On update → publishes {@link VariantCreatedOrUpdatedEvent}</li>
 *   <li>On soft delete → publishes {@link VariantCreatedOrUpdatedEvent}</li>
 * </ul>
 *
 * @see MaterialVariantRepository
 * @see sg.projects.stockflowengine.ai.WarehouseEventListener
 */
@Service
public class MaterialVariantService {

    private final MaterialVariantRepository materialVariantRepository;
    private final MaterialRepository materialRepository;
    private final InwardRepository inwardRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs MaterialVariantService with required dependencies.
     *
     * @param materialVariantRepository repository for variant persistence
     * @param materialRepository        repository for parent material validation
     * @param inwardRepository          repository for soft delete protection check
     * @param eventPublisher            Spring event publisher for AI embedding decoupling
     */
    public MaterialVariantService(MaterialVariantRepository materialVariantRepository,
                                  MaterialRepository materialRepository,
                                  InwardRepository inwardRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.materialVariantRepository = materialVariantRepository;
        this.materialRepository = materialRepository;
        this.inwardRepository = inwardRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new material variant under an existing parent material.
     *
     * <p>Forces null on materialVariantId to prevent client-supplied ID upsert attacks.
     * Validates parent material existence before saving.
     * Publishes {@link VariantCreatedOrUpdatedEvent} after save to trigger
     * immediate PGVector embedding via {@link sg.projects.stockflowengine.ai.WarehouseEventListener}.
     *
     * @param variant the variant to create (materialVariantId ignored if provided)
     * @return saved variant with auto-generated ID and ACTIVE status
     * @throws IllegalArgumentException if parent material does not exist
     */
    public MaterialVariant saveMaterialVariant(MaterialVariant variant) {

        // Force null to prevent client-supplied ID upsert attacks
        variant.setMaterialVariantId(null);

        if (variant.getStatus() == null || variant.getStatus().isEmpty()) {
            variant.setStatus("ACTIVE");
        }

        // Validate parent material exists
        if (variant.getMaterialId() == null ||
                !materialRepository.existsById(variant.getMaterialId())) {
            throw new IllegalArgumentException(
                    "Invalid Material! A Variant must belong to an existing parent Material.");
        }

        MaterialVariant saved = materialVariantRepository.save(variant);

        // Publish event — WarehouseEventListener handles AI embedding
        eventPublisher.publishEvent(new VariantCreatedOrUpdatedEvent(saved));
        return saved;
    }

    /**
     * Retrieves all material variants in the system.
     *
     * @return list of all variants including INACTIVE ones
     */
    public List<MaterialVariant> getAllMaterialVariants() {
        return materialVariantRepository.findAll();
    }

    /**
     * Retrieves a specific material variant by ID.
     *
     * @param id the variant ID to look up
     * @return Optional containing the variant if found, empty if not found
     */
    public Optional<MaterialVariant> getMaterialVariantById(Long id) {
        return materialVariantRepository.findById(id);
    }

    /**
     * Updates an existing variant's name or parent material.
     *
     * <p>Publishes {@link VariantCreatedOrUpdatedEvent} after update to
     * re-embed the variant with updated details in PGVector.
     *
     * @param id             the ID of the variant to update
     * @param updatedVariant the new variant details to apply
     * @return updated variant record
     * @throws RuntimeException if no variant found with the given ID
     */
    public MaterialVariant updateMaterialVariant(Long id, MaterialVariant updatedVariant) {
        MaterialVariant existing = materialVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Variant not found with id: " + id));
        existing.setMaterialVariantName(updatedVariant.getMaterialVariantName());
        existing.setMaterialId(updatedVariant.getMaterialId());
        if (updatedVariant.getStatus() != null) {
            existing.setStatus(updatedVariant.getStatus());
        }
        MaterialVariant updated = materialVariantRepository.save(existing);

        // Publish event — WarehouseEventListener re-embeds with updated details
        eventPublisher.publishEvent(new VariantCreatedOrUpdatedEvent(updated));
        return updated;
    }

    /**
     * Soft deletes a variant by setting its status to INACTIVE.
     *
     * <p>Blocked if any PENDING_INSPECTION or ACTIVE inwards exist for this variant —
     * live or pending stock cannot be orphaned.
     * REJECTED inwards do not block deactivation since they have no stock impact.
     *
     * <p>Publishes {@link VariantCreatedOrUpdatedEvent} after deactivation to
     * re-embed the variant with INACTIVE status in PGVector.
     *
     * @param id the ID of the variant to deactivate
     * @return deactivated variant with INACTIVE status
     * @throws RuntimeException         if no variant found with the given ID
     * @throws IllegalArgumentException if PENDING_INSPECTION or ACTIVE inwards exist
     */
    public MaterialVariant softDeleteMaterialVariant(Long id) {
        MaterialVariant existing = materialVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Variant not found with id: " + id));

        // Soft delete protection
        boolean hasActiveInwards = inwardRepository
                .existsByMaterialVariantIdAndStatusIn(
                        id, List.of("PENDING_INSPECTION", "ACTIVE"));
        if (hasActiveInwards) {
            throw new IllegalArgumentException(
                    "Cannot deactivate Variant! Active or pending inwards exist.");
        }
        existing.setStatus("INACTIVE");
        MaterialVariant updated = materialVariantRepository.save(existing);

        // Publish event — WarehouseEventListener re-embeds with INACTIVE status
        eventPublisher.publishEvent(new VariantCreatedOrUpdatedEvent(updated));
        return updated;
    }
}