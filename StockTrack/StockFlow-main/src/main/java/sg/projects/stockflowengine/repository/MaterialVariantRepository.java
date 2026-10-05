package sg.projects.stockflowengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.projects.stockflowengine.model.MaterialVariant;

/**
 * Repository interface for {@link MaterialVariant} persistence operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations.
 * Spring Data JPA automatically generates the implementation at runtime.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialVariantService} — variant management</li>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialService} — soft delete protection</li>
 *   <li>{@link sg.projects.stockflowengine.service.InwardService} — variant validation</li>
 *   <li>{@link sg.projects.stockflowengine.service.OutwardService} — variant validation</li>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialVariantStockService} — stock calculation</li>
 * </ul>
 */
public interface MaterialVariantRepository extends JpaRepository<MaterialVariant, Long> {

    /**
     * Checks if any variants exist for a material with the given status.
     *
     * <p>Used by soft delete protection in
     * {@link sg.projects.stockflowengine.service.MaterialService} to prevent
     * deactivating a material that still has ACTIVE variants under it,
     * maintaining inventory hierarchy integrity.
     *
     * <p>Spring Data JPA automatically generates:
     * <pre>
     * SELECT COUNT(*) > 0 FROM material_variant_master
     * WHERE material_id = ? AND status = ?
     * </pre>
     *
     * @param materialId the parent material ID to check
     * @param status     the status to filter by (typically "ACTIVE")
     * @return true if any variants exist with the given status, false otherwise
     */
    boolean existsByMaterialIdAndStatus(Long materialId, String status);
}
