package sg.projects.stockflowengine.dto;

/**
 * Data Transfer Object representing real-time stock levels for a material variant.
 *
 * <p>Stock is never stored as a static value — it is always calculated dynamically:
 * <pre>
 * Current Stock = Total ACTIVE Inwards - Total COMPLETED Outwards
 * </pre>
 *
 * <p>This DTO is populated by {@link sg.projects.stockflowengine.service.MaterialVariantStockService}
 * and used in both API responses and PGVector embeddings for AI stock queries.
 *
 * @see sg.projects.stockflowengine.service.MaterialVariantStockService
 * @see sg.projects.stockflowengine.controller.MaterialVariantStockController
 */
public class MaterialVariantStockDto {

    private Long materialVariantId;
    private int totalInward;
    private int totalOutward;
    private int currentStock;
    private String materialVariantName;

    /**
     * Constructs MaterialVariantStockDto with calculated stock figures.
     *
     * @param materialVariantId   the variant's unique identifier
     * @param materialVariantName the variant's display name
     * @param totalInward         sum of all ACTIVE inward quantities for this variant
     * @param totalOutward        sum of all outward quantities for this variant
     * @param currentStock        calculated available stock (totalInward - totalOutward)
     */
    public MaterialVariantStockDto(Long materialVariantId, String materialVariantName,
                                   int totalInward, int totalOutward, int currentStock) {
        this.materialVariantId = materialVariantId;
        this.totalInward = totalInward;
        this.totalOutward = totalOutward;
        this.currentStock = currentStock;
        this.materialVariantName = materialVariantName;
    }

    /**
     * Returns the variant's unique identifier.
     *
     * @return material variant ID
     */
    public Long getMaterialVariantId() { return materialVariantId; }

    /**
     * Sets the variant's unique identifier.
     *
     * @param materialVariantId the variant ID to set
     */
    public void setMaterialVariantId(Long materialVariantId) {
        this.materialVariantId = materialVariantId;
    }

    /**
     * Returns the total quantity of ACTIVE approved inwards for this variant.
     *
     * @return total inward quantity
     */
    public int getTotalInward() { return totalInward; }

    /**
     * Sets the total inward quantity.
     *
     * @param totalInward total ACTIVE inward quantity
     */
    public void setTotalInward(int totalInward) { this.totalInward = totalInward; }

    /**
     * Returns the total quantity dispatched via outward transactions.
     *
     * @return total outward quantity
     */
    public int getTotalOutward() { return totalOutward; }

    /**
     * Sets the total outward quantity.
     *
     * @param totalOutward total outward quantity
     */
    public void setTotalOutward(int totalOutward) { this.totalOutward = totalOutward; }

    /**
     * Returns the current available stock level.
     * Calculated as totalInward - totalOutward.
     *
     * @return current stock quantity
     */
    public int getCurrentStock() { return currentStock; }

    /**
     * Sets the current stock level.
     *
     * @param currentStock calculated stock quantity
     */
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    /**
     * Returns the variant's display name.
     *
     * @return material variant name
     */
    public String getMaterialVariantName() { return materialVariantName; }

    /**
     * Sets the variant's display name.
     *
     * @param materialVariantName the variant name to set
     */
    public void setMaterialVariantName(String materialVariantName) {
        this.materialVariantName = materialVariantName;
    }
}