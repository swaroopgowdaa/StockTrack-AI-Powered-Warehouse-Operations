package sg.projects.stockflowengine.service;

import org.springframework.stereotype.Service;
import sg.projects.stockflowengine.model.Customer;
import sg.projects.stockflowengine.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for customer management business logic.
 *
 * <p>Customers represent the recipients of outward shipments.
 * INACTIVE customers cannot receive new outward transactions,
 * preventing shipments to discontinued or invalid recipients.
 *
 * <p>All customer IDs are auto-generated — client-supplied IDs
 * are forced null to prevent upsert attacks.
 *
 * @see CustomerRepository
 * @see sg.projects.stockflowengine.model.Customer
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Constructs CustomerService with the customer repository.
     *
     * @param customerRepository repository for customer persistence operations
     */
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Creates a new customer record.
     *
     * <p>Forces null on customerId to prevent client-supplied ID upsert attacks.
     * Defaults status to ACTIVE if not provided.
     *
     * @param customer the customer to create (customerId ignored if provided)
     * @return created customer with auto-generated ID and ACTIVE status
     */
    public Customer saveCustomer(Customer customer) {

        // Force null to prevent client-supplied ID upsert attacks
        customer.setCustomerId(null);

        if (customer.getStatus() == null || customer.getStatus().isEmpty()) {
            customer.setStatus("ACTIVE");
        }
        return customerRepository.save(customer);
    }

    /**
     * Retrieves all customers in the system.
     *
     * @return list of all customers including INACTIVE ones
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    /**
     * Retrieves a specific customer by ID.
     *
     * @param id the customer ID to look up
     * @return Optional containing the customer if found, empty if not found
     */
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    /**
     * Updates an existing customer's name and code.
     *
     * @param id              the ID of the customer to update
     * @param updatedCustomer the new customer details to apply
     * @return updated customer record
     * @throws RuntimeException if no customer found with the given ID
     */
    public Customer updateCustomer(Long id, Customer updatedCustomer) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with id: " + id));
        existing.setCustomerName(updatedCustomer.getCustomerName());
        existing.setCustomerCode(updatedCustomer.getCustomerCode());
        if (updatedCustomer.getStatus() != null) {
            existing.setStatus(updatedCustomer.getStatus());
        }
        return customerRepository.save(existing);
    }

    /**
     * Soft deletes a customer by setting their status to INACTIVE.
     *
     * <p>INACTIVE customers cannot receive new outward shipments.
     * Customer record is preserved for historical outward transaction records.
     *
     * @param id the ID of the customer to deactivate
     * @return deactivated customer with INACTIVE status
     * @throws RuntimeException if no customer found with the given ID
     */
    public Customer softDeleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setStatus("INACTIVE");
        return customerRepository.save(customer);
    }
}