package sg.projects.stockflowengine.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.model.MaterialVariant;
import sg.projects.stockflowengine.service.MaterialVariantService;
import java.util.List;

/**
 * REST controller for managing material variants in the warehouse system.
 *
 * <p>Variants represent specific types or specifications of a parent material.
 * They sit in the middle of the inventory hierarchy:
 * <pre>
 * Material → MaterialVariant → Inward/Outward transactions
 * </pre>
 *
 * <p>Example: Material "Steel Rod" may have variants
 * "12mm Standard Rod" and "16mm Heavy Rod".
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Variant must belong to an existing parent material</li>
 *   <li>Variant name must be unique within the same parent material</li>
 *   <li>INACTIVE variants cannot receive new inwards or outwards</li>
 *   <li>Cannot deactivate a variant with PENDING_INSPECTION or ACTIVE inwards</li>
 * </ul>
 *
 * <p>Accessible by TECHNICAL_STAFF, MANAGER and ADMIN roles.
 * All endpoints require a valid JWT token.
 *
 * @see MaterialVariantService
 * @see sg.projects.stockflowengine.model.MaterialVariant
 */
@RestController
@RequestMapping("/api/variants")
@CrossOrigin(origins = "*")
public class MaterialVariantController {

    private final MaterialVariantService materialVariantService;

    /**
     * Constructs MaterialVariantController with the variant management service.
     *
     * @param materialVariantService service handling variant business logic and persistence
     */
    public MaterialVariantController(MaterialVariantService materialVariantService) {
        this.materialVariantService = materialVariantService;
    }

    /**
     * Creates a new material variant under an existing parent material.
     *
     * <p>Status defaults to ACTIVE if not provided.
     * Client-supplied materialVariantId is ignored — ID is always auto-generated.
     * materialId (FK) must reference an existing material.
     *
     * @param variant the variant details including parent materialId
     * @return created variant with auto-generated ID and ACTIVE status
     */
    @PostMapping
    public ResponseEntity<MaterialVariant> createVariant(@Valid @RequestBody MaterialVariant variant) {
        return ResponseEntity.ok(materialVariantService.saveMaterialVariant(variant));
    }

    /**
     * Retrieves all material variants in the system.
     *
     * @return list of all variants including INACTIVE ones
     */
    @GetMapping
    public ResponseEntity<List<MaterialVariant>> getAllVariants() {
        return ResponseEntity.ok(materialVariantService.getAllMaterialVariants());
    }

    /**
     * Retrieves a specific material variant by ID.
     *
     * @param id the variant ID to look up
     * @return variant details, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<MaterialVariant> getVariantById(@PathVariable Long id) {
        return materialVariantService.getMaterialVariantById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing variant's name or parent material.
     *
     * @param id      the ID of the variant to update
     * @param variant the updated variant details
     * @return updated variant record
     */
    @PutMapping("/{id}")
    public ResponseEntity<MaterialVariant> updateVariant(@PathVariable Long id,
                                                         @RequestBody MaterialVariant variant) {
        return ResponseEntity.ok(materialVariantService.updateMaterialVariant(id, variant));
    }

    /**
     * Soft deletes a variant by setting its status to INACTIVE.
     *
     * <p>Blocked if any PENDING_INSPECTION or ACTIVE inwards exist for this variant —
     * live stock cannot be orphaned. REJECTED inwards do not block deactivation.
     * Variant record is preserved for historical transaction records.
     *
     * @param id the ID of the variant to deactivate
     * @return deactivated variant with INACTIVE status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MaterialVariant> deleteVariant(@PathVariable Long id) {
        return ResponseEntity.ok(materialVariantService.softDeleteMaterialVariant(id));
    }
}