package sg.projects.stockflowengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sg.projects.stockflowengine.model.Inward;

import java.util.List;

/**
 * Repository interface for {@link Inward} persistence operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations.
 * Spring Data JPA automatically generates the implementation at runtime.
 *
 * <p>Contains optimized targeted queries to replace inefficient
 * {@code findAll()} + stream filtering patterns.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link sg.projects.stockflowengine.service.InwardService} — inward management</li>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialVariantService} — soft delete protection</li>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialVariantStockService} — stock calculation</li>
 *   <li>{@link sg.projects.stockflowengine.service.OutwardService} — stock validation</li>
 * </ul>
 */
public interface InwardRepository extends JpaRepository<Inward, Long> {

    /**
     * Checks if any inward records exist for a variant with the given statuses.
     *
     * <p>Used by soft delete protection to prevent deactivating a variant
     * that has PENDING_INSPECTION or ACTIVE inwards — live stock cannot be orphaned.
     *
     * <p>Spring Data JPA automatically generates:
     * <pre>
     * SELECT COUNT(*) > 0 FROM inward
     * WHERE material_variant_id = ? AND status IN (?, ?)
     * </pre>
     *
     * @param materialVariantId the variant ID to check
     * @param statuses          list of statuses to check against (e.g. PENDING_INSPECTION, ACTIVE)
     * @return true if any matching inward records exist, false otherwise
     */
    boolean existsByMaterialVariantIdAndStatusIn(Long materialVariantId, List<String> statuses);

    /**
     * Calculates total approved inward quantity for a specific variant.
     *
     * <p>Only counts ACTIVE inwards — PENDING_INSPECTION and REJECTED
     * inwards are excluded from stock calculations, ensuring only
     * verified goods count towards available stock.
     *
     * <p>COALESCE handles the case where no ACTIVE inwards exist,
     * returning 0 instead of null.
     *
     * @param id the material variant ID to sum inwards for
     * @return total quantity of ACTIVE inwards for the variant, 0 if none exist
     */
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inward i WHERE i.materialVariantId = :id AND i.status = 'ACTIVE'")
    int sumActiveQuantityByMaterialVariantId(@Param("id") Long id);
}