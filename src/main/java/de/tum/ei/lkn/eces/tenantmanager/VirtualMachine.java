package de.tum.ei.lkn.eces.tenantmanager;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.network.Host;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Component representing a VM.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = TenantManagerSystem.class)
public class VirtualMachine extends Component {
    private String name;
    private Host hostMachine;
    private List<Flow> flows;
    private Tenant tenant;
    private String managementConnection;

    VirtualMachine(Tenant tenant, String name) {
        this.tenant = tenant;
        this.name = name;
        this.flows = new LinkedList<>();
        this.managementConnection = null;
    }

    void addFlow(Flow flow) {
        this.flows.add(flow);
    }

    void removeFlow(Flow flow) {
        this.flows.remove(flow);
    }

    public String getName() {
        return name;
    }

    public List<Flow> getFlows() {
        return Collections.unmodifiableList(flows);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Host getHostMachine() {
        return hostMachine;
    }

    void setHostMachine(Host host) {
        this.hostMachine = host;
    }

    public String getManagementConnection() {
        return managementConnection;
    }

    public void setManagementConnection(String managementConnection) {
        this.managementConnection = managementConnection;
    }

    @Override
    public String toString() {
        if(hostMachine == null)
            return tenant.toString() + "/VM" + getIdForString() + "@unplaced";
        else
            return tenant.toString() + "/VM" + getIdForString() + "@" + hostMachine.toString();
    }

    private String getIdForString() {
        if(this.getEntity() == null)
            return Integer.toHexString(this.hashCode());
        else
            return String.valueOf(this.getId());
    }
}
