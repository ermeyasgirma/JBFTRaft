package messages;
import java.io.Serializable;

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