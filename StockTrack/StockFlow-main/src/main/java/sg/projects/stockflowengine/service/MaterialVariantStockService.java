package sg.projects.stockflowengine.service;

import org.springframework.stereotype.Service;
import sg.projects.stockflowengine.dto.MaterialVariantStockDto;
import sg.projects.stockflowengine.model.MaterialVariant;
import sg.projects.stockflowengine.repository.InwardRepository;
import sg.projects.stockflowengine.repository.OutwardRepository;
import sg.projects.stockflowengine.repository.MaterialVariantRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for dynamic stock level calculation per material variant.
 *
 * <p>Stock is never stored as a static value — it is always computed on demand:
 * <pre>
 * Current Stock = Total ACTIVE Inwards - Total Outwards (all statuses)
 * </pre>
 *
 * <p>This dynamic approach ensures stock always reflects the true warehouse state:
 * <ul>
 *   <li>PENDING_INSPECTION inwards excluded — unverified goods don't count</li>
 *   <li>REJECTED inwards excluded — failed quality checks don't count</li>
 *   <li>PENDING_SHIPMENT outwards included — reserved stock is deducted immediately</li>
 *   <li>COMPLETED outwards included — shipped goods permanently deducted</li>
 * </ul>
 *
 * <p>Uses targeted repository queries instead of {@code findAll()} + stream filtering
 * to minimize database load and avoid loading unnecessary records into memory.
 *
 * <p>Also used by {@link sg.projects.stockflowengine.ai.WarehouseEventListener}
 * to re-embed stock levels in PGVector after inward approvals and outward completions.
 *
 * @see InwardRepository#sumActiveQuantityByMaterialVariantId(Long)
 * @see OutwardRepository#sumQuantityByMaterialVariantId(Long)
 * @see sg.projects.stockflowengine.dto.MaterialVariantStockDto
 */
@Service
public class MaterialVariantStockService {

    private final InwardRepository inwardRepository;
    private final OutwardRepository outwardRepository;
    private final MaterialVariantRepository materialVariantRepository;

    /**
     * Constructs MaterialVariantStockService with required repositories.
     *
     * <p>Note: DTOs are not Spring-managed beans — they are instantiated
     * directly in service methods as value objects.
     *
     * @param inwardRepository          repository for ACTIVE inward quantity queries
     * @param outwardRepository         repository for total outward quantity queries
     * @param materialVariantRepository repository for variant lookup and listing
     */
    public MaterialVariantStockService(InwardRepository inwardRepository,
                                       OutwardRepository outwardRepository,
                                       MaterialVariantRepository materialVariantRepository) {
        this.inwardRepository = inwardRepository;
        this.outwardRepository = outwardRepository;
        this.materialVariantRepository = materialVariantRepository;
    }

    /**
     * Calculates current stock level for a specific material variant.
     *
     * <p>Executes two targeted SQL aggregation queries:
     * <ol>
     *   <li>SUM of ACTIVE inward quantities for the variant</li>
     *   <li>SUM of all outward quantities for the variant</li>
     * </ol>
     *
     * <p>Stock = totalInward - totalOutward
     * Both return 0 via COALESCE if no records exist, preventing null arithmetic.
     *
     * @param materialVariantId the variant ID to calculate stock for
     * @return DTO containing variant name, totalInward, totalOutward and currentStock
     * @throws IllegalArgumentException if no variant found with the given ID
     */
    public MaterialVariantStockDto getStockForMaterialVariant(Long materialVariantId) {

        // Fetch variant for name and ID — single DB call
        MaterialVariant variant = materialVariantRepository.findById(materialVariantId)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found"));

        // Targeted aggregation queries
        int totalInward = inwardRepository
                .sumActiveQuantityByMaterialVariantId(materialVariantId);
        int totalOutward = outwardRepository
                .sumQuantityByMaterialVariantId(materialVariantId);

        int currentStock = totalInward - totalOutward;

        return new MaterialVariantStockDto(
                variant.getMaterialVariantId(),
                variant.getMaterialVariantName(),
                totalInward,
                totalOutward,
                currentStock
        );
    }

    /**
     * Calculates current stock levels for all material variants.
     *
     * <p>Delegates to {@link #getStockForMaterialVariant(Long)} for each variant
     * to ensure consistent stock calculation logic across single and bulk queries.
     *
     * @return list of stock level DTOs for all variants in the system
     */
    public List<MaterialVariantStockDto> getAllStock() {
        return materialVariantRepository.findAll().stream()
                .map(variant -> getStockForMaterialVariant(variant.getMaterialVariantId()))
                .collect(Collectors.toList());
    }
}