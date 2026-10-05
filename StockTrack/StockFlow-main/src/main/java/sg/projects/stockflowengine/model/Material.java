package sg.projects.stockflowengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing a material in the warehouse inventory system.
 *
 * <p>Materials are the top-level entity in the inventory hierarchy:
 * <pre>
 * Material → MaterialVariant → Inward/Outward transactions
 * </pre>
 *
 * <p>Examples: "Fiber Laser Source", "Steel Rod", "Copper Wire"
 *
 * <p>Business rules:
 * <ul>
 *   <li>Material names must be unique across the system</li>
 *   <li>Cannot be deactivated if active variants exist under it</li>
 *   <li>Soft deleted (INACTIVE) rather than hard deleted to preserve history</li>
 *   <li>materialId is always auto-generated — client-supplied IDs are ignored</li>
 * </ul>
 *
 * <p>Mapped to the {@code material_master} table in PostgreSQL.
 *
 * @see sg.projects.stockflowengine.service.MaterialService
 * @see sg.projects.stockflowengine.model.MaterialVariant
 */
@Entity
@Table(name="MaterialMaster")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialId;

    /** Must be unique across the system — duplicate names return 400 */
    @NotBlank(message = "Material name is required.")
    @Column(unique = true)
    private String materialName;

    /** Optional description of the material */
    private String description;

    /** ACTIVE or INACTIVE — INACTIVE materials cannot have new variants */
    private String status;

    /**
     * Default no-args constructor required by JPA.
     */
    public Material() {}

    /**
     * Constructs a Material with all fields.
     *
     * @param materialId   auto-generated material ID
     * @param materialName unique material name
     * @param description  optional material description
     * @param status       material status (ACTIVE or INACTIVE)
     */
    public Material(Long materialId, String materialName, String description, String status) {
        this.materialId = materialId;
        this.materialName = materialName;
        this.description = description;
        this.status = status;
    }

    /** @return the auto-generated material ID */
    public Long getMaterialId() { return materialId; }

    /** @param materialId the material ID to set (forced null in service to prevent upsert) */
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    /** @return the unique material name */
    public String getMaterialName() { return materialName; }

    /** @param materialName the material name to set */
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    /** @return the material status (ACTIVE or INACTIVE) */
    public String getStatus() { return status; }

    /** @param status the status to set */
    public void setStatus(String status) { this.status = status; }

    /** @return the optional material description */
    public String getDescription() { return description; }

    /** @param description the description to set */
    public void setDescription(String description) { this.description = description; }
}