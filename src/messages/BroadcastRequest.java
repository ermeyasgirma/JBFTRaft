package messages;

/**
 * The BroadcastRequest class is used to ask the leader to inform the leader we have received a message from the client
 * and need to commit this as well as inform all other nodes in the network.
 */
public class BroadcastRequest {

    private int nodeID;
    private String message;

    public BroadcastRequest(int nodeID, String message) {
        this.nodeID = nodeID;
        this.message = message;
    }

    public int getNodeID() {
        return nodeID;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Broadcast Request - Node ID: " + Integer.toString(nodeID) + ", Message: " + message + ". ";
    }
}
