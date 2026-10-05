package sg.projects.stockflowengine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.service.MaterialVariantStockService;

/**
 * REST controller for querying real-time warehouse stock levels.
 *
 * <p>Stock is calculated dynamically — never stored as a static value.
 * For each variant, stock is computed as:
 * <pre>
 * Current Stock = Total ACTIVE Inwards - Total COMPLETED Outwards
 * </pre>
 *
 * <p>This ensures stock always reflects the true warehouse state:
 * <ul>
 *   <li>PENDING_INSPECTION inwards are excluded — unverified goods don't count</li>
 *   <li>REJECTED inwards are excluded — failed quality checks don't count</li>
 *   <li>PENDING_SHIPMENT outwards are included — reserved stock is deducted</li>
 * </ul>
 *
 * <p>Accessible by TECHNICAL_STAFF, MANAGER and ADMIN roles.
 * All endpoints require a valid JWT token.
 *
 * @see MaterialVariantStockService
 * @see sg.projects.stockflowengine.dto.MaterialVariantStockDto
 */
@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class MaterialVariantStockController {

    private final MaterialVariantStockService materialVariantStockService;

    /**
     * Constructs MaterialVariantStockController with the stock calculation service.
     *
     * @param materialVariantStockService service for dynamic stock level calculation
     */
    public MaterialVariantStockController(
            MaterialVariantStockService materialVariantStockService) {
        this.materialVariantStockService = materialVariantStockService;
    }

    /**
     * Retrieves current stock levels for all material variants.
     *
     * <p>Returns a complete warehouse inventory snapshot with
     * total inward, total outward and current stock per variant.
     *
     * @return list of stock level DTOs for all variants
     */
    @GetMapping
    public ResponseEntity<?> getAllStock() {
        return ResponseEntity.ok(materialVariantStockService.getAllStock());
    }

    /**
     * Retrieves current stock level for a specific material variant.
     *
     * @param materialVariantId the variant ID to query stock for
     * @return stock level DTO with totalInward, totalOutward and currentStock,
     *         or 404 if variant not found
     */
    @GetMapping("/{materialVariantId}")
    public ResponseEntity<?> getMaterialVariantStock(
            @PathVariable Long materialVariantId) {
        return ResponseEntity.ok(
                materialVariantStockService.getStockForMaterialVariant(materialVariantId));
    }
}