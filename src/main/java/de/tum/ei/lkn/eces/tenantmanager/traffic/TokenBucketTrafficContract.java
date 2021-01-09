package de.tum.ei.lkn.eces.tenantmanager.traffic;

/**
 * A token bucket traffic contract.
 *
 * @author Amaury Van Bemten
 */
public class TokenBucketTrafficContract extends TrafficContract {
    private long rate; // bps
    private long burst; // bytes

    public TokenBucketTrafficContract(long rate, long burst) {
        this.rate = rate;
        this.burst = burst;
    }


    public long getRate() {
        return rate;
    }

    public long getBurst() {
        return burst;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof TokenBucketTrafficContract))
            return false;
        else
            return (
                ((TokenBucketTrafficContract) other).rate == rate &&
                ((TokenBucketTrafficContract) other).burst == burst);
    }
}
