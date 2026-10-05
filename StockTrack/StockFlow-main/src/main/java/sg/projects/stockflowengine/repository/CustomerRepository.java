package sg.projects.stockflowengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.projects.stockflowengine.model.Customer;

/**
 * Repository interface for {@link Customer} persistence operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations.
 * Spring Data JPA automatically generates the implementation at runtime.
 *
 * <p>Used by {@link sg.projects.stockflowengine.service.CustomerService}
 * for all customer persistence operations including create, read,
 * update and soft delete.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}