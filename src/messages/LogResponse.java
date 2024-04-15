package messages;
import java.io.Serializable;

/**
 * The LogResponse class is sent in response to a LogRequest, and the follower either denies or grants the request
 * to update our log entry.
 */

public class LogResponse implements Serializable {

    private int nodeID;
    private int term;
    private int ackLength;
    private boolean granted;

    public LogResponse(int nodeID, int term, int ackLength, boolean granted) {
        this.nodeID = nodeID;
        this.term = term;
        this.ackLength = ackLength;
        this.granted = granted;
    }

    public int getNodeID() {
        return nodeID;
    }

    public int getTerm() {
        return term;
    }

    public int getAckLength() {
        return ackLength;
    }

    public boolean getGranted() {
        return granted;
    }

    @Override
    public String toString() {
        return "Logresponse - NodeID: " + Integer.toString(nodeID) + ", Term: " + Integer.toString(term) + ", Acklength: " + Integer.toString(ackLength) + ", Granted: " + Boolean.toString(granted) + ". ";
    }
}