package messages;
import java.io.Serializable;

/**
 * The LeaderHeartBeat class is an extension of the HeartBeat class used by the leader. When the followers timers for 
 * LeaderHeartBeat messages expire, they trigger an election.
 */

public class LeaderHeartBeat implements Serializable {

    private int nodeID;
    private int timeout;

    public LeaderHeartBeat(int nodeID, int timeout) {
        this.nodeID = nodeID;
        this.timeout = timeout;
    }

    public int getNodeID() {
        return nodeID;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public String toString() {
        return "LeaderHeartBeat - NodeID: " + Integer.toString(nodeID) + ", Timeout: " + Integer.toString(timeout) + ". ";
    }
}
