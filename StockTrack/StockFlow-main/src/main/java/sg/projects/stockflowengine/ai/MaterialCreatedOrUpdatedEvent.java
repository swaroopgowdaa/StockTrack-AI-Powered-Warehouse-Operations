package sg.projects.stockflowengine.ai;

import sg.projects.stockflowengine.model.Material;

/**
 * Domain event published when a material is created, updated, or deactivated.
 *
 * <p>Published by {@link sg.projects.stockflowengine.service.MaterialService}
 * on the following operations:
 * <ul>
 *   <li>New material created → embed for AI discoverability</li>
 *   <li>Material updated → re-embed with latest name/description</li>
 *   <li>Material deactivated (soft delete) → re-embed with INACTIVE status</li>
 * </ul>
 *
 * <p>Handled by {@link WarehouseEventListener} to keep material records
 * in PGVector synchronized with the database in real time.
 *
 * @see WarehouseEventListener
 * @see sg.projects.stockflowengine.service.MaterialService
 */
public class MaterialCreatedOrUpdatedEvent {

    private final Material material;

    /**
     * Constructs a MaterialCreatedOrUpdatedEvent with the affected material.
     *
     * @param material the material that was created, updated, or deactivated
     */
    public MaterialCreatedOrUpdatedEvent(Material material) {
        this.material = material;
    }

    /**
     * Returns the material associated with this event.
     *
     * @return the created or updated material record
     */
    public Material getMaterial() {
        return material;
    }
}