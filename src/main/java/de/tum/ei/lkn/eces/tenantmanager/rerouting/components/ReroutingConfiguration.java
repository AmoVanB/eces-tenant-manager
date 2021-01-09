package de.tum.ei.lkn.eces.tenantmanager.rerouting.components;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.tenantmanager.TenantManagerSystem;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.CostIncreaseTypes;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.FlowSelectionTypes;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.LimitReroutingTypes;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.SortFlowTypes;

/**
 * Component defining the rerouting strategy to use on a given network.
 * The component should be attached to the Entity of the Network object.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = TenantManagerSystem.class)
public class ReroutingConfiguration extends Component {
	private final FlowSelectionTypes flowSelectionTypes;
	private final SortFlowTypes sortFlowTypes;
	private final CostIncreaseTypes costIncreaseTypes;
	private final LimitReroutingTypes limitReroutingTypes;
	private final int rerouteLimit;

	public ReroutingConfiguration(FlowSelectionTypes flowSelectionTypes, SortFlowTypes sortFlowTypes, CostIncreaseTypes costIncreaseTypes, LimitReroutingTypes limitReroutingTypes, int rerouteLimit) {
		this.flowSelectionTypes = flowSelectionTypes;
		this.sortFlowTypes = sortFlowTypes;
		this.costIncreaseTypes = costIncreaseTypes;
		this.limitReroutingTypes = limitReroutingTypes;
		this.rerouteLimit = rerouteLimit;

		if (this.limitReroutingTypes == LimitReroutingTypes.PERCENT) {
			if (rerouteLimit < 0 || rerouteLimit > 100)
				throw new RuntimeException("when the limit rerouting type is in percent, the reroute limit must be [0;100], got " + rerouteLimit);
		}
	}

	public CostIncreaseTypes getCostIncreaseTypes() {
		return costIncreaseTypes;
	}

	public FlowSelectionTypes getFlowSelectionTypes() {
		return flowSelectionTypes;
	}

	public SortFlowTypes getSortFlowTypes() {
		return sortFlowTypes;
	}

	public LimitReroutingTypes getLimitReroutingTypes() {
		return limitReroutingTypes;
	}

	public int getRerouteLimit() {
		return rerouteLimit;
	}
}
