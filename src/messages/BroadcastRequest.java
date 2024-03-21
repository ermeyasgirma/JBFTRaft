package messages;

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
