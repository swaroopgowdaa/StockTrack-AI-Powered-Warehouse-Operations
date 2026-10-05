package sg.projects.stockflowengine.ai;

import sg.projects.stockflowengine.model.Outward;

/**
 * Domain event published when a new outward transaction is created.
 *
 * <p>Published by {@link sg.projects.stockflowengine.service.OutwardService}
 * when a new outward request is submitted by TECHNICAL_STAFF, MANAGER or ADMIN.
 * The outward is created with PENDING_SHIPMENT status, pending physical dispatch.
 *
 * <p>Handled by {@link WarehouseEventListener} to immediately embed the new
 * outward record in PGVector, making it searchable via AI queries such as
 * "what shipments are pending?" or "what did I dispatch today?".
 *
 * @see WarehouseEventListener
 * @see sg.projects.stockflowengine.service.OutwardService
 */
public class OutwardCreatedEvent {

    private final Outward outward;

    /**
     * Constructs an OutwardCreatedEvent with the saved outward record.
     *
     * @param outward the newly created and persisted outward transaction
     */
    public OutwardCreatedEvent(Outward outward) {
        this.outward = outward;
    }

    /**
     * Returns the outward transaction associated with this event.
     *
     * @return the newly created outward record
     */
    public Outward getOutward() {
        return outward;
    }
}