package de.tum.ei.lkn.eces.tenantmanager;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Component representing a tenant.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = TenantManagerSystem.class)
public class Tenant extends Component {
    private String name;
    private List<VirtualMachine> vms;

    Tenant(String name) {
        this.name = name;
        this.vms = new LinkedList<>();
    }

    void addVM(VirtualMachine vm) {
        this.vms.add(vm);
    }

    void removeVM(VirtualMachine vm) {
        this.vms.remove(vm);
    }

    public String getName() {
        return name;
    }

    public List<VirtualMachine> getVirtualMachines() {
        return Collections.unmodifiableList(vms);
    }

    @Override
    public String toString() {
        return "Tenant" + getIdForString() + "(" + name + ")";
    }

    private String getIdForString() {
        if(this.getEntity() == null)
            return Integer.toHexString(this.hashCode());
        else
            return String.valueOf(this.getId());
    }
}
