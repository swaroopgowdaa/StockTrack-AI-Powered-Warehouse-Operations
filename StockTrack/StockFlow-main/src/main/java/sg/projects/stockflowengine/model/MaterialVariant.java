package sg.projects.stockflowengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a specific variant of a material in the warehouse.
 *
 * <p>Variants sit in the middle of the inventory hierarchy:
 * <pre>
 * Material → MaterialVariant → Inward/Outward transactions
 * </pre>
 *
 * <p>Examples: Material "Steel Rod" may have variants
 * "12mm Standard Rod" and "16mm Heavy Rod".
 *
 * <p>Business rules:
 * <ul>
 *   <li>Must belong to an existing parent material</li>
 *   <li>Variant name must be unique within the same parent material
 *       (composite unique constraint on materialId + materialVariantName)</li>
 *   <li>Cannot be deactivated if PENDING_INSPECTION or ACTIVE inwards exist</li>
 *   <li>INACTIVE variants cannot receive new inwards or outwards</li>
 *   <li>Soft deleted (INACTIVE) rather than hard deleted to preserve history</li>
 *   <li>materialVariantId is always auto-generated — client-supplied IDs are ignored</li>
 * </ul>
 *
 * <p>Mapped to the {@code material_variant_master} table in PostgreSQL.
 *
 * @see sg.projects.stockflowengine.service.MaterialVariantService
 * @see sg.projects.stockflowengine.model.Material
 */
@Entity
@Table(name = "MaterialVariantMaster", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"materialId", "materialVariantName"})
})
public class MaterialVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialVariantId;

    /**
     * Foreign key linking this variant to its parent material.
     * Must reference an existing material — validated in service layer.
     */
    @NotNull(message = "Parent Material ID is absolutely required to create a Variant.")
    private Long materialId;

    /**
     * Unique variant name within the parent material.
     * Composite unique constraint enforced at DB level.
     */
    @NotBlank(message = "Variant name is required.")
    private String materialVariantName;

    /** ACTIVE or INACTIVE — INACTIVE variants block new inwards and outwards */
    private String status;

    /**
     * Default no-args constructor required by JPA.
     */
    public MaterialVariant() {}

    /**
     * Constructs a MaterialVariant with all fields.
     *
     * @param materialVariantId   auto-generated variant ID
     * @param materialId          parent material ID (FK)
     * @param materialVariantName unique variant name within parent material
     * @param status              variant status (ACTIVE or INACTIVE)
     */
    public MaterialVariant(Long materialVariantId, Long materialId,
                           String materialVariantName, String status) {
        this.materialVariantId = materialVariantId;
        this.materialId = materialId;
        this.materialVariantName = materialVariantName;
        this.status = status;
    }

    /** @return the auto-generated variant ID */
    public Long getMaterialVariantId() { return materialVariantId; }

    /** @param materialVariantId the variant ID to set (forced null in service to prevent upsert) */
    public void setMaterialVariantId(Long materialVariantId) {
        this.materialVariantId = materialVariantId;
    }

    /** @return the parent material ID */
    public Long getMaterialId() { return materialId; }

    /** @param materialId the parent material ID to set */
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    /** @return the variant name */
    public String getMaterialVariantName() { return materialVariantName; }

    /** @param materialVariantName the variant name to set */
    public void setMaterialVariantName(String materialVariantName) {
        this.materialVariantName = materialVariantName;
    }

    /** @return the variant status (ACTIVE or INACTIVE) */
    public String getStatus() { return status; }

    /** @param status the status to set */
    public void setStatus(String status) { this.status = status; }
}