package sg.projects.stockflowengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sg.projects.stockflowengine.model.Outward;

/**
 * Repository interface for {@link Outward} persistence operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations.
 * Spring Data JPA automatically generates the implementation at runtime.
 *
 * <p>Contains optimized targeted query to replace inefficient
 * {@code findAll()} + stream filtering patterns.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link sg.projects.stockflowengine.service.OutwardService} — outward management
 *       and stock validation</li>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialVariantStockService} — stock
 *       calculation</li>
 * </ul>
 */
public interface OutwardRepository extends JpaRepository<Outward, Long> {

    /**
     * Calculates total dispatched quantity for a specific variant.
     *
     * <p>Sums ALL outward quantities regardless of status — both
     * PENDING_SHIPMENT and COMPLETED outwards are included since:
     * <ul>
     *   <li>PENDING_SHIPMENT → goods reserved, deducted from available stock</li>
     *   <li>COMPLETED → goods shipped, permanently deducted from stock</li>
     * </ul>
     *
     * <p>COALESCE handles the case where no outwards exist,
     * returning 0 instead of null.
     *
     * @param id the material variant ID to sum outwards for
     * @return total dispatched quantity for the variant, 0 if none exist
     */
    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM Outward o WHERE o.materialVariantId = :id")
    int sumQuantityByMaterialVariantId(@Param("id") Long id);
}