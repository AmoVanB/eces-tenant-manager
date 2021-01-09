package de.tum.ei.lkn.eces.tenantmanager;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.util.EventCountTestSystem;
import de.tum.ei.lkn.eces.dnm.DNMSystem;
import de.tum.ei.lkn.eces.dnm.ResidualMode;
import de.tum.ei.lkn.eces.dnm.config.ACModel;
import de.tum.ei.lkn.eces.dnm.config.BurstIncreaseModel;
import de.tum.ei.lkn.eces.dnm.config.DetServConfig;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Division;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.LowerLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Summation;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.UpperLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.values.Constant;
import de.tum.ei.lkn.eces.dnm.config.costmodels.values.QueuePriority;
import de.tum.ei.lkn.eces.dnm.mappers.DetServConfigMapper;
import de.tum.ei.lkn.eces.dnm.proxies.DetServProxy;
import de.tum.ei.lkn.eces.dnm.resourcemanagement.resourceallocation.MHM.MHMRateRatiosAllocation;
import de.tum.ei.lkn.eces.graph.GraphSystem;
import de.tum.ei.lkn.eces.network.Host;
import de.tum.ei.lkn.eces.network.Network;
import de.tum.ei.lkn.eces.network.NetworkNode;
import de.tum.ei.lkn.eces.network.NetworkingSystem;
import de.tum.ei.lkn.eces.network.util.NetworkInterface;
import de.tum.ei.lkn.eces.routing.RoutingSystem;
import de.tum.ei.lkn.eces.routing.algorithms.RoutingAlgorithm;
import de.tum.ei.lkn.eces.routing.algorithms.csp.unicast.cbf.CBFAlgorithm;
import de.tum.ei.lkn.eces.routing.mappers.PathListMapper;
import de.tum.ei.lkn.eces.routing.mappers.PathMapper;
import de.tum.ei.lkn.eces.routing.pathlist.PathListSystem;
import de.tum.ei.lkn.eces.tenantmanager.exceptions.TenantManagerException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

/**
 * Test class for the whole system.
 *
 * @author Amaury Van Bemten
 */
public class TenantManagerSystemTest {
    private Controller controller;
    private NetworkingSystem networkingSystem;
    private EventCountTestSystem countTestSystem;
    private DNMSystem dnmSystem;
    private TenantManagerSystem tenantManagerSystem;
    private Network network;
    private PathMapper pathMapper;
    private PathListSystem pathListSystem;
    private PathListMapper pathListMapper;

    @Before
    public void setUp() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("de.tum.ei.lkn.eces.dnm").setLevel(Level.ERROR);
        Logger.getLogger("de.tum.ei.lkn.eces.graph").setLevel(Level.ERROR);
        Logger.getLogger("de.tum.ei.lkn.eces.network").setLevel(Level.ERROR);
        Logger.getLogger("org.eclipse.jetty").setLevel(Level.ERROR);
        Logger.getLogger("de.tum.ei.lkn.eces.routing").setLevel(Level.INFO);

        controller = new Controller();
        GraphSystem graphSystem = new GraphSystem(controller);
        networkingSystem = new NetworkingSystem(controller, graphSystem);

        DetServConfig modelConfig = new DetServConfig(
                ACModel.MHM,
                ResidualMode.LEAST_LATENCY,
                BurstIncreaseModel.NO,
                false,
                new LowerLimit(new UpperLimit(
                        new Division(new Constant(), new Summation(new Constant(), new QueuePriority())),
                        1), 0),
                (controller1, scheduler) -> new MHMRateRatiosAllocation(controller1, new double[]{1.0/4, 1.0/5, 1.0/6, 1.0/8}));


        // DNC
        dnmSystem = new DNMSystem(controller);
        DetServProxy proxy = new DetServProxy(controller);

        // Routing
        new RoutingSystem(controller);
        RoutingAlgorithm cbf = new CBFAlgorithm(controller);
        cbf.setProxy(proxy);
        modelConfig.initCostModel(controller);
        pathMapper = new PathMapper(controller);
        pathListSystem = new PathListSystem(controller);
        pathListMapper = new PathListMapper(controller);

        // Create network
        network = networkingSystem.createNetwork();
        DetServConfigMapper modelingConfigMapper = new DetServConfigMapper(controller);
        modelingConfigMapper.attachComponent(network.getQueueGraph(), modelConfig);

        tenantManagerSystem = new TenantManagerSystem(network, cbf, controller);
        countTestSystem = new EventCountTestSystem(controller);
    }

    @Test
    public void testCreateTenant() {
        Tenant tenant;

        // One tenant
        try {
            tenant = tenantManagerSystem.createTenant("first tenant");
            assertEquals(tenant.getName(), "first tenant");
            assertEquals(tenant.getVirtualMachines().size(), 0);
        }
        catch(TenantManagerException e) {
            fail();
        }
        countTestSystem.doFullCheck(Tenant.class, 1, 0, 0);
        countTestSystem.checkIfEmpty();

        // Two tenants with same name
        try {
            tenant = tenantManagerSystem.createTenant("first tenant");
            assertEquals(tenant.getName(), "first tenant");
            assertEquals(tenant.getVirtualMachines().size(), 0);
            tenant = tenantManagerSystem.createTenant("first tenant");
            fail();
        }
        catch(TenantManagerException e) {
            // should be thrown
        }
        countTestSystem.checkIfEmpty();

        // Three tenants with different names
        try {
            tenant = tenantManagerSystem.createTenant("another tenant");
            assertEquals(tenant.getName(), "another tenant");
            assertEquals(tenant.getVirtualMachines().size(), 0);
            tenant = tenantManagerSystem.createTenant("sec tenant");
            assertEquals(tenant.getName(), "sec tenant");
            assertEquals(tenant.getVirtualMachines().size(), 0);
            tenant = tenantManagerSystem.createTenant("third tenant");
            assertEquals(tenant.getName(), "third tenant");
            assertEquals(tenant.getVirtualMachines().size(), 0);
        }
        catch(TenantManagerException e) {
            fail();
        }
        countTestSystem.doFullCheck(Tenant.class, 3, 0, 0);
        countTestSystem.checkIfEmpty();
    }

    @Test
    public void testCreateVM() {
        VirtualMachine virtualMachine;
        Tenant tenant = null;
        // One tenant
        try {
            tenant = tenantManagerSystem.createTenant("first tenant");
            assertEquals(tenant.getName(), "first tenant");
            assertEquals(tenant.getVirtualMachines().size(), 0);
            tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            fail();
        }
        catch(TenantManagerException e) {
            // Should not work because there is no host to place the VM in
        }
        countTestSystem.doFullCheck(Tenant.class, 1, 0, 0);
        countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 0);
        countTestSystem.checkIfEmpty();

        // Create hosts
        Host lundi = networkingSystem.createHost(network, "lundi");
        Host mardi = networkingSystem.createHost(network, "mardi");
        try {
            virtualMachine = tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            assertEquals(tenant.getVirtualMachines().size(), 1);
            assertTrue(tenant.getVirtualMachines().contains(virtualMachine));
            assertSame(virtualMachine.getTenant(), tenant);
        }
        catch(TenantManagerException e) {
            fail();
        }
        countTestSystem.doFullCheck(Tenant.class, 0, 1, 0);
        // creation + update to place the VM
        countTestSystem.doFullCheck(VirtualMachine.class, 1, 1, 0);

        // Other VMs
        try {
            tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            fail();
        }
        catch(TenantManagerException e) {
            // same name fails
        }
        countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
        countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 0);

        try {
            virtualMachine = tenantManagerSystem.createVirtualMachine(tenant, "newVm1");
            assertTrue(tenant.getVirtualMachines().contains(virtualMachine));
            assertSame(virtualMachine.getTenant(), tenant);
            virtualMachine = tenantManagerSystem.createVirtualMachine(tenant, "newVm2");
            assertTrue(tenant.getVirtualMachines().contains(virtualMachine));
            assertSame(virtualMachine.getTenant(), tenant);
            virtualMachine = tenantManagerSystem.createVirtualMachine(tenant, "newVm3");
            assertTrue(tenant.getVirtualMachines().contains(virtualMachine));
            assertSame(virtualMachine.getTenant(), tenant);
            virtualMachine = tenantManagerSystem.createVirtualMachine(tenant, "newVm4", lundi);
            assertTrue(tenant.getVirtualMachines().contains(virtualMachine));
            assertSame(virtualMachine.getTenant(), tenant);
            assertEquals(virtualMachine.getHostMachine(), lundi);
            virtualMachine = tenantManagerSystem.createVirtualMachine(tenant, "newVm5", mardi);
            assertTrue(tenant.getVirtualMachines().contains(virtualMachine));
            assertSame(virtualMachine.getTenant(), tenant);
            assertEquals(virtualMachine.getHostMachine(), mardi);
            virtualMachine = tenantManagerSystem.createVirtualMachine(tenant, "newVm6", lundi);
            assertTrue(tenant.getVirtualMachines().contains(virtualMachine));
            assertSame(virtualMachine.getTenant(), tenant);
            assertEquals(virtualMachine.getHostMachine(), lundi);
            assertEquals(tenant.getVirtualMachines().size(), 7);
            tenantManagerSystem.createVirtualMachine(tenant, "newVm3");
            fail();
        }
        catch(TenantManagerException e) {
            // same name fails at the end
        }
        countTestSystem.doFullCheck(Tenant.class, 0, 6, 0);
        countTestSystem.doFullCheck(VirtualMachine.class, 6, 6, 0);
    }

    @Test
    public void testCreateFlow() throws UnknownHostException {
        VirtualMachine vm1 = null, vm2 = null;
        Tenant tenant = null;
        Host lundi = null, mardi = null;
        NetworkNode lundiNode = null, mardiNode = null;
        Flow flow = null;
        // One tenant
        try {
            tenant = tenantManagerSystem.createTenant("first tenant");
            lundi = networkingSystem.createHost(network, "lundi");
            mardi = networkingSystem.createHost(network, "mardi");
            // Creating two VMs on the two different hosts
            vm1 = tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            vm2 = null;
            int i = 0;
            while(vm2 == null || vm2.getHostMachine() == vm1.getHostMachine())
                vm2 = tenantManagerSystem.createVirtualMachine(tenant, "newVm" + ++i);
            countTestSystem.doFullCheck(Tenant.class, 1, i + 1, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, i + 1, i + 1, 0);
        }
        catch(TenantManagerException e) {
            fail();
        }

        // Create flows

        try {
            tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 1);
            fail();
        } catch (TenantManagerException e) {
            // Should not work because 0 interfaces
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 0);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 0);
        }

        try {
            lundiNode = networkingSystem.addInterface(lundi, new NetworkInterface("1", "00:00:00:00:00:00"));
            mardiNode = networkingSystem.addInterface(mardi, new NetworkInterface("2", "00:00:00:00:00:00"));
            tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 1);
            fail();
        } catch (TenantManagerException e) {
            // Should not work because no links
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 0);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 0);
        }

        NetworkNode node = networkingSystem.createNode(network);
        networkingSystem.createLinkWithPriorityScheduling(lundiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, lundiNode, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(mardiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, mardiNode, 1e9 / 8, 0, new double[]{30000});

        try {
            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 10);
            assertEquals(vm1.getFlows().size(), 1);
            assertEquals(vm2.getFlows().size(), 1);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            assertTrue(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 1);
        } catch (TenantManagerException e) {
            fail();
        }
        // Should work because we have now a link
        countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
        countTestSystem.doFullCheck(VirtualMachine.class, 0, 2, 0);
        countTestSystem.doFullCheck(Flow.class, 1, 0, 0);

        // show fail we < rate/burst/latency
        try {
            tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, -1, 10);
            fail();
        } catch (TenantManagerException e) {
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 0);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 0);
        }

        // show fail with huge rate
        try {
            tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 1000000000L, 5, 10);
            fail();
        } catch (TenantManagerException e) {
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 0);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 0);
        }

        // show always OK on two same VMs
        try {
            // Creating two VMs on the two different hosts
            vm1 = tenantManagerSystem.createVirtualMachine(tenant, "newVmagain");
            vm2 = null;
            int i = 0;
            while(vm2 == null || vm2.getHostMachine() != vm1.getHostMachine())
                vm2 = tenantManagerSystem.createVirtualMachine(tenant, "nVm" + ++i);
            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 12, 15, 10, 2000000000L, 5, 10);
            assertEquals(vm1.getFlows().size(), 1);
            assertEquals(vm2.getFlows().size(), 1);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            // Here, no path should be there because there was no routing
            assertFalse(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 1);
            flow = tenantManagerSystem.createFlow("f1", vm2, vm1, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 15, 15, 10, 2000000000L, 5, 10);
            assertEquals(vm1.getFlows().size(), 2);
            assertEquals(vm2.getFlows().size(), 2);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            // Here, no path should be there because there was no routing
            assertFalse(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 1);
            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 2000000000L, 5, 10);
            assertEquals(vm1.getFlows().size(), 3);
            assertEquals(vm2.getFlows().size(), 3);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            // Here, no path should be there because there was no routing
            assertFalse(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 1);

            countTestSystem.doFullCheck(Tenant.class, 0, i + 1, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, i + 1, (i + 1) + 6, 0);
            countTestSystem.doFullCheck(Flow.class, 3, 0, 0);

        } catch (TenantManagerException e) {
            fail();
        }

        // Show now it fails because of same matching
        try {
            tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 2000000000L, 5, 10);
            fail();
        } catch (TenantManagerException e) {
            // should fail
        }

        // show fail with 2 ifc
        networkingSystem.addInterface(lundi, new NetworkInterface("3", "00:00:00:00:00:01"));
        networkingSystem.addInterface(mardi, new NetworkInterface("4", "00:00:00:00:00:02"));
        try {
            // Creating two VMs on the two different hosts
            vm1 = tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            vm2 = null;
            int i = 0;
            while(vm2 == null || vm2.getHostMachine() == vm1.getHostMachine())
                vm2 = tenantManagerSystem.createVirtualMachine(tenant, "nVm" + ++i);
            tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 10);
            fail();
        } catch (TenantManagerException e) {
            // Should work because we have 2 ifcs per VM
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 0);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 0);
        }
    }

    @Test
    public void testDeleteFlow() throws UnknownHostException {
        VirtualMachine vm1 = null, vm2 = null, vm3 = null;
        Tenant tenant = null;
        Host lundi = null, mardi = null;
        NetworkNode lundiNode = null, mardiNode = null;
        Flow flow = null;

        lundi = networkingSystem.createHost(network, "lundi");
        mardi = networkingSystem.createHost(network, "mardi");
        lundiNode = networkingSystem.addInterface(lundi, new NetworkInterface("1", "00:00:00:00:00:00"));
        mardiNode = networkingSystem.addInterface(mardi, new NetworkInterface("2", "00:00:00:00:00:00"));
        NetworkNode node = networkingSystem.createNode(network);
        networkingSystem.createLinkWithPriorityScheduling(lundiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, lundiNode, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(mardiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, mardiNode, 1e9 / 8, 0, new double[]{30000});

        try {
            tenant = tenantManagerSystem.createTenant("first tenant");
            // Creating two VMs on the two different hosts
            vm1 = tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            vm2 = null;
            int i = 0;
            while(vm2 == null || vm2.getHostMachine() == vm1.getHostMachine())
                vm2 = tenantManagerSystem.createVirtualMachine(tenant, "newVm" + ++i);
            vm3 = tenantManagerSystem.createVirtualMachine(tenant, "a third random one");
            countTestSystem.doFullCheck(Tenant.class, 1, i + 2, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, i + 2, i + 2, 0);

            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 10);
            assertEquals(vm1.getFlows().size(), 1);
            assertEquals(vm2.getFlows().size(), 1);
            assertEquals(vm3.getFlows().size(), 0);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            assertTrue(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 1);
            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 16, 10, 20, 5, 10);
            assertEquals(vm1.getFlows().size(), 2);
            assertEquals(vm2.getFlows().size(), 2);
            assertEquals(vm3.getFlows().size(), 0);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 4, 0);
            countTestSystem.doFullCheck(Flow.class, 2, 0, 0);
            assertTrue(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 2);

            // Remove the last flow
            tenantManagerSystem.deleteFlow(flow);
            assertEquals(vm1.getFlows().size(), 1);
            assertEquals(vm2.getFlows().size(), 1);
            assertEquals(vm3.getFlows().size(), 0);
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 2, 0);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 1);
            assertFalse(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 1);
        }
        catch(TenantManagerException e) {
            fail();
        }
    }

    @Test
    public void testDeleteVM() throws UnknownHostException {
        VirtualMachine vm1 = null, vm2 = null, vm3 = null;
        Tenant tenant = null;
        Host lundi = null, mardi = null;
        NetworkNode lundiNode = null, mardiNode = null;
        Flow flow = null;

        lundi = networkingSystem.createHost(network, "lundi");
        mardi = networkingSystem.createHost(network, "mardi");
        lundiNode = networkingSystem.addInterface(lundi, new NetworkInterface("1", "00:00:00:00:00:00"));
        mardiNode = networkingSystem.addInterface(mardi, new NetworkInterface("2", "00:00:00:00:00:00"));
        NetworkNode node = networkingSystem.createNode(network);
        networkingSystem.createLinkWithPriorityScheduling(lundiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, lundiNode, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(mardiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, mardiNode, 1e9 / 8, 0, new double[]{30000});

        try {
            tenant = tenantManagerSystem.createTenant("first tenant");
            // Creating two VMs on the two different hosts
            vm1 = tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            vm2 = null;
            int i = 0;
            while(vm2 == null || vm2.getHostMachine() == vm1.getHostMachine())
                vm2 = tenantManagerSystem.createVirtualMachine(tenant, "newVm" + ++i);
            vm3 = tenantManagerSystem.createVirtualMachine(tenant, "a third random one");
            countTestSystem.doFullCheck(Tenant.class, 1, i + 2, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, i + 2, i + 2, 0);

            int initialVMCount = i + 2;

            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 10);
            assertEquals(vm1.getFlows().size(), 1);
            assertEquals(vm2.getFlows().size(), 1);
            assertEquals(vm3.getFlows().size(), 0);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 16, 10, 20, 5, 10);
            assertEquals(vm1.getFlows().size(), 2);
            assertEquals(vm2.getFlows().size(), 2);
            assertEquals(vm3.getFlows().size(), 0);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 4, 0);
            countTestSystem.doFullCheck(Flow.class, 2, 0, 0);

            // Remove the last flow
            tenantManagerSystem.deleteFlow(flow);
            assertEquals(vm1.getFlows().size(), 1);
            assertEquals(vm2.getFlows().size(), 1);
            assertEquals(vm3.getFlows().size(), 0);
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 2, 0);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 1);
            assertFalse(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 1);

            // Now delete
            tenantManagerSystem.deleteVM(vm1);
            assertEquals(vm2.getFlows().size(), 0);
            assertEquals(vm3.getFlows().size(), 0);
            assertEquals(tenant.getVirtualMachines().size(), initialVMCount - 1);
            countTestSystem.doFullCheck(Tenant.class, 0, 1, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 2, 1);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 1);
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 0);

            tenantManagerSystem.deleteVM(vm2);
            assertEquals(vm3.getFlows().size(), 0);
            assertEquals(tenant.getVirtualMachines().size(), initialVMCount - 2);
            countTestSystem.doFullCheck(Tenant.class, 0, 1, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 1);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 0);

            tenantManagerSystem.deleteVM(vm3);
            assertEquals(tenant.getVirtualMachines().size(), initialVMCount - 3);
            countTestSystem.doFullCheck(Tenant.class, 0, 1, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 0, 1);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 0);
        }
        catch(TenantManagerException e) {
            fail();
        }
    }

    @Test
    public void testDeleteTenant() throws UnknownHostException {
        VirtualMachine vm1 = null, vm2 = null, vm3 = null;
        Tenant tenant = null;
        Host lundi = null, mardi = null;
        NetworkNode lundiNode = null, mardiNode = null;
        Flow flow = null;

        lundi = networkingSystem.createHost(network, "lundi");
        mardi = networkingSystem.createHost(network, "mardi");
        lundiNode = networkingSystem.addInterface(lundi, new NetworkInterface("1", "00:00:00:00:00:00"));
        mardiNode = networkingSystem.addInterface(mardi, new NetworkInterface("2", "00:00:00:00:00:00"));
        NetworkNode node = networkingSystem.createNode(network);
        networkingSystem.createLinkWithPriorityScheduling(lundiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, lundiNode, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(mardiNode, node, 1e9 / 8, 0, new double[]{30000});
        networkingSystem.createLinkWithPriorityScheduling(node, mardiNode, 1e9 / 8, 0, new double[]{30000});

        try {
            tenant = tenantManagerSystem.createTenant("first tenant");
            // Creating two VMs on the two different hosts
            vm1 = tenantManagerSystem.createVirtualMachine(tenant, "newVm");
            vm2 = null;
            int i = 0;
            while(vm2 == null || vm2.getHostMachine() == vm1.getHostMachine())
                vm2 = tenantManagerSystem.createVirtualMachine(tenant, "newVm" + ++i);
            vm3 = tenantManagerSystem.createVirtualMachine(tenant, "a third random one");
            countTestSystem.doFullCheck(Tenant.class, 1, i + 2, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, i + 2, i + 2, 0);

            int initialVMCount = i + 2;

            flow = tenantManagerSystem.createFlow("f1", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 15, 10, 20, 5, 10);
            assertEquals(vm1.getFlows().size(), 1);
            assertEquals(vm2.getFlows().size(), 1);
            assertEquals(vm3.getFlows().size(), 0);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            flow = tenantManagerSystem.createFlow("f2", vm1, vm2, InetAddress.getByName("0.0.0.1"), InetAddress.getByName("0.0.0.1"), 10, 16, 10, 20, 5, 10);
            assertEquals(vm1.getFlows().size(), 2);
            assertEquals(vm2.getFlows().size(), 2);
            assertEquals(vm3.getFlows().size(), 0);
            assertTrue(vm1.getFlows().contains(flow));
            assertTrue(vm2.getFlows().contains(flow));
            countTestSystem.doFullCheck(Tenant.class, 0, 0, 0);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 4, 0);
            countTestSystem.doFullCheck(Flow.class, 2, 0, 0);
            assertTrue(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 2);

            // Remove the tenant
            tenantManagerSystem.deleteTenant(tenant);
            countTestSystem.doFullCheck(Tenant.class, 0, initialVMCount, 1);
            countTestSystem.doFullCheck(VirtualMachine.class, 0, 4, initialVMCount);
            countTestSystem.doFullCheck(Flow.class, 0, 0, 2);
            assertFalse(pathMapper.isIn(flow));
            assertEquals(pathListMapper.get(network.getQueueGraph().getEntity()).getPathList().size(), 0);
        }
        catch(TenantManagerException e) {
            fail();
        }
    }
}
