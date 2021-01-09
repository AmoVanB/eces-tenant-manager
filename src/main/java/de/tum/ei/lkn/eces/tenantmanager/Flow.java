package de.tum.ei.lkn.eces.tenantmanager;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.tenantmanager.matching.Matching;
import de.tum.ei.lkn.eces.tenantmanager.traffic.TrafficContract;

/**
 * Component representing a flow. This component is supposed to be attached to its counterparts in the RoutingSystem
 * (requests/responses).
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = TenantManagerSystem.class)
public class Flow extends Component {
    private String name;
    private VirtualMachine source;
    private VirtualMachine destination;
    private Matching matching;
    private TrafficContract trafficContract;

    Flow(String name, VirtualMachine source, VirtualMachine destination, Matching matching, TrafficContract trafficContract) {
        this.name = name;
        this.source = source;
        this.destination = destination;
        this.matching = matching;
        this.trafficContract = trafficContract;
    }

    public VirtualMachine getSource() {
        return source;
    }

    public VirtualMachine getDestination() {
        return destination;
    }

    public Matching getMatching() {
        return matching;
    }

    public TrafficContract getTrafficContract() {
        return trafficContract;
    }

    @Override
    public String toString() {
        return getIdForString() + "(" + name + ")@[" + source.toString() + "->" + destination.toString() + "]";
    }

    private String getIdForString() {
        if(this.getEntity() == null)
            return Integer.toHexString(this.hashCode());
        else
            return String.valueOf(this.getId());
    }
}
