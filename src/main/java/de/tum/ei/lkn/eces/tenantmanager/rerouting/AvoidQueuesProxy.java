package de.tum.ei.lkn.eces.tenantmanager.rerouting;

import de.tum.ei.lkn.eces.dnm.proxies.DetServProxy;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.routing.proxies.PathProxy;
import de.tum.ei.lkn.eces.routing.proxies.Proxy;
import de.tum.ei.lkn.eces.routing.requests.Request;
import de.tum.ei.lkn.eces.routing.responses.Path;

import java.util.HashSet;
import java.util.Set;

/**
 * Proxy that can increase the cost of some edges.
 *
 * @author Amaury Van Bemten
 */
public class AvoidQueuesProxy extends PathProxy {
    private double MULTIPLIER = 30000;

    private Set<Edge> highCostEdges = new HashSet<>();
    private DetServProxy underlyingProxy;

    public AvoidQueuesProxy(DetServProxy proxy) {
        underlyingProxy = proxy;
    }

    /**
     * Increases the cost of a queue.
     * @param edge the Edge of a queue.
     */
    public void addQueue(Edge edge) {
        highCostEdges.add(edge);
    }

    public Proxy getProxy() {
        return underlyingProxy;
    }

    @Override
    public double[] getNewParameters(Iterable<Edge> iterable, Edge edge, double[] doubles, Request request, boolean b) {
        return underlyingProxy.getNewParameters(iterable, edge, doubles, request, b);
    }

    @Override
    public boolean hasAccess(Iterable<Edge> iterable, Edge edge, double[] doubles, Request request, boolean b) {
        return underlyingProxy.hasAccess(iterable, edge, doubles, request, b);
    }

    @Override
    public double getCost(Iterable<Edge> iterable, Edge edge, double[] doubles, Request request, boolean b) {
        if(highCostEdges.contains(edge))
            return MULTIPLIER * underlyingProxy.getCost(iterable, edge, doubles, request, b);
        else
            return underlyingProxy.getCost(iterable, edge, doubles, request, b);
    }

    @Override
    public double[] getConstraintsValues(Iterable<Edge> iterable, Edge edge, double[] doubles, Request request, boolean b) {
        return underlyingProxy.getConstraintsValues(iterable, edge, doubles, request, b);
    }

    @Override
    public boolean register(Iterable<Edge> iterable, Edge edge, double[] doubles, Request request) {
        return underlyingProxy.register(iterable, edge, doubles, request);
    }

    @Override
    public boolean deregister(Iterable<Edge> iterable, Edge edge, double[] doubles, Request request) {
        return underlyingProxy.deregister(iterable, edge, doubles, request);
    }

    @Override
    public boolean register(Path path, Request request) {
        return underlyingProxy.register(path, request);
    }

    @Override
    public boolean deregister(Path path, Request request) {
        return underlyingProxy.deregister(path, request);
    }

    @Override
    public boolean handle(Request request, boolean b) {
        return underlyingProxy.handle(request, b);
    }

    @Override
    public int getNumberOfConstraints(Request request) {
        return underlyingProxy.getNumberOfConstraints(request);
    }

    @Override
    public int getNumberOfParameters(Request request) {
        return underlyingProxy.getNumberOfParameters(request);
    }

    @Override
    public double[] getConstraintsBounds(Request request) {
        return underlyingProxy.getConstraintsBounds(request);
    }
}
