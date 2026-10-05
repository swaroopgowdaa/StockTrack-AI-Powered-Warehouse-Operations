package sg.projects.stockflowengine.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import sg.projects.stockflowengine.ai.MaterialCreatedOrUpdatedEvent;
import sg.projects.stockflowengine.model.Material;
import sg.projects.stockflowengine.repository.MaterialRepository;
import sg.projects.stockflowengine.repository.MaterialVariantRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for material master data business logic.
 *
 * <p>Materials are the top-level entity in the inventory hierarchy:
 * <pre>
 * Material → MaterialVariant → Inward/Outward transactions
 * </pre>
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Material names must be unique — enforced at DB level via unique constraint</li>
 *   <li>Cannot deactivate a material with ACTIVE variants under it</li>
 *   <li>Soft delete (INACTIVE) preserves historical transaction records</li>
 *   <li>materialId forced null to prevent client-supplied ID upsert attacks</li>
 * </ul>
 *
 * <p>AI integration via Spring Events :
 * <ul>
 *   <li>On create → publishes {@link MaterialCreatedOrUpdatedEvent}</li>
 *   <li>On update → publishes {@link MaterialCreatedOrUpdatedEvent}</li>
 *   <li>On soft delete → publishes {@link MaterialCreatedOrUpdatedEvent}</li>
 * </ul>
 *
 * @see MaterialRepository
 * @see sg.projects.stockflowengine.ai.WarehouseEventListener
 */
@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialVariantRepository materialVariantRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs MaterialService with required dependencies.
     *
     * @param materialRepository        repository for material persistence
     * @param materialVariantRepository repository for soft delete protection check
     * @param eventPublisher            Spring event publisher for AI embedding decoupling
     */
    public MaterialService(MaterialRepository materialRepository,
                           MaterialVariantRepository materialVariantRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.materialRepository = materialRepository;
        this.materialVariantRepository = materialVariantRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new material record.
     *
     * <p>Forces null on materialId to prevent client-supplied ID upsert attacks.
     * Publishes {@link MaterialCreatedOrUpdatedEvent} after save to trigger
     * immediate PGVector embedding via {@link sg.projects.stockflowengine.ai.WarehouseEventListener}.
     *
     * @param material the material to create (materialId ignored if provided)
     * @return saved material with auto-generated ID and ACTIVE status
     */
    public Material saveMaterial(Material material) {

        // Force null to prevent client-supplied ID upsert attacks
        material.setMaterialId(null);

        if (material.getStatus() == null || material.getStatus().isEmpty()) {
            material.setStatus("ACTIVE");
        }
        Material saved = materialRepository.save(material);

        // Publish event — WarehouseEventListener handles AI embedding
        eventPublisher.publishEvent(new MaterialCreatedOrUpdatedEvent(saved));
        return saved;
    }

    /**
     * Retrieves all materials in the system.
     *
     * @return list of all materials including INACTIVE ones
     */
    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    /**
     * Retrieves a specific material by ID.
     *
     * @param id the material ID to look up
     * @return Optional containing the material if found, empty if not found
     */
    public Optional<Material> getMaterialById(Long id) {
        return materialRepository.findById(id);
    }

    /**
     * Updates an existing material's name and description.
     *
     * <p>Publishes {@link MaterialCreatedOrUpdatedEvent} after update to
     * re-embed the material with updated details in PGVector.
     *
     * @param id              the ID of the material to update
     * @param updatedMaterial the new material details to apply
     * @return updated material record
     * @throws RuntimeException if no material found with the given ID
     */
    public Material updateMaterial(Long id, Material updatedMaterial) {
        Material existing = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Material not found with id: " + id));
        existing.setMaterialName(updatedMaterial.getMaterialName());
        existing.setDescription(updatedMaterial.getDescription());
        if (updatedMaterial.getStatus() != null) {
            existing.setStatus(updatedMaterial.getStatus());
        }
        Material updated = materialRepository.save(existing);

        // Publish event — WarehouseEventListener re-embeds with updated details
        eventPublisher.publishEvent(new MaterialCreatedOrUpdatedEvent(updated));
        return updated;
    }

    /**
     * Soft deletes a material by setting its status to INACTIVE.
     *
     * <p>Blocked if any ACTIVE variants exist under this material —
     * variants must be deactivated first to maintain inventory hierarchy integrity.
     *
     * <p>Publishes {@link MaterialCreatedOrUpdatedEvent} after deactivation to
     * re-embed the material with INACTIVE status in PGVector.
     *
     * @param id the ID of the material to deactivate
     * @return deactivated material with INACTIVE status
     * @throws RuntimeException         if no material found with the given ID
     * @throws IllegalArgumentException if ACTIVE variants exist under this material
     */
    public Material softDeleteMaterial(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Material not found with id: " + id));

        // Soft delete protection
        boolean hasActiveVariants = materialVariantRepository
                .existsByMaterialIdAndStatus(id, "ACTIVE");
        if (hasActiveVariants) {
            throw new IllegalArgumentException(
                    "Cannot deactivate Material! Active variants exist under it.");
        }
        material.setStatus("INACTIVE");
        Material updated = materialRepository.save(material);

        // Publish event — WarehouseEventListener re-embeds with INACTIVE status
        eventPublisher.publishEvent(new MaterialCreatedOrUpdatedEvent(updated));
        return updated;
    }
}