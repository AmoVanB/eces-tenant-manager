package de.tum.ei.lkn.eces.tenantmanager.rerouting.components;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.Entity;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.tenantmanager.TenantManagerSystem;

/**
 * Component holding the previous Entity on which a flow was embedded (due to rerouting)
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = TenantManagerSystem.class)
public class RerouteFrom extends Component {
	private Entity formerEntity;
	public RerouteFrom(Entity formerEntity) {
		this.formerEntity = formerEntity;
	}

	public Entity getPath() {
		return formerEntity;
	}
}
