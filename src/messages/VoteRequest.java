package messages;
import java.io.Serializable;

public class VoteRequest implements Serializable {
    
    private int nodeID;
    private int currTerm;
    private int logLength;
    private int lastTerm;

    public VoteRequest(int nodeID, int currTerm, int logLength, int lastTerm) {
        this.nodeID = nodeID;
        this.currTerm = currTerm;
        this.logLength = logLength;
        this.lastTerm = lastTerm;
    }

    /* TO DO: Implement serializability */

    public int getNodeID() {
        return nodeID;
    }

    public int getCurrTerm() {
        return currTerm;
    }

    public int getLogLength() {
        return logLength;
    }

    public int getLastTerm() {
        return lastTerm;
    }

    @Override
    public String toString() {
        return "Voterequest - NodeID " + Integer.toString(nodeID) + ", Currterm: " + Integer.toString(currTerm) + ", Loglength: " + Integer.toString(logLength) + ", Lastterm: " + Integer.toString(lastTerm) + ". ";
    }
}