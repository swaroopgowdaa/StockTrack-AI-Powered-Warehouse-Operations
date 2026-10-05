package sg.projects.stockflowengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.projects.stockflowengine.model.Material;

/**
 * Repository interface for {@link Material} persistence operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations.
 * Spring Data JPA automatically generates the implementation at runtime.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialService} — material management</li>
 *   <li>{@link sg.projects.stockflowengine.service.MaterialVariantService} — parent
 *       material validation on variant creation</li>
 * </ul>
 */
public interface MaterialRepository extends JpaRepository<Material, Long> {
}