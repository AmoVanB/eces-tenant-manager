package de.tum.ei.lkn.eces.tenantmanager.rerouting.components;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.tenantmanager.Flow;
import de.tum.ei.lkn.eces.tenantmanager.TenantManagerSystem;

import java.util.List;

/**
 * Component holding stats about the rerouting that happened.
 * Attached to a Flow and gives the stats for embedding this flow.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = TenantManagerSystem.class)
public class ReroutingStats extends Component {
	private final List<Flow> reconfigurationAttempts;
	private final List<Flow> reconfigurationSuccess;
	private final List<Flow> flowsReconfigured;

	public ReroutingStats(List<Flow> reconfigurationAttempts, List<Flow> reconfigurationSuccess, List<Flow> flowsReconfigured) {
		this.reconfigurationAttempts = reconfigurationAttempts;
		this.reconfigurationSuccess = reconfigurationSuccess;
		this.flowsReconfigured = flowsReconfigured;
	}

	public List<Flow> getReconfigurationAttempts() {
		return reconfigurationAttempts;
	}

	public List<Flow> getReconfigurationSuccess() {
		return reconfigurationSuccess;
	}

	public List<Flow> getFlowsReconfigured() {
		return flowsReconfigured;
	}
}
