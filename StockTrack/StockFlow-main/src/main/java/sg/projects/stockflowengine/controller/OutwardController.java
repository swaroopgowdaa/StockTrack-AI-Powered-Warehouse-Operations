package sg.projects.stockflowengine.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.model.Outward;
import sg.projects.stockflowengine.service.OutwardService;

import java.util.List;

/**
 * REST controller for managing outward transactions in the warehouse.
 *
 * <p>Outward transactions represent the dispatch of goods from the warehouse
 * to customers. They follow a strict status workflow:
 * <pre>
 * PENDING_SHIPMENT → COMPLETED (confirmed by MANAGER/ADMIN)
 * </pre>
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Cannot outward against an INACTIVE variant</li>
 *   <li>Cannot outward to an INACTIVE customer</li>
 *   <li>Cannot outward more than current available stock</li>
 *   <li>Only PENDING_SHIPMENT outwards can be completed</li>
 * </ul>
 *
 * <p>Access control:
 * <ul>
 *   <li>POST /api/outwards → TECHNICAL_STAFF, MANAGER, ADMIN</li>
 *   <li>GET /api/outwards → TECHNICAL_STAFF, MANAGER, ADMIN</li>
 *   <li>PUT /api/outwards/{id}/complete → MANAGER, ADMIN only</li>
 * </ul>
 *
 * @see OutwardService
 * @see sg.projects.stockflowengine.model.Outward
 */
@RestController
@RequestMapping("/api/outwards")
@CrossOrigin(origins = "*")
public class OutwardController {

    private final OutwardService outwardService;

    /**
     * Constructs OutwardController with the outward management service.
     *
     * @param outwardService service handling outward business logic and persistence
     */
    public OutwardController(OutwardService outwardService) {
        this.outwardService = outwardService;
    }

    /**
     * Creates a new outward transaction for goods to be dispatched.
     *
     * <p>Validates sufficient stock before saving — blocks if requested
     * quantity exceeds current available stock.
     * Outward date defaults to today if not provided.
     * Status defaults to PENDING_SHIPMENT.
     * MaterialId is auto-populated from the variant's parent material.
     * Client-supplied outwardId is ignored — ID is always auto-generated.
     *
     * @param outward the outward transaction details
     * @return created outward with PENDING_SHIPMENT status
     */
    @PostMapping
    public ResponseEntity<?> createOutward(@Valid @RequestBody Outward outward) {
        return ResponseEntity.ok(outwardService.saveOutward(outward));
    }

    /**
     * Retrieves a specific outward transaction by ID.
     *
     * @param id the outward transaction ID to look up
     * @return outward details, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Outward> getOutwardById(@PathVariable Long id) {
        return outwardService.getOutwardById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marks a PENDING_SHIPMENT outward as COMPLETED, confirming physical dispatch.
     *
     * <p>Restricted to MANAGER and ADMIN roles only.
     * Sets shippedDate to today automatically on completion.
     * Stock level for the variant is re-calculated and re-embedded in
     * PGVector via Spring Events after completion.
     *
     * @param id the outward transaction ID to complete
     * @return completed outward with COMPLETED status and shippedDate stamped
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeOutward(@PathVariable Long id) {
        return ResponseEntity.ok(outwardService.completeOutward(id));
    }

    /**
     * Retrieves all outward transactions in the system.
     *
     * <p>Returns all outwards regardless of status — including
     * PENDING_SHIPMENT and COMPLETED records.
     *
     * @return list of all outward transactions
     */
    @GetMapping
    public ResponseEntity<List<Outward>> getAllOutwards() {
        return ResponseEntity.ok(outwardService.getAllOutwards());
    }
}