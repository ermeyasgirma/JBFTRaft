package messages;
import java.io.Serializable;

/**
 * The Connection Request class is sent by a node after crash recovery to request other nodes to reconnect to it.
 */

public class ConnectionRequest implements Serializable {

    private int nodeID;

    public ConnectionRequest(int nodeID) {
        this.nodeID = nodeID;
    }

    public int getNodeID() {
        return nodeID;
    }

    @Override
    public String toString() {
        return "ConnectionRequest - NodeID: " + Integer.toString(nodeID) + ". ";
    }
}