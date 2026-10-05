package sg.projects.stockflowengine.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.model.Customer;
import sg.projects.stockflowengine.service.CustomerService;
import java.util.List;

/**
 * REST controller for managing customers in the warehouse system.
 *
 * <p>Customers represent the recipients of outward shipments.
 * An outward transaction cannot be created for an INACTIVE customer.
 *
 * <p>Accessible by TECHNICAL_STAFF, MANAGER and ADMIN roles.
 * All endpoints require a valid JWT token.
 *
 * @see CustomerService
 * @see sg.projects.stockflowengine.model.Customer
 */
@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Constructs CustomerController with the customer management service.
     *
     * @param customerService service handling customer business logic and persistence
     */
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Creates a new customer record.
     *
     * <p>Customer status defaults to ACTIVE if not provided.
     * Client-supplied customerId is ignored — ID is always auto-generated.
     *
     * @param customer the customer details to create
     * @return created customer with auto-generated ID and ACTIVE status
     */
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.saveCustomer(customer));
    }

    /**
     * Retrieves all customers in the system.
     *
     * @return list of all customers including INACTIVE ones
     */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    /**
     * Retrieves a specific customer by ID.
     *
     * @param id the customer ID to look up
     * @return customer details, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing customer's details.
     *
     * @param id       the ID of the customer to update
     * @param customer the updated customer details
     * @return updated customer record
     */
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id,
                                                   @RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    /**
     * Soft deletes a customer by setting their status to INACTIVE.
     *
     * <p>INACTIVE customers cannot receive new outward shipments.
     * Customer record is preserved for historical outward records.
     *
     * @param id the ID of the customer to deactivate
     * @return deactivated customer with INACTIVE status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Customer> deleteCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.softDeleteCustomer(id));
    }
}