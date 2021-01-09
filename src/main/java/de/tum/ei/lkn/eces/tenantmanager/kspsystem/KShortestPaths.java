package de.tum.ei.lkn.eces.tenantmanager.kspsystem;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.graph.Node;
import de.tum.ei.lkn.eces.routing.responses.Path;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Component that stores, for each node, a map which contains, for each destination node, the kSPs.
 *
 * The set of kSPs is: all shortest paths of equal length + all second shortest paths of equal length + etc.
 * The limit is configured by the KSPSystem.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = KSPSystem.class)
public class KShortestPaths extends Component {
    /* For each destination node:
     *  idx of the list: which SPs (first ones, second, etc.)
     *  next list: paths of this length
     */
    HashMap<Node, List<List<Path>>> shortestPaths;

    KShortestPaths() {
        shortestPaths = new HashMap<>();
    }

    public boolean hasShortestPathsTo(Node destination) {
        return shortestPaths.containsKey(destination);
    }

    public List<Path> getShortestPaths(Node destination, int level) {
        if(!shortestPaths.containsKey(destination))
            throw new RuntimeException("unknown destination node");
        if(level >= shortestPaths.get(destination).size())
            throw new RuntimeException("too many kSPs asked");
        return Collections.unmodifiableList(shortestPaths.get(destination).get(level));
    }
}
