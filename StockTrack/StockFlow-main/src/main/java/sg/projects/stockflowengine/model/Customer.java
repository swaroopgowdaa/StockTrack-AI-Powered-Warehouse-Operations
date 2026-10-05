package sg.projects.stockflowengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing a customer who receives outward shipments.
 *
 * <p>Customers are the recipients of goods dispatched from the warehouse.
 * An outward transaction cannot be created for an INACTIVE customer,
 * preventing shipments to discontinued or invalid recipients.
 *
 * <p>Mapped to the {@code customer_master} table in PostgreSQL.
 *
 * @see sg.projects.stockflowengine.service.CustomerService
 * @see sg.projects.stockflowengine.model.Outward
 */
@Entity
@Table(name="CustomerMaster")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @NotBlank(message = "Customer name is required.")
    private String customerName;

    /** Unique customer code for identification (e.g. CUST001) */
    @NotBlank(message = "Customer code is required.")
    private String customerCode;

    /** ACTIVE or INACTIVE — INACTIVE customers cannot receive outward shipments */
    private String status;

    /**
     * Default no-args constructor required by JPA.
     */
    public Customer() {}

    /**
     * Constructs a Customer with all fields.
     *
     * @param customerId   auto-generated customer ID
     * @param customerName the customer's display name
     * @param customerCode unique identifier code for the customer
     * @param status       account status (ACTIVE or INACTIVE)
     */
    public Customer(Long customerId, String customerName, String customerCode, String status) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.status = status;
    }

    /** @return the auto-generated customer ID */
    public Long getCustomerId() { return customerId; }

    /** @param customerId the customer ID to set */
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    /** @return the customer's display name */
    public String getCustomerName() { return customerName; }

    /** @param customerName the customer name to set */
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    /** @return the unique customer code */
    public String getCustomerCode() { return customerCode; }

    /** @param customerCode the customer code to set */
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    /** @return the account status (ACTIVE or INACTIVE) */
    public String getStatus() { return status; }

    /** @param status the status to set */
    public void setStatus(String status) { this.status = status; }
}
