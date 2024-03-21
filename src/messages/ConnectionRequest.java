package messages;
import java.io.Serializable;

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