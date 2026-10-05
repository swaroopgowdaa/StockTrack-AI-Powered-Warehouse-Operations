package sg.projects.stockflowengine.ai;

import sg.projects.stockflowengine.model.Inward;

/**
 * Domain event published when an inward transaction status is updated.
 *
 * <p>Published by {@link sg.projects.stockflowengine.service.InwardService}
 * when a MANAGER or ADMIN approves (ACTIVE) or rejects (REJECTED) a
 * PENDING_INSPECTION inward record.
 *
 * <p>Handled by {@link WarehouseEventListener} which:
 * <ul>
 *   <li>Re-embeds the inward record with updated status in PGVector</li>
 *   <li>Re-embeds the stock level for the variant since approval
 *       changes the stock calculation</li>
 * </ul>
 *
 * @see WarehouseEventListener
 * @see sg.projects.stockflowengine.service.InwardService
 */
public class InwardStatusChangedEvent {

    private final Inward inward;

    /**
     * Constructs an InwardStatusChangedEvent with the updated inward record.
     *
     * @param inward the inward transaction with its new status applied
     */
    public InwardStatusChangedEvent(Inward inward) {
        this.inward = inward;
    }

    /**
     * Returns the inward transaction associated with this event.
     *
     * @return the updated inward record with new status
     */
    public Inward getInward() {
        return inward;
    }
}