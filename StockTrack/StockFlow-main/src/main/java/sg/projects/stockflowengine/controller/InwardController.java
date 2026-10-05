package sg.projects.stockflowengine.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.model.Inward;
import sg.projects.stockflowengine.service.InwardService;

import java.util.List;

/**
 * REST controller for managing inward transactions in the warehouse.
 *
 * <p>Inward transactions represent the receipt of goods into the warehouse.
 * They follow a strict status workflow:
 * <pre>
 * PENDING_INSPECTION → ACTIVE (approved by MANAGER/ADMIN)
 * PENDING_INSPECTION → REJECTED (rejected by MANAGER/ADMIN)
 * </pre>
 *
 * <p>Only ACTIVE inwards contribute to stock calculations.
 * PENDING_INSPECTION inwards are quarantined from stock until approved,
 * preventing unverified goods from being dispatched.
 *
 * <p>Access control:
 * <ul>
 *   <li>POST /api/inwards → TECHNICAL_STAFF, MANAGER, ADMIN</li>
 *   <li>GET /api/inwards → TECHNICAL_STAFF, MANAGER, ADMIN</li>
 *   <li>PUT /api/inwards/{id}/status → MANAGER, ADMIN only</li>
 * </ul>
 *
 * @see InwardService
 * @see sg.projects.stockflowengine.model.Inward
 */
@RestController
@RequestMapping("/api/inwards")
@CrossOrigin(origins = "*")
public class InwardController {

    private final InwardService inwardService;

    /**
     * Constructs InwardController with the inward management service.
     *
     * @param inwardService service handling inward business logic and persistence
     */
    public InwardController(InwardService inwardService) {
        this.inwardService = inwardService;
    }

    /**
     * Creates a new inward transaction for received goods.
     *
     * <p>Inward date defaults to today if not provided.
     * Status defaults to PENDING_INSPECTION.
     * MaterialId is autopopulated from the variant's parent material.
     * Client-supplied inwardId is ignored — ID is always auto-generated.
     *
     * @param inward the inward transaction details
     * @return created inward with PENDING_INSPECTION status
     */
    @PostMapping
    public ResponseEntity<?> createInward(@Valid @RequestBody Inward inward) {
        return ResponseEntity.ok(inwardService.saveInward(inward));
    }

    /**
     * Retrieves all inward transactions in the system.
     *
     * <p>Returns all inwards regardless of status — including
     * PENDING_INSPECTION, ACTIVE and REJECTED records.
     *
     * @return list of all inward transactions
     */
    @GetMapping
    public ResponseEntity<List<Inward>> getAllInwards() {
        return ResponseEntity.ok(inwardService.getAllInwards());
    }

    /**
     * Retrieves a specific inward transaction by ID.
     *
     * @param id the inward transaction ID to look up
     * @return inward details, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Inward> getInwardById(@PathVariable Long id) {
        return inwardService.getInwardById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates the status of a PENDING_INSPECTION inward transaction.
     *
     * <p>Restricted to MANAGER and ADMIN roles only.
     * Only PENDING_INSPECTION inwards can be updated.
     * Valid status transitions: ACTIVE (approve) or REJECTED (reject).
     *
     * <p>On ACTIVE status — stock calculation for the variant is updated
     * and vector store embedding is refreshed via Spring Events.
     *
     * @param id     the inward transaction ID to update
     * @param status the new status — must be ACTIVE or REJECTED
     * @return updated inward with new status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestParam String status) {
        return ResponseEntity.ok(inwardService.updateInwardStatus(id, status));
    }
}