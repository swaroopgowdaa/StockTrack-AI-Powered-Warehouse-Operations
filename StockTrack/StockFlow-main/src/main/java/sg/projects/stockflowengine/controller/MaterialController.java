package sg.projects.stockflowengine.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.projects.stockflowengine.model.Material;
import sg.projects.stockflowengine.service.MaterialService;

import java.util.List;

/**
 * REST controller for managing materials in the warehouse system.
 *
 * <p>Materials are the top-level master data entity in the inventory hierarchy:
 * <pre>
 * Material → MaterialVariant → Inward/Outward transactions
 * </pre>
 *
 * <p>A material cannot be deactivated if it has active variants under it,
 * ensuring referential integrity across the inventory hierarchy.
 *
 * <p>Material names must be unique across the system.
 *
 * <p>Accessible by TECHNICAL_STAFF, MANAGER and ADMIN roles.
 * All endpoints require a valid JWT token.
 *
 * @see MaterialService
 * @see sg.projects.stockflowengine.model.Material
 */
@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class MaterialController {

    private final MaterialService materialService;

    /**
     * Constructs MaterialController with the material management service.
     *
     * @param materialService service handling material business logic and persistence
     */
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /**
     * Creates a new material record.
     *
     * <p>Material name must be unique — duplicate names return 400.
     * Status defaults to ACTIVE if not provided.
     * Client-supplied materialId is ignored — ID is always auto-generated.
     *
     * @param material the material details to create
     * @return created material with auto-generated ID and ACTIVE status
     */
    @PostMapping
    public ResponseEntity<Material> createMaterial(@Valid @RequestBody Material material) {
        return ResponseEntity.ok(materialService.saveMaterial(material));
    }

    /**
     * Retrieves all materials in the system.
     *
     * @return list of all materials including INACTIVE ones
     */
    @GetMapping
    public ResponseEntity<List<Material>> getAllMaterials() {
        return ResponseEntity.ok(materialService.getAllMaterials());
    }

    /**
     * Retrieves a specific material by ID.
     *
     * @param id the material ID to look up
     * @return material details, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Material> getMaterialById(@PathVariable Long id) {
        return materialService.getMaterialById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing material's name or description.
     *
     * @param id       the ID of the material to update
     * @param material the updated material details
     * @return updated material record
     */
    @PutMapping("/{id}")
    public ResponseEntity<Material> updateMaterial(@PathVariable Long id,
                                                   @RequestBody Material material) {
        return ResponseEntity.ok(materialService.updateMaterial(id, material));
    }

    /**
     * Soft deletes a material by setting its status to INACTIVE.
     *
     * <p>Blocked if any ACTIVE variants exist under this material —
     * variants must be deactivated first to maintain inventory integrity.
     * Material record is preserved for historical inward/outward records.
     *
     * @param id the ID of the material to deactivate
     * @return deactivated material with INACTIVE status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Material> deleteMaterial(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.softDeleteMaterial(id));
    }
}
