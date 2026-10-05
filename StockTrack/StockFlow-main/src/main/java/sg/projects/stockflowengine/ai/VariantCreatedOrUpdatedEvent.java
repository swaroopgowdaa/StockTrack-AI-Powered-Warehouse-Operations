package sg.projects.stockflowengine.ai;

import sg.projects.stockflowengine.model.MaterialVariant;

/**
 * Domain event published when a material variant is created, updated, or deactivated.
 *
 * <p>Published by {@link sg.projects.stockflowengine.service.MaterialVariantService}
 * on the following operations:
 * <ul>
 *   <li>New variant created → embed for AI discoverability</li>
 *   <li>Variant updated → re-embed with latest name or parent material</li>
 *   <li>Variant deactivated (soft delete) → re-embed with INACTIVE status</li>
 * </ul>
 *
 * <p>Handled by {@link WarehouseEventListener} to keep variant records
 * in PGVector synchronized with the database in real time, enabling
 * queries such as "what variants exist for Steel Rod?" or
 * "is 16mm Heavy Rod an active variant?".
 *
 * @see WarehouseEventListener
 * @see sg.projects.stockflowengine.service.MaterialVariantService
 */
public class VariantCreatedOrUpdatedEvent {

    private final MaterialVariant variant;

    /**
     * Constructs a VariantCreatedOrUpdatedEvent with the affected variant.
     *
     * @param variant the material variant that was created, updated, or deactivated
     */
    public VariantCreatedOrUpdatedEvent(MaterialVariant variant) {
        this.variant = variant;
    }

    /**
     * Returns the material variant associated with this event.
     *
     * @return the created or updated material variant record
     */
    public MaterialVariant getVariant() {
        return variant;
    }
}