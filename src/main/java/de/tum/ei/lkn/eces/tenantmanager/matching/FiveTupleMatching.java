package de.tum.ei.lkn.eces.tenantmanager.matching;

import java.net.InetAddress;

/**
 * A five-tuple matching structure.
 *
 * @author Amaury Van Bemten
 */
public class FiveTupleMatching extends Matching {
    private InetAddress sourceIP;
    private InetAddress destinationIP;
    private int sourcePort;
    private int destinationPort;
    private int protocol;

    public FiveTupleMatching(InetAddress sourceIP, InetAddress destinationIP, int sourcePort, int destinationPort, int protocol) {
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
    }

    public InetAddress getSourceIP() {
        return sourceIP;
    }

    public InetAddress getDestinationIP() {
        return destinationIP;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public int getProtocol() {
        return protocol;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof FiveTupleMatching))
            return false;
        else
            return ((FiveTupleMatching) other).sourceIP.equals(sourceIP) &&
                ((FiveTupleMatching) other).destinationIP.equals(destinationIP) &&
                ((FiveTupleMatching) other).sourcePort == sourcePort &&
                ((FiveTupleMatching) other).destinationPort == destinationPort &&
                ((FiveTupleMatching) other).protocol == protocol;
    }
}
