# Tenant Manager 

This project implements a tenant manager module on top of the [routing](https://github.com/AmoVanB/eces-routing) library of the [ECES](https://github.com/AmoVanB/eces-core) framework.

The module introduces the concepts of [Tenant](src/main/java/de/tum/ei/lkn/eces/tenantmanager/Tenant.java), [Virtual Machine](src/main/java/de/tum/ei/lkn/eces/tenantmanager/VirtualMachine.java), and [Flow](src/main/java/de/tum/ei/lkn/eces/tenantmanager/Flow.java).
The project uses the [DNM](https://github.com/AmoVanB/eces-dnm) library to enable the embedding of flows with strict delay guarantees in a communication network.

This project contains the reference implementation (in *[TenantManagerSystem](src/main/java/de/tum/ei/lkn/eces/tenantmanager/TenantManagerSystem.java)*.`createFlow()`) of the rerouting logic of **Chameleon** originally published in (Sec. 3.4 and 4.1.3):
- [Amaury Van Bemten, Nemanja Ðerić, Amir Varasteh, Stefan Schmid, Carmen Mas-Machuca, Andreas Blenk, and Wolfgang Kellerer. "Chameleon: Predictable Latency and High Utilization with Queue-Aware and Adaptive Source Routing." ACM CoNEXT, 2020](https://mediatum.ub.tum.de/doc/1577772/file.pdf).
The implementation uses this strategy for embedding a flow.

## Usage

The project can be downloaded from maven central using:
```xml
<dependency>
  <groupId>de.tum.ei.lkn.eces</groupId>
  <artifactId>tenant-manager</artifactId>
  <version>X.Y.Z</version>
</dependency>
```

## Examples

```java
tenant = tenantManagerSystem.createTenant("first tenant");
host1 = networkingSystem.createHost(network, "host1");
host2 = networkingSystem.createHost(network, "host2");
vm1 = tenantManagerSystem.createVirtualMachine(tenant, "vm1");
vm2 = tenantManagerSystem.createVirtualMachine(tenant, "vm2");
tenantManagerSystem.createFlow("flow #1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 1);
```

See [tests](src/test) for other simple examples.

See other ECES repositories using this library (e.g., the [NBI](https://github.com/AmoVanB/eces-nbi)) for more detailed/advanced examples.