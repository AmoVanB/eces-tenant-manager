package de.tum.ei.lkn.eces.tenantmanager;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.Entity;
import de.tum.ei.lkn.eces.core.MapperSpace;
import de.tum.ei.lkn.eces.core.RootSystem;
import de.tum.ei.lkn.eces.dnm.NCRequestData;
import de.tum.ei.lkn.eces.dnm.mappers.NCRequestDataMapper;
import de.tum.ei.lkn.eces.dnm.proxies.DetServProxy;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.graph.Node;
import de.tum.ei.lkn.eces.network.Host;
import de.tum.ei.lkn.eces.network.Network;
import de.tum.ei.lkn.eces.network.NetworkNode;
import de.tum.ei.lkn.eces.network.mappers.LinkMapper;
import de.tum.ei.lkn.eces.network.mappers.NetworkMapper;
import de.tum.ei.lkn.eces.network.mappers.NetworkNodeMapper;
import de.tum.ei.lkn.eces.network.mappers.ToNetworkMapper;
import de.tum.ei.lkn.eces.routing.SelectedRoutingAlgorithm;
import de.tum.ei.lkn.eces.routing.algorithms.RoutingAlgorithm;
import de.tum.ei.lkn.eces.routing.mappers.PathListMapper;
import de.tum.ei.lkn.eces.routing.mappers.PathMapper;
import de.tum.ei.lkn.eces.routing.mappers.SelectedRoutingAlgorithmMapper;
import de.tum.ei.lkn.eces.routing.mappers.UnicastRequestMapper;
import de.tum.ei.lkn.eces.routing.proxies.PathProxy;
import de.tum.ei.lkn.eces.routing.requests.UnicastRequest;
import de.tum.ei.lkn.eces.routing.responses.Path;
import de.tum.ei.lkn.eces.tenantmanager.exceptions.TenantManagerException;
import de.tum.ei.lkn.eces.tenantmanager.kspsystem.KSPSystem;
import de.tum.ei.lkn.eces.tenantmanager.kspsystem.mappers.KShortestPathsMapper;
import de.tum.ei.lkn.eces.tenantmanager.mappers.FlowMapper;
import de.tum.ei.lkn.eces.tenantmanager.mappers.TenantMapper;
import de.tum.ei.lkn.eces.tenantmanager.mappers.VirtualMachineMapper;
import de.tum.ei.lkn.eces.tenantmanager.matching.FiveTupleMatching;
import de.tum.ei.lkn.eces.tenantmanager.matching.Matching;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.AvoidQueuesProxy;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.CostIncreaseTypes;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.FlowSelectionTypes;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.SortFlowTypes;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.components.RerouteFrom;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.components.ReroutingConfiguration;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.components.ReroutingStats;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.mappers.RerouteFromMapper;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.mappers.ReroutingConfigurationMapper;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.mappers.ReroutingStatsMapper;
import de.tum.ei.lkn.eces.tenantmanager.traffic.TokenBucketTrafficContract;
import de.tum.ei.lkn.eces.tenantmanager.traffic.TrafficContract;
import de.tum.ei.lkn.eces.topologies.networktopologies.FatTree;
import de.uni_kl.cs.discodnc.curves.CurvePwAffine;
import de.uni_kl.cs.discodnc.numbers.Num;

import java.net.InetAddress;
import java.util.*;

/**
 * The tenant manager system.
 *
 * The system allows to create tenants, add VMs (which are then placed in the network) and embed flows between VMs.
 *
 * @author Amaury Van Bemten
 */
public class TenantManagerSystem extends RootSystem {
    private final Controller controller;
    private final TenantMapper tenantMapper;
    private final VirtualMachineMapper virtualMachineMapper;
    private final FlowMapper flowMapper;
    private final NetworkMapper networkMapper;
    private final UnicastRequestMapper requestMapper;
    private final NCRequestDataMapper ncRequestDataMapper;
    private final SelectedRoutingAlgorithmMapper selectedRoutingAlgorithmMapper;
    private final PathMapper pathMapper;
    private final ReroutingConfigurationMapper reroutingConfigurationMapper;
    private final ReroutingStatsMapper reroutingStatsMapper;
    private final NetworkNodeMapper networkNodeMapper;
    private final ToNetworkMapper toNetworkMapper;
    private final KShortestPathsMapper kShortestPathsMapper;
    private final LinkMapper linkMapper;
    private final PathListMapper pathListMapper;
    private final RerouteFromMapper rerouteFromMapper;

    private final RoutingAlgorithm routingAlgorithm;
    private final Network network;
    private final boolean isFatTree;
    private final FatTree networkTopology; // null if above is false
    private final Set<Tenant> tenants;

    public TenantManagerSystem(FatTree network, RoutingAlgorithm routingAlgorithm, Controller controller) {
        this(network, network.getNetwork(), routingAlgorithm, controller, true);
    }

    public TenantManagerSystem(Network network, RoutingAlgorithm routingAlgorithm, Controller controller) {
        this(null, network, routingAlgorithm, controller, false);
    }

    private TenantManagerSystem(FatTree topology, Network network, RoutingAlgorithm routingAlgorithm, Controller controller, boolean isFatTree) {
        super(controller);
        this.controller = controller;
        this.network = network;
        this.routingAlgorithm = routingAlgorithm;
        this.tenantMapper = new TenantMapper(controller);
        this.virtualMachineMapper = new VirtualMachineMapper(controller);
        this.flowMapper = new FlowMapper(controller);
        this.networkMapper = new NetworkMapper(controller);
        this.requestMapper = new UnicastRequestMapper(controller);
        this.ncRequestDataMapper = new NCRequestDataMapper(controller);
        this.reroutingStatsMapper = new ReroutingStatsMapper(controller);
        this.selectedRoutingAlgorithmMapper = new SelectedRoutingAlgorithmMapper(controller);
        this.reroutingConfigurationMapper = new ReroutingConfigurationMapper(controller);
        this.pathMapper = new PathMapper(controller);
        this.toNetworkMapper = new ToNetworkMapper(controller);
        this.kShortestPathsMapper = new KShortestPathsMapper(controller);
        this.linkMapper = new LinkMapper(controller);
        this.networkNodeMapper = new NetworkNodeMapper(controller);
        this.pathListMapper = new PathListMapper(controller);
        this.rerouteFromMapper = new RerouteFromMapper(controller);
        this.networkTopology = topology;
        this.isFatTree = isFatTree;

        this.tenants = new HashSet<>();

        // Compute k shortest paths when necessary
        if(reroutingConfigurationMapper.isIn(network.getEntity())) {
            KSPSystem kspSystem;
            switch (reroutingConfigurationMapper.get(network.getEntity()).getFlowSelectionTypes()) {
                case ONE_SHORTEST_PATH:
                case ALL_SHORTEST_PATHS:
                    if (isFatTree) {
                        networkTopology.computeEqualLengthShortestPaths();
                    } else {
                        kspSystem = new KSPSystem(controller, 1);
                        kspSystem.update(network.getLinkGraph());
                    }
                    break;
                case ALL_TWO_SHORTEST_PATHS:
                    kspSystem = new KSPSystem(controller, 2);
                    kspSystem.update(network.getLinkGraph());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Creates a new tenant.
     * @param name Name of the tenant.
     * @return the tenant object.
     * @throws TenantManagerException if another tenant with the same name exists.
     */
    public Tenant createTenant(String name) throws TenantManagerException {
        if(name == null)
            name = "null";

        logger.info("Creating tenant with name " + name);

        Tenant newTenant;

        // Making sure this name does not exist yet
        for(Tenant tenant : tenants)
            if(tenant.getName().equals(name))
                throw new TenantManagerException("tenant name already exists");

        try(MapperSpace ms = controller.startMapperSpace()) {
            Entity entity = controller.createEntity();
            newTenant = new Tenant(name);
            tenantMapper.attachComponent(entity, newTenant);
            tenants.add(newTenant);
        }

        logger.info("Tenant " + newTenant + " created.");
        return newTenant;
    }

    /**
     * Creates a new VM for a given tenant.
     * @param tenant tenant for which the VM should be created.
     * @param name name of the VM.
     * @return the created VM.
     * @throws TenantManagerException if a VM with that name already exists for the tenant, if there are no hosts
     * in the network, if the new VM could not be allocated to any host.
     */
    public VirtualMachine createVirtualMachine(Tenant tenant, String name) throws TenantManagerException {
        return createVirtualMachine(tenant, name, null);
    }

    /**
     * Creates a new VM for a given tenant.
     * @param tenant tenant for which the VM should be created.
     * @param name name of the VM.
     * @param host host on which to place the VM.
     * @return the created VM.
     * @throws TenantManagerException if a VM with that name already exists for the tenant, if there are no hosts
     * in the network, if the specified host is invalid or cannot run the new VM.
     */
    public VirtualMachine createVirtualMachine(Tenant tenant, String name, Host host) throws TenantManagerException {
        if(tenant == null || name == null)
            throw new TenantManagerException("no null element accepted");
        if(host != null && network != host.getNetwork())
            throw new TenantManagerException("the proposed host is not in the same network");

        logger.info("Creating VM " + name + " for tenant " + tenant);

        VirtualMachine newVm = new VirtualMachine(tenant, name);
        try(MapperSpace ms = controller.startMapperSpace()) {
            tenantMapper.acquireReadLock(tenant);
            // Making sure a VM with this name does not exist
            for(VirtualMachine vm : tenant.getVirtualMachines()) {
                virtualMachineMapper.acquireReadLock(vm);
                if (vm.getName().equals(name))
                    throw new TenantManagerException("VM name already exists for tenant");
            }

            // Allocate VM to a host
            networkMapper.acquireReadLock(network);
            int nHosts = network.getHosts().size();
            if(nHosts < 1)
                throw new TenantManagerException("no hosts in the network to assign the new VM");

            Host hostToPlaceTheVm = placeVM(newVm, host);
            if(hostToPlaceTheVm == null)
                throw new TenantManagerException("could not find a place for the VM");

            Entity vmEntity = controller.createEntity();
            virtualMachineMapper.attachComponent(vmEntity, newVm);
            tenantMapper.updateComponent(tenant, () -> tenant.addVM(newVm));
        }

        logger.info("VM " + newVm + " for tenant " + tenant + " created");
        return newVm;
    }

    /**
     * Creates a new flow for a tenant.
     *
     * !!!!!
     * THIS METHOD CANNOT BE RUN IN A MAPPER SPACE
     * !!!!!
     *
     * @param name Name of the flow.
     * @param sourceVm Source VM of the flow.
     * @param destinationVm Destination VM of the flow.
     * @param srcIP Matching source IP (five-tuple matching).
     * @param dstIP Matching destination IP (five-tuple matching).
     * @param srcPort Matching source port (five-tuple matching).
     * @param dstPort Matching destination port (five-tuple matching).
     * @param protocol Matching protocol (five-tuple matching).
     * @param rate Rate of the new flow (bps)
     * @param burst Burst of the new flow (bytes)
     * @param latency Latency of the new flow (ms)
     * @return the added flow attached to the same entity as the RoutingSystem request/response.
     * @throws TenantManagerException if the two VMs do not belong to the same tenant/network, if this matching
     * structure already exists for the source VM, if the src/dst host does not have exactly one interface, if the
     * rate/latency/burst values are not strictly positive, if the flow could not be embedded in the network.
     */
    public Flow createFlow(String name, VirtualMachine sourceVm, VirtualMachine destinationVm, InetAddress srcIP, InetAddress dstIP, int srcPort, int dstPort, int protocol, long rate, long burst, double latency) throws TenantManagerException {
        if(name == null || sourceVm == null || destinationVm == null || srcIP == null || dstIP == null)
            throw new TenantManagerException("no null element accepted");

        logger.info("Received flow creation request: " + sourceVm + "->" + destinationVm + "(" + srcIP + ">" + dstIP + ":" + srcPort + ">" + dstPort + ":" + protocol + ") " + rate + " bps " + burst + " bytes " + latency + " ms");

        Flow newFlow;
        Entity newFlowEntity;
        NetworkNode srcNode, dstNode;
        Matching newMatching = new FiveTupleMatching(srcIP, dstIP, srcPort, dstPort, protocol);
        TrafficContract newTrafficContract = new TokenBucketTrafficContract(rate, burst);

        try(MapperSpace ms = controller.startMapperSpace()) {
            virtualMachineMapper.acquireReadLock(sourceVm);
            virtualMachineMapper.acquireReadLock(destinationVm);

            if (sourceVm.getTenant() != destinationVm.getTenant())
                throw new TenantManagerException("the two VMs do not belong to the same tenant");

            if(sourceVm.getHostMachine().getNetwork() != sourceVm.getHostMachine().getNetwork())
                throw new TenantManagerException("the two VMs hosts are not in the same network");

            for(Flow flow : sourceVm.getFlows()) {
                flowMapper.acquireReadLock(flow);
                if(flow.getSource() == sourceVm && flow.getMatching().equals(newMatching))
                    throw new TenantManagerException("this matching already exists for source VM");
            }

            if(sourceVm.getHostMachine().getInterfaces().size() != 1) {
                throw new TenantManagerException("Source host does not have a single interface but " + sourceVm.getHostMachine().getInterfaces().size() + " - that's not supported");
            }

            srcNode = sourceVm.getHostMachine().getNetworkNode(sourceVm.getHostMachine().getInterfaces().iterator().next());

            if(destinationVm.getHostMachine().getInterfaces().size() != 1) {
                throw new TenantManagerException("Destination host does not have a single interface but " + destinationVm.getHostMachine().getInterfaces().size() + " - that's not supported");
            }

            dstNode = destinationVm.getHostMachine().getNetworkNode(destinationVm.getHostMachine().getInterfaces().iterator().next());

            if(rate <= 0 || burst <= 0 || latency <= 0)
                throw new TenantManagerException("rate/burst/latency must be strictly positive");
        }

        // Check if we want to try to reroute other flows
        boolean rerouteOtherFlows = reroutingConfigurationMapper.isIn(sourceVm.getHostMachine().getNetwork().getEntity());

        newFlow = new Flow(name, sourceVm, destinationVm, newMatching, newTrafficContract);

        if(srcNode == dstNode) {
            logger.info("Both VMs are on the same host, no routing");
            // The two VMs are on the same host: always accept, Flow is then added to an empty entity
            newFlowEntity = controller.createEntity();
        }
        else {
            logger.info("Creating routing request");
            // Creating request
            UnicastRequest newFlowRequest = new UnicastRequest(srcNode.getQueueNode(), dstNode.getQueueNode());
            NCRequestData newFlowNCRequestData = new NCRequestData(CurvePwAffine.getFactory().createTokenBucket(((double) rate) / 8, burst), Num.getFactory().create(latency / 1000));
            SelectedRoutingAlgorithm selectedRoutingAlgorithm = new SelectedRoutingAlgorithm(routingAlgorithm);

            try (MapperSpace ms = controller.startMapperSpace()) {
                newFlowEntity = controller.createEntity();
                requestMapper.attachComponent(newFlowEntity, newFlowRequest);
                ncRequestDataMapper.attachComponent(newFlowEntity, newFlowNCRequestData);
                selectedRoutingAlgorithmMapper.attachComponent(newFlowEntity, selectedRoutingAlgorithm);
            }

            // Check that the flow was routed
            Path path = pathMapper.get(newFlowEntity);
            if (path == null && !rerouteOtherFlows) {
                throw new TenantManagerException("unable to embed the flow");
            }
            else if(path == null) {
                List<Flow> reconfigurationAttempts = new LinkedList<>();
                List<Flow> flowsReconfigured = new LinkedList<>();
                List<Flow> reconfigurationSuccesses = new LinkedList<>();
                ReroutingConfiguration reroutingConfig = reroutingConfigurationMapper.get(sourceVm.getHostMachine().getNetwork().getEntity());

                // Step 1: determine which flows to reroute
                List<Entity> flowsToReroute = selectFlowsToReroute(newFlowRequest, reroutingConfig.getFlowSelectionTypes());
                int maxFlowsToReroute;
                switch(reroutingConfig.getLimitReroutingTypes()) {
                    case ABSOLUTE:
                        maxFlowsToReroute = reroutingConfig.getRerouteLimit();
                        break;
                    case PERCENT:
                        // +1 to have at least 1: e.g., 2 * 10/100 = 0.
                        maxFlowsToReroute = ((flowsToReroute.size() * reroutingConfig.getRerouteLimit()) / 100) + 1;
                        break;
                    default:
                        maxFlowsToReroute = flowsToReroute.size();
                        break;
                }
                maxFlowsToReroute = Math.min(flowsToReroute.size(), maxFlowsToReroute);
                this.logger.info("Will try at most " + maxFlowsToReroute + " reroutings, out of " + flowsToReroute.size() + " possible");

                // Step 2: Sort the flows
                sortFlows(flowsToReroute, newFlowRequest, reroutingConfig.getSortFlowTypes());

                // Step 3: Now we reroute one by one and retry to embed the new flow
                for (Entity flowToRerouteEntity : flowsToReroute.subList(0, maxFlowsToReroute)) {
                    reconfigurationAttempts.add(flowMapper.get(flowToRerouteEntity));
                    this.logger.info("Rerouting attempt #" + reconfigurationAttempts.size() + " - flow " + flowToRerouteEntity.getId());

                    // 3.1: Get the original request and path
                    UnicastRequest requestOfReroutedFlow = requestMapper.get(flowToRerouteEntity);
                    Path originalPathOfReroutedFlow = pathMapper.get(flowToRerouteEntity);
                    NCRequestData ncDataOfReroutedFlow = ncRequestDataMapper.get(flowToRerouteEntity);

                    // 3.2: Increase cost of some edges
                    DetServProxy originalProxy = (DetServProxy) routingAlgorithm.getProxy();
                    routingAlgorithm.setProxy(getCostIncreaseProxy(originalProxy, flowToRerouteEntity, originalPathOfReroutedFlow, reroutingConfig.getCostIncreaseTypes()));

                    // 3.3: Find a new route for the flow to reroute
                    Entity newEntityFlowToReroute = controller.createEntity();
                    try (MapperSpace ms = controller.startMapperSpace()) {
                        requestMapper.attachComponent(newEntityFlowToReroute, new UnicastRequest(requestOfReroutedFlow.getSource(), requestOfReroutedFlow.getDestination()));
                        ncRequestDataMapper.attachComponent(newEntityFlowToReroute, new NCRequestData(CurvePwAffine.getFactory().createTokenBucket(ncDataOfReroutedFlow.getTb().getUltAffineRate(), ncDataOfReroutedFlow.getTb().getBurst()), ncDataOfReroutedFlow.getDeadline()));
                        selectedRoutingAlgorithmMapper.attachComponent(newEntityFlowToReroute, new SelectedRoutingAlgorithm(routingAlgorithm));
                        rerouteFromMapper.attachComponent(newEntityFlowToReroute, new RerouteFrom(flowToRerouteEntity));
                    }

                    // Reset proxy
                    routingAlgorithm.setProxy(originalProxy);

                    // If rerouting fails
                    if (!pathMapper.isIn(newEntityFlowToReroute)) {
                        this.logger.info("Rerouting of " + flowMapper.get(flowToRerouteEntity) + " failed!");
                        continue;
                    }

                    this.logger.info("Rerouting of " + flowMapper.get(flowToRerouteEntity) + " successful!");
                    flowsReconfigured.add(flowMapper.get(flowToRerouteEntity));

                    // 3.4: Deregister the previous path of the flow
                    try (MapperSpace ms = controller.startMapperSpace()) {
                        requestMapper.detachComponent(flowToRerouteEntity);
                    }

                    // Update Flow Component location
                    Flow reroutedFlow = flowMapper.detachComponent(flowToRerouteEntity);
                    flowMapper.attachComponent(newEntityFlowToReroute, reroutedFlow);

                    // Retry to add the new flow, if rerouting was successful
                    this.logger.info("Re-adding the new flow... ");
                    newFlowEntity = controller.createEntity();
                    try (MapperSpace ms = controller.startMapperSpace()) {
                        requestMapper.attachComponent(newFlowEntity, newFlowRequest);
                        ncRequestDataMapper.attachComponent(newFlowEntity, newFlowNCRequestData);
                        selectedRoutingAlgorithmMapper.attachComponent(newFlowEntity, new SelectedRoutingAlgorithm(routingAlgorithm));
                    }

                    if (pathMapper.isIn(newFlowEntity)) {
                        this.logger.info("Re-routing and re-adding successful!");
                        reconfigurationSuccesses.add(reroutedFlow);
                        break; // If rerouting and re-adding is successful, don't try to reroute more.
                    }

                    this.logger.info("Re-adding the new flow failed!");
                }

                reroutingStatsMapper.attachComponent(newFlowEntity, new ReroutingStats(reconfigurationAttempts, reconfigurationSuccesses, flowsReconfigured));

                if(reconfigurationSuccesses.size() == 0)
                    throw new TenantManagerException("unable to embed the flow");
            }
            else {
                // Path was found! OK!
                if(rerouteOtherFlows)
                    reroutingStatsMapper.attachComponent(newFlowEntity, new ReroutingStats(new LinkedList<>(), new LinkedList<>(), new LinkedList<>()));
            }
        }

        flowMapper.attachComponent(newFlowEntity, newFlow);
        virtualMachineMapper.updateComponent(sourceVm, () -> sourceVm.addFlow(newFlow));
        virtualMachineMapper.updateComponent(destinationVm, () -> destinationVm.addFlow(newFlow));
        logger.info("Flow " + newFlow + " from " + sourceVm + " to " + destinationVm + " created");
        return newFlow;
    }

    public void deleteFlow(Flow flow) {
        if(flow == null)
            return;

        logger.info("Deleting " + flow);

        try(MapperSpace ms = controller.startMapperSpace()) {
            flowMapper.acquireReadLock(flow);
            virtualMachineMapper.acquireReadLock(flow.getSource());
            virtualMachineMapper.acquireReadLock(flow.getDestination());
            virtualMachineMapper.updateComponent(flow.getSource(), () -> flow.getSource().removeFlow(flow));
            virtualMachineMapper.updateComponent(flow.getDestination(), () -> flow.getDestination().removeFlow(flow));

            // Remove request and flow
            requestMapper.detachComponent(flow);
            flowMapper.detachComponent(flow);
        }
    }

    public void deleteVM(VirtualMachine vm) {
        if(vm == null)
            return;

        logger.info("Deleting " + vm);

        try(MapperSpace ms = controller.startMapperSpace()) {
            virtualMachineMapper.acquireReadLock(vm);
            for(Flow flow : vm.getFlows())
                this.deleteFlow(flow);
            tenantMapper.updateComponent(vm.getTenant(), () -> vm.getTenant().removeVM(vm));
            virtualMachineMapper.detachComponent(vm);
        }
    }

    public void deleteTenant(Tenant tenant) {
        if(tenant == null)
            return;

        logger.info("Deleting " + tenant);

        try(MapperSpace ms = controller.startMapperSpace()) {
            tenantMapper.acquireReadLock(tenant);
            Set<Flow> flowsToRemove = new HashSet<>();
            for(VirtualMachine vm : tenant.getVirtualMachines()) {
                // We cannot simply delete VM otherwise it will try to delete twice each flow
                virtualMachineMapper.acquireReadLock(vm);
                flowsToRemove.addAll(vm.getFlows());
            }

            // Delete the flows
            for(Flow flow : flowsToRemove)
                this.deleteFlow(flow);

            // Delete now the VMs
            for(VirtualMachine vm : tenant.getVirtualMachines()) {
                tenantMapper.updateComponent(vm.getTenant(), () -> vm.getTenant().removeVM(vm));
                virtualMachineMapper.detachComponent(vm);
            }

            tenantMapper.detachComponent(tenant);
        }
    }

    /**
     * Places a VM in the network.
     * @param vm VM to place.
     * @param host host where to place the VM or null if to be decided by the function.
     * @return Host where to place the VM or null if no place found.
     */
    private Host placeVM(VirtualMachine vm, Host host) {
        Host chosenHost;
        if(host == null) {
            logger.info("Placing VM " + vm + " on a host");
            int nHosts = network.getHosts().size();
            Host[] hosts = new Host[nHosts];
            network.getHosts().toArray(hosts);
            chosenHost = hosts[new Random().nextInt(nHosts)];
            logger.info("Host " + chosenHost + " chosen");
        }
        else {
            logger.info("Placing VM " + vm + " on " + host + " as requested");
            if(host.getNetwork() != network) {
                logger.error("The forced host for hosting the VM is no in the same network...");
                return null;
            }
            else
                chosenHost = host;
        }
        virtualMachineMapper.updateComponent(vm, () -> vm.setHostMachine(chosenHost));
        return chosenHost;
    }

    private List<Entity> selectFlowsToReroute(UnicastRequest request, FlowSelectionTypes flowSelectionType) {
        Node linkSourceNode = networkNodeMapper.get(toNetworkMapper.get(request.getSource().getEntity()).getNetworkEntity()).getLinkNode();
        Node linkDestinationNode = networkNodeMapper.get(toNetworkMapper.get(request.getDestination().getEntity()).getNetworkEntity()).getLinkNode();

        Set<Entity> setOfFlows = new HashSet<>();

        List<Edge[]> pathsToConsider = new ArrayList<>();
        switch (flowSelectionType) {
            case ALL_FLOWS:
                return new ArrayList<>(pathListMapper.get(request.getSource().getGraph().getEntity()).getPathList());
            case ONE_SHORTEST_PATH:
                Edge[] path;
                if (isFatTree) {
                    path = networkTopology.getEqualLengthShortestPaths(linkSourceNode, linkDestinationNode).get(0);
                } else {
                    path = kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 0).get(0).getPath();
                }
                pathsToConsider.add(path);
                break;
            case ALL_SHORTEST_PATHS:
                if (isFatTree) {
                    pathsToConsider.addAll(networkTopology.getEqualLengthShortestPaths(linkSourceNode, linkDestinationNode));
                } else {
                    for (Path p : kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 0))
                        pathsToConsider.add(p.getPath());
                }
                break;
            case ALL_TWO_SHORTEST_PATHS:
                for (Path p : kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 0))
                    pathsToConsider.add(p.getPath());
                for (Path p : kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 1))
                    pathsToConsider.add(p.getPath());
                break;
            default:
                break;
        }

        // Compute set of edges (that prevents from trying to add several times the same edge)
        Set<Entity> setOfEdges = new HashSet<>();
        for(Edge[] path : pathsToConsider) {
            for(Edge linkEdge : path) {
                Edge[] queueEdges = linkMapper.get(toNetworkMapper.get(linkEdge.getEntity()).getNetworkEntity()).getQueueEdges();
                for(Edge queueEdge : queueEdges)
                    setOfEdges.add(queueEdge.getEntity());
            }
        }

        for(Entity queueEdgeEntity : setOfEdges)
            setOfFlows.addAll(pathListMapper.get(queueEdgeEntity).getPathList());

        return new ArrayList<>(setOfFlows);
    }

    private void sortFlows(List<Entity> selectedFlows, UnicastRequest request, SortFlowTypes sortFlowType) {
        Node linkSourceNode = networkNodeMapper.get(toNetworkMapper.get(request.getSource().getEntity()).getNetworkEntity()).getLinkNode();
        Node linkDestinationNode = networkNodeMapper.get(toNetworkMapper.get(request.getDestination().getEntity()).getNetworkEntity()).getLinkNode();

        Comparator<Entity> comparator = null;

        switch(sortFlowType) {
            case COMMON_EDGES_SORT:
                Set<Edge> setOfPhysicalEdges = new HashSet<>();
                if (isFatTree) {
                    List<Edge[]> shortestPaths = networkTopology.getEqualLengthShortestPaths(linkSourceNode, linkDestinationNode);
                    for(Edge[] sp : shortestPaths)
                        for (Edge edge : sp)
                            setOfPhysicalEdges.add(edge);
                } else {
                    List<Path> shortestPaths = kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 0);
                    for(Path sp : shortestPaths) {
                        for (Edge edge : sp.getPath())
                            setOfPhysicalEdges.add(edge);
                    }
                }

                comparator = Comparator.comparingInt((Entity e) -> {
                    int commonEdges = 0;
                    for(Edge queueEdge : pathMapper.get(e).getPath()) {
                        Edge linkEdge = linkMapper.get(toNetworkMapper.get(queueEdge.getEntity()).getNetworkEntity()).getLinkEdge();
                        if(setOfPhysicalEdges.contains(linkEdge))
                            commonEdges++;
                    }
                    return commonEdges;
                });
                comparator = comparator.reversed();
                break;
            case RATE_SORT:
                comparator = Comparator.comparingDouble((Entity e) -> ncRequestDataMapper.get(e).getTb().getUltAffineRate().doubleValue());
                comparator = comparator.reversed();
                break;
            case BURST_SORT:
                comparator = Comparator.comparingDouble((Entity e) -> ncRequestDataMapper.get(e).getTb().getBurst().doubleValue());
                comparator = comparator.reversed();
                break;
            case DELAY_SORT:
                comparator = Comparator.comparingDouble((Entity e) -> ncRequestDataMapper.get(e).getDeadline().doubleValue());
                comparator = comparator.reversed();
                break;
            case NO_SORT:
                break;
        }

        if(comparator != null)
            selectedFlows.sort(comparator.thenComparingLong(Entity::getId));
    }

    private PathProxy getCostIncreaseProxy(DetServProxy originalProxy, Entity flowToRerouteEntity, Path originalPathOfReroutedFlow, CostIncreaseTypes costIncreaseType) {
        UnicastRequest request = requestMapper.get(flowToRerouteEntity);
        Node linkSourceNode = networkNodeMapper.get(toNetworkMapper.get(request.getSource().getEntity()).getNetworkEntity()).getLinkNode();
        Node linkDestinationNode = networkNodeMapper.get(toNetworkMapper.get(request.getDestination().getEntity()).getNetworkEntity()).getLinkNode();

        AvoidQueuesProxy increasedCostProxy = new AvoidQueuesProxy(originalProxy);

        // SP part
        Edge[] queues;
        switch (costIncreaseType) {
            case PHYSICAL_LINK_INCREASE_AND_SP:
            case QUEUE_LINK_INCREASE_AND_SP:
                // Just the queues on the SP
                if (isFatTree) {
                    queues = networkTopology.getEqualLengthShortestPaths(linkSourceNode, linkDestinationNode).get(0);
                } else {
                    queues = kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 0).get(0).getPath();
                }
                for(int i = 1; i < queues.length - 1; i++) // ignore first and last queues as these are host links
                    for (Edge queueToAdd : linkMapper.get(toNetworkMapper.get(queues[i].getEntity()).getNetworkEntity()).getQueueEdges())
                        increasedCostProxy.addQueue(queueToAdd);
                break;
            case PHYSICAL_LINK_INCREASE_AND_ALL_TWO_SP:
            case QUEUE_LINK_INCREASE_AND_ALL_TWO_SP:
                for(Path path : kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 1)) {
                    queues = path.getPath();
                    for(int i = 1; i < queues.length - 1; i++) // ignore first and last queues as these are host links
                        for (Edge queueToAdd : linkMapper.get(toNetworkMapper.get(queues[i].getEntity()).getNetworkEntity()).getQueueEdges())
                            increasedCostProxy.addQueue(queueToAdd);
                }
            case PHYSICAL_LINK_INCREASE_AND_ALL_SP:
            case QUEUE_LINK_INCREASE_AND_ALL_SP:
                if (isFatTree) {
                    for(Edge[] path : networkTopology.getEqualLengthShortestPaths(linkSourceNode, linkDestinationNode)) {
                        for(int i = 1; i < path.length - 1; i++) // ignore first and last queues as these are host links
                            for (Edge queueToAdd : linkMapper.get(toNetworkMapper.get(path[i].getEntity()).getNetworkEntity()).getQueueEdges())
                                increasedCostProxy.addQueue(queueToAdd);
                    }
                } else {
                    for(Path path : kShortestPathsMapper.get(linkSourceNode.getEntity()).getShortestPaths(linkDestinationNode, 0)) {
                        for(int i = 1; i < path.getPath().length - 1; i++) // ignore first and last queues as these are host links
                            for (Edge queueToAdd : linkMapper.get(toNetworkMapper.get(path.getPath()[i].getEntity()).getNetworkEntity()).getQueueEdges())
                                increasedCostProxy.addQueue(queueToAdd);
                    }
                }
        }

        // Existing path part
        switch (costIncreaseType) {
            case QUEUE_LINK_INCREASE:
            case QUEUE_LINK_INCREASE_AND_SP:
            case QUEUE_LINK_INCREASE_AND_ALL_SP:
            case QUEUE_LINK_INCREASE_AND_ALL_TWO_SP:
                queues = originalPathOfReroutedFlow.getPath();
                for(int i = 1; i < queues.length; i++) // ignore first queue as these are host links (keep the last one because it's only per-queue thing)
                    increasedCostProxy.addQueue(queues[i]);
                break;
            case PHYSICAL_LINK_INCREASE:
            case PHYSICAL_LINK_INCREASE_AND_SP:
            case PHYSICAL_LINK_INCREASE_AND_ALL_SP:
            case PHYSICAL_LINK_INCREASE_AND_ALL_TWO_SP:
                queues = originalPathOfReroutedFlow.getPath();
                for(int i = 1; i < queues.length; i++) { // ignore first queue and last as these are host links
                    for (Edge queueToAdd : linkMapper.get(toNetworkMapper.get(queues[i].getEntity()).getNetworkEntity()).getQueueEdges())
                        increasedCostProxy.addQueue(queueToAdd);
                }
                break;
        }

        return increasedCostProxy;
    }
}
