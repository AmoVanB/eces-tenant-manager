package de.tum.ei.lkn.eces.tenantmanager.kspsystem;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.RootSystem;
import de.tum.ei.lkn.eces.graph.Graph;
import de.tum.ei.lkn.eces.graph.Node;
import de.tum.ei.lkn.eces.routing.algorithms.sp.ksp.KSPAlgorithm;
import de.tum.ei.lkn.eces.routing.algorithms.sp.ksp.yen.YenAlgorithm;
import de.tum.ei.lkn.eces.routing.proxies.ShortestPathProxy;
import de.tum.ei.lkn.eces.routing.requests.UnicastRequest;
import de.tum.ei.lkn.eces.routing.responses.Path;
import de.tum.ei.lkn.eces.tenantmanager.kspsystem.mappers.KShortestPathsMapper;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * System automatically computing the kSP between source/destination pairs in a graph.
 *
 * @author Amaury Van Bemten
 */
public class KSPSystem extends RootSystem {
    private KShortestPathsMapper kShortestPathsMapper;
    private int maxLevel;

    public KSPSystem(Controller controller, int maxLevel) {
        super(controller);
        kShortestPathsMapper = new KShortestPathsMapper(controller);
        this.maxLevel = maxLevel;
    }

    public void update(Graph graph) {
        this.logger.info("Computing kSPs on " + graph);
        for(Node sourceNode : graph.getNodes()) {
            KShortestPaths kSPComponent;

            // Check if component is there
            if (kShortestPathsMapper.isIn(sourceNode.getEntity())) {
                kSPComponent = kShortestPathsMapper.get(sourceNode.getEntity());
                // If kSPs are there already, remove them!
                this.logger.debug("Removing old kSPs for " + sourceNode + " -> " + sourceNode);
                kShortestPathsMapper.detachComponent(sourceNode.getEntity());
            } else {
                // Component is not even there, create it!
                kSPComponent = new KShortestPaths();
                this.logger.debug("Creating new empty kSPs for " + sourceNode + " -> " + sourceNode);
                kShortestPathsMapper.attachComponent(sourceNode.getEntity(), kSPComponent);
            }

            for (Node destinationNode : graph.getNodes()) {
                if(sourceNode == destinationNode)
                    continue;

                // The component is there but does not have info for the given destination yet
                this.logger.info("Computing kSPs for " + sourceNode + " -> " + destinationNode + "!");

                // Create array of size "maxLevel" that contains empty arrays
                kSPComponent.shortestPaths.put(destinationNode, new ArrayList<>());
                for (int level = 0; level < maxLevel; level++)
                    kSPComponent.shortestPaths.get(destinationNode).add(new ArrayList<>());

                // Computing the kSPs
                KSPAlgorithm yen = new YenAlgorithm(controller);
                yen.setProxy(new ShortestPathProxy());
                UnicastRequest kspRequest = new UnicastRequest(sourceNode, destinationNode);
                Iterator<Path> kSPsIterator = yen.iterator(kspRequest);


                // Get first shortest path
                Path firstPath;
                if (kSPsIterator.hasNext()) {
                    firstPath = kSPsIterator.next();
                    kSPComponent.shortestPaths.get(destinationNode).get(0).add(firstPath);
                } else {
                    // There's just nothing
                    return;
                }

                int lengthChanges = 0;
                int lastLength = firstPath.getPath().length;
                while (kSPsIterator.hasNext()) {
                    Path nextPath = kSPsIterator.next();
                    // Check if still same length
                    if (lastLength == nextPath.getPath().length) {
                        kSPComponent.shortestPaths.get(destinationNode).get(lengthChanges).add(nextPath);
                    } else {
                        lengthChanges++;
                        if (lengthChanges == maxLevel)
                            break;
                        else {
                            lastLength = nextPath.getPath().length;
                            kSPComponent.shortestPaths.get(destinationNode).get(lengthChanges).add(nextPath);
                        }
                    }
                }
            }
        }
    }
}
