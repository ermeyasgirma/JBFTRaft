package messages;
import java.io.Serializable;

/**
 * The HeartBeat class is used to send heartbeat reminders to other nodes in the network periodically. If we do not receive
 * a heartbeat message from each node with in a certain time period we assume they have disconnected or crashed.
 */
public class HeartBeat implements Serializable {

    private int nodeID;
    private int timeout;

    public HeartBeat(int nodeID, int timeout) {
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
        return "HeartBeat - NodeID: " + Integer.toString(nodeID) + ", Timeout: " + Integer.toString(timeout) + ". ";
    }
}