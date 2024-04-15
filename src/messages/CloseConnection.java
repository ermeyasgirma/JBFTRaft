package messages;

import java.io.Serializable;

/**
 * 
 * The CloseConnection class is used to send other nodes in the network a request to close their connection, and all others.
 * It is triggered when the user inputs the string "exit", and is how we safely terminate the program.
 */

public class CloseConnection implements Serializable {
    public int nodeID;

    public CloseConnection(int nodeID) {
        this.nodeID = nodeID;
    }

    public int getNodeID() {
        return nodeID;
    }

    @Override
    public String toString() {
        return "CloseConnection - NodeID: " + Integer.toString(nodeID) + ". \n";
    }
}
