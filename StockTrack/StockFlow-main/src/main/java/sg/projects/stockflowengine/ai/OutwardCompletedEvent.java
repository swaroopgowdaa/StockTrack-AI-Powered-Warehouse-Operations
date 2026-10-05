package sg.projects.stockflowengine.ai;

import sg.projects.stockflowengine.model.Outward;

/**
 * Domain event published when an outward transaction is marked as completed.
 *
 * <p>Published by {@link sg.projects.stockflowengine.service.OutwardService}
 * when a MANAGER or ADMIN completes a PENDING_SHIPMENT outward, confirming
 * physical dispatch of goods from the warehouse.
 *
 * <p>Handled by {@link WarehouseEventListener} which:
 * <ul>
 *   <li>Re-embeds the outward record with COMPLETED status in PGVector</li>
 *   <li>Re-embeds the stock level for the variant since completion
 *       confirms goods have left the warehouse</li>
 * </ul>
 *
 * @see WarehouseEventListener
 * @see sg.projects.stockflowengine.service.OutwardService
 */
public class OutwardCompletedEvent {

    private final Outward outward;

    /**
     * Constructs an OutwardCompletedEvent with the completed outward record.
     *
     * @param outward the outward transaction marked as COMPLETED with shipped date stamped
     */
    public OutwardCompletedEvent(Outward outward) {
        this.outward = outward;
    }

    /**
     * Returns the outward transaction associated with this event.
     *
     * @return the completed outward record
     */
    public Outward getOutward() {
        return outward;
    }
}