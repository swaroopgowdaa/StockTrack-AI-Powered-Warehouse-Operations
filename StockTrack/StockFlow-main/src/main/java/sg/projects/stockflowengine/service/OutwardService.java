package sg.projects.stockflowengine.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import sg.projects.stockflowengine.ai.OutwardCompletedEvent;
import sg.projects.stockflowengine.ai.OutwardCreatedEvent;
import sg.projects.stockflowengine.model.Customer;
import sg.projects.stockflowengine.model.MaterialVariant;
import sg.projects.stockflowengine.model.Outward;
import sg.projects.stockflowengine.repository.CustomerRepository;
import sg.projects.stockflowengine.repository.InwardRepository;
import sg.projects.stockflowengine.repository.MaterialVariantRepository;
import sg.projects.stockflowengine.repository.OutwardRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for outward transaction business logic.
 *
 * <p>Outward transactions represent the dispatch of goods from the warehouse
 * to customers. This service enforces the outward status workflow and validates
 * all business rules before persisting transactions.
 *
 * <p>Status workflow enforced:
 * <pre>
 * PENDING_SHIPMENT → COMPLETED (confirmed by MANAGER/ADMIN)
 * </pre>
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Variant must exist and be ACTIVE before dispatching</li>
 *   <li>Customer must exist and be ACTIVE before dispatching</li>
 *   <li>Quantity must be greater than zero</li>
 *   <li>Requested quantity must not exceed current available stock</li>
 *   <li>Only PENDING_SHIPMENT outwards can be completed</li>
 *   <li>materialId auto-populated from variant — never client-supplied</li>
 *   <li>outwardDate defaults to today if not provided</li>
 *   <li>shippedDate auto-stamped on completion</li>
 *   <li>outwardId forced null to prevent upsert attacks</li>
 * </ul>
 *
 * <p>Stock validation uses real-time calculation:
 * <pre>
 * Available Stock = SUM(ACTIVE inwards) - SUM(all outwards)
 * </pre>
 * PENDING_SHIPMENT outwards are included in the deduction — reserved
 * stock cannot be double-committed to multiple shipments.
 *
 * <p>AI integration via Spring Events :
 * <ul>
 *   <li>On create → publishes {@link OutwardCreatedEvent}</li>
 *   <li>On complete → publishes {@link OutwardCompletedEvent}</li>
 * </ul>
 *
 * @see OutwardRepository
 * @see sg.projects.stockflowengine.ai.WarehouseEventListener
 */
@Service
public class OutwardService {

    private final OutwardRepository outwardRepository;
    private final InwardRepository inwardRepository;
    private final MaterialVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs OutwardService with required dependencies.
     *
     * @param outwardRepository repository for outward persistence
     * @param inwardRepository  repository for stock calculation (ACTIVE inward sum)
     * @param variantRepository repository for variant validation
     * @param customerRepository repository for customer validation
     * @param eventPublisher    Spring event publisher for AI embedding decoupling
     */
    public OutwardService(OutwardRepository outwardRepository,
                          InwardRepository inwardRepository,
                          MaterialVariantRepository variantRepository,
                          CustomerRepository customerRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.outwardRepository = outwardRepository;
        this.inwardRepository = inwardRepository;
        this.variantRepository = variantRepository;
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new outward transaction for goods to be dispatched.
     *
     * <p>Validates variant and customer existence and ACTIVE status.
     * Performs real-time stock validation before saving — blocks if
     * requested quantity exceeds currently available stock.
     *
     * <p>Publishes {@link OutwardCreatedEvent} after successful save to
     * trigger immediate PGVector embedding via
     * {@link sg.projects.stockflowengine.ai.WarehouseEventListener}.
     *
     * @param outward the outward transaction to create
     * @return saved outward with PENDING_SHIPMENT status and auto-populated materialId
     * @throws IllegalArgumentException if variant/customer doesn't exist or is INACTIVE,
     *                                  quantity is invalid, or stock is insufficient
     */
    public Outward saveOutward(Outward outward) {

        // Force null to prevent client-supplied ID upsert attacks
        outward.setOutwardId(null);

        if (outward.getQuantity() == null || outward.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be specified and greater than zero!");
        }

        // Single DB call — validates existence and retrieves materialId in one query
        MaterialVariant variant = variantRepository.findById(outward.getMaterialVariantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Variant! Cannot outward a material that doesn't exist."));

        if ("INACTIVE".equals(variant.getStatus())) {
            throw new IllegalArgumentException(
                    "Cannot outward against an INACTIVE variant!");
        }

        // Validate customer exists and is active
        Customer customer = customerRepository.findById(outward.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Customer! Cannot ship to an unknown customer."));

        if ("INACTIVE".equals(customer.getStatus())) {
            throw new IllegalArgumentException(
                    "Cannot ship to an INACTIVE customer!");
        }

        // Auto-populate materialId from variant — never trust client-supplied value
        outward.setMaterialId(variant.getMaterialId());

        // Real-time stock calculation — prevents over-committing reserved stock
        int totalInward = inwardRepository.sumActiveQuantityByMaterialVariantId(
                outward.getMaterialVariantId());
        int totalOutward = outwardRepository.sumQuantityByMaterialVariantId(
                outward.getMaterialVariantId());
        int currentStock = totalInward - totalOutward;

        if (outward.getQuantity() > currentStock) {
            throw new IllegalArgumentException(
                    "Insufficient Stock! You only have " + currentStock + " units available.");
        }

        // Default outward date to today if not provided
        if (outward.getOutwardDate() == null) {
            outward.setOutwardDate(LocalDate.now());
        }
        if (outward.getStatus() == null || outward.getStatus().isEmpty()) {
            outward.setStatus("PENDING_SHIPMENT");
        }

        Outward saved = outwardRepository.save(outward);

        // Publish event — WarehouseEventListener handles AI embedding
        eventPublisher.publishEvent(new OutwardCreatedEvent(saved));
        return saved;
    }

    /**
     * Marks a PENDING_SHIPMENT outward as COMPLETED, confirming physical dispatch.
     *
     * <p>Auto-stamps shippedDate with today's date on completion.
     * Publishes {@link OutwardCompletedEvent} after completion to trigger
     * re-embedding of the outward record and stock level recalculation
     * in PGVector via {@link sg.projects.stockflowengine.ai.WarehouseEventListener}.
     *
     * @param id the outward transaction ID to complete
     * @return completed outward with COMPLETED status and shippedDate stamped
     * @throws IllegalArgumentException if outward not found or not in PENDING_SHIPMENT status
     */
    public Outward completeOutward(Long id) {
        Outward outward = outwardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outward not found."));

        // Only PENDING_SHIPMENT outwards can be completed
        if (!"PENDING_SHIPMENT".equals(outward.getStatus())) {
            throw new IllegalArgumentException(
                    "Only PENDING_SHIPMENT outwards can be completed.");
        }

        outward.setStatus("COMPLETED");

        // Auto-stamp shipped date on completion
        outward.setShippedDate(LocalDate.now());
        Outward completed = outwardRepository.save(outward);

        // Publish event — WarehouseEventListener re-embeds outward and updates stock
        eventPublisher.publishEvent(new OutwardCompletedEvent(completed));
        return completed;
    }

    /**
     * Retrieves all outward transactions in the system.
     *
     * @return list of all outwards regardless of status
     */
    public List<Outward> getAllOutwards() {
        return outwardRepository.findAll();
    }

    /**
     * Retrieves a specific outward transaction by ID.
     *
     * @param id the outward transaction ID to look up
     * @return Optional containing the outward if found, empty if not found
     */
    public Optional<Outward> getOutwardById(Long id) {
        return outwardRepository.findById(id);
    }
}