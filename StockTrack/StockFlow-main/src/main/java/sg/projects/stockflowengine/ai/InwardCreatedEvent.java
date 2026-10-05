package sg.projects.stockflowengine.ai;

import sg.projects.stockflowengine.model.Inward;

/**
 * Domain event published when a new inward transaction is created.
 *
 * <p>Published by {@link sg.projects.stockflowengine.service.InwardService}
 * and handled by {@link WarehouseEventListener} to trigger immediate
 * vector embedding of the new inward record in PGVector.
 *
 * <p>Part of the Spring Events decoupling pattern — services publish events
 * without any knowledge of the AI embedding layer.
 *
 * @see WarehouseEventListener
 * @see sg.projects.stockflowengine.service.InwardService
 */
public class InwardCreatedEvent {

    private final Inward inward;

    /**
     * Constructs an InwardCreatedEvent with the saved inward record.
     *
     * @param inward the newly created and persisted inward transaction
     */
    public InwardCreatedEvent(Inward inward) {
        this.inward = inward;
    }

    /**
     * Returns the inward transaction associated with this event.
     *
     * @return the newly created inward record
     */
    public Inward getInward() {
        return inward;
    }
}