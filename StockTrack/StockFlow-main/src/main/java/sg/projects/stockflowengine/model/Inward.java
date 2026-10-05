package sg.projects.stockflowengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Entity representing an inward transaction — the receipt of goods into the warehouse.
 *
 * <p>Inward transactions follow a strict status workflow:
 * <pre>
 * PENDING_INSPECTION → ACTIVE   (approved by MANAGER/ADMIN)
 * PENDING_INSPECTION → REJECTED (rejected by MANAGER/ADMIN)
 * </pre>
 *
 * <p>Only ACTIVE inwards contribute to stock calculations.
 * PENDING_INSPECTION inwards are quarantined until a supervisor verifies
 * the goods, preventing unverified stock from being dispatched.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>materialId is auto-populated from the variant's parent — never client-supplied</li>
 *   <li>inwardDate defaults to today if not provided</li>
 *   <li>status defaults to PENDING_INSPECTION on creation</li>
 *   <li>inwardId is always auto-generated — client-supplied IDs are ignored</li>
 * </ul>
 *
 * <p>Mapped to the {@code inward} table in PostgreSQL.
 *
 * @see sg.projects.stockflowengine.service.InwardService
 * @see sg.projects.stockflowengine.model.MaterialVariant
 */
@Entity
@Table(name="Inward")
public class Inward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inwardId;

    /** Auto-populated from the variant's parent material — never set by client */
    private Long materialId;

    @NotNull(message = "MaterialVariant ID is required.")
    private Long materialVariantId;

    /** Type of inward (e.g. VENDOR_DELIVERY, RETURN, TRANSFER) */
    private String inwardType;

    /** Optional description or notes for this inward transaction */
    private String description;

    /** Name of the person who received and recorded the goods */
    private String inwardBy;

    /** Date goods were received — defaults to today if not provided */
    private LocalDate inwardDate;

    /** Optional serial number for tracking individual items */
    private String serialNo;

    /**
     * Current status in the inward workflow.
     * PENDING_INSPECTION → ACTIVE or REJECTED
     */
    private String status;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be greater than zero.")
    private Integer quantity;

    /** Physical condition of received goods (e.g. NEW, USED, DAMAGED) */
    private String itemCondition;

    /**
     * Default no-args constructor required by JPA.
     */
    public Inward() {}

    /** @return the auto-generated inward transaction ID */
    public Long getInwardId() { return inwardId; }

    /** @param inwardId the inward ID to set (forced null in service to prevent upsert) */
    public void setInwardId(Long inwardId) { this.inwardId = inwardId; }

    /** @return the parent material ID (auto-populated from variant) */
    public Long getMaterialId() { return materialId; }

    /** @param materialId the material ID to set */
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    /** @return the material variant ID this inward is recorded against */
    public Long getMaterialVariantId() { return materialVariantId; }

    /** @param materialVariantId the variant ID to set */
    public void setMaterialVariantId(Long materialVariantId) {
        this.materialVariantId = materialVariantId;
    }

    /** @return the type of inward transaction */
    public String getInwardType() { return inwardType; }

    /** @param inwardType the inward type to set */
    public void setInwardType(String inwardType) { this.inwardType = inwardType; }

    /** @return optional description or notes */
    public String getDescription() { return description; }

    /** @param description the description to set */
    public void setDescription(String description) { this.description = description; }

    /** @return the name of the person who recorded the inward */
    public String getInwardBy() { return inwardBy; }

    /** @param inwardBy the recorder's name to set */
    public void setInwardBy(String inwardBy) { this.inwardBy = inwardBy; }

    /** @return the date goods were received */
    public LocalDate getInwardDate() { return inwardDate; }

    /** @param inwardDate the receipt date to set */
    public void setInwardDate(LocalDate inwardDate) { this.inwardDate = inwardDate; }

    /** @return optional serial number */
    public String getSerialNo() { return serialNo; }

    /** @param serialNo the serial number to set */
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    /** @return the quantity of goods received */
    public Integer getQuantity() { return quantity; }

    /** @param quantity the quantity to set */
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    /** @return the physical condition of received goods */
    public String getItemCondition() { return itemCondition; }

    /** @param itemCondition the item condition to set */
    public void setItemCondition(String itemCondition) { this.itemCondition = itemCondition; }

    /** @return the current workflow status */
    public String getStatus() { return status; }

    /** @param status the status to set */
    public void setStatus(String status) { this.status = status; }
}