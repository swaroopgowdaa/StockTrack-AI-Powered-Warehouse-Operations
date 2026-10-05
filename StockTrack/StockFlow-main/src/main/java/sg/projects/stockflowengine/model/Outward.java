package sg.projects.stockflowengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Entity representing an outward transaction — the dispatch of goods from the warehouse.
 *
 * <p>Outward transactions follow a strict status workflow:
 * <pre>
 * PENDING_SHIPMENT → COMPLETED (confirmed by MANAGER/ADMIN)
 * </pre>
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>materialId is auto-populated from the variant's parent — never client-supplied</li>
 *   <li>outwardDate defaults to today if not provided</li>
 *   <li>shippedDate is auto-stamped when status changes to COMPLETED</li>
 *   <li>status defaults to PENDING_SHIPMENT on creation</li>
 *   <li>outwardId is always auto-generated — client-supplied IDs are ignored</li>
 *   <li>Stock is validated before saving — blocks if quantity exceeds available stock</li>
 * </ul>
 *
 * <p>Stock calculation impact:
 * <ul>
 *   <li>PENDING_SHIPMENT outwards deduct from available stock immediately on creation</li>
 *   <li>This prevents over-committing stock that is already reserved for shipment</li>
 * </ul>
 *
 * <p>Mapped to the {@code outward} table in PostgreSQL.
 *
 * @see sg.projects.stockflowengine.service.OutwardService
 * @see sg.projects.stockflowengine.model.MaterialVariant
 * @see sg.projects.stockflowengine.model.Customer
 */
@Entity
@Table(name="Outward")
public class Outward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outwardId;

    /** Auto-populated from the variant's parent material — never set by client */
    private Long materialId;

    @NotNull(message = "Material Variant ID is required.")
    private Long materialVariantId;

    @NotNull(message = "Customer ID is required.")
    private Long customerId;

    /** Optional purpose or reason for this outward dispatch */
    private String purpose;

    /** Optional serial number for tracking individual items */
    private String serialNo;

    /** Name of the person who created the outward dispatch request */
    @NotBlank(message = "Outward by: is required.")
    private String outwardBy;

    /** Date outward request was created — defaults to today if not provided */
    private LocalDate outwardDate;

    /** Date goods were physically shipped — auto-stamped on COMPLETED status */
    private LocalDate shippedDate;

    /**
     * Current status in the outward workflow.
     * PENDING_SHIPMENT → COMPLETED
     */
    private String status;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be greater than zero.")
    private Integer quantity;

    /**
     * Default no-args constructor required by JPA.
     */
    public Outward() {}

    /** @return the auto-generated outward transaction ID */
    public Long getOutwardId() { return outwardId; }

    /** @param outwardId the outward ID to set (forced null in service to prevent upsert) */
    public void setOutwardId(Long outwardId) { this.outwardId = outwardId; }

    /** @return the parent material ID (auto-populated from variant) */
    public Long getMaterialId() { return materialId; }

    /** @param materialId the material ID to set */
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    /** @return the material variant ID this outward is recorded against */
    public Long getMaterialVariantId() { return materialVariantId; }

    /** @param variantId the variant ID to set */
    public void setMaterialVariantId(Long variantId) { this.materialVariantId = variantId; }

    /** @return the customer ID receiving this shipment */
    public Long getCustomerId() { return customerId; }

    /** @param customerId the customer ID to set */
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    /** @return the optional purpose of this dispatch */
    public String getPurpose() { return purpose; }

    /** @param purpose the purpose to set */
    public void setPurpose(String purpose) { this.purpose = purpose; }

    /** @return the optional serial number */
    public String getSerialNo() { return serialNo; }

    /** @param serialNo the serial number to set */
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    /** @return the name of the person who created the outward request */
    public String getOutwardBy() { return outwardBy; }

    /** @param outwardBy the creator's name to set */
    public void setOutwardBy(String outwardBy) { this.outwardBy = outwardBy; }

    /** @return the date the outward request was created */
    public LocalDate getOutwardDate() { return outwardDate; }

    /** @param outwardDate the outward date to set */
    public void setOutwardDate(LocalDate outwardDate) { this.outwardDate = outwardDate; }

    /** @return the date goods were physically shipped (null until COMPLETED) */
    public LocalDate getShippedDate() { return shippedDate; }

    /** @param shippedDate the shipped date to set (auto-stamped on completion) */
    public void setShippedDate(LocalDate shippedDate) { this.shippedDate = shippedDate; }

    /** @return the current workflow status */
    public String getStatus() { return status; }

    /** @param status the status to set */
    public void setStatus(String status) { this.status = status; }

    /** @return the quantity of goods dispatched */
    public Integer getQuantity() { return quantity; }

    /** @param quantity the quantity to set */
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}