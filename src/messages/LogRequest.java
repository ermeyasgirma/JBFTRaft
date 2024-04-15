package messages;

import java.io.Serializable;
import java.util.List;

import main.LogItem;

/**
 * The LogRequest class is used by the leader to periodically ask follower nodes to update their log entries to 
 * match the leader's.
 */

public class LogRequest implements Serializable {

    private int leaderID;
    private int term;
    private int prefixLen;
    private int prefixTerm;
    private int leaderCommitLen;
    private List<LogItem> suffix;

    public LogRequest(int leaderID, int term, int prefixLen, int prefixTerm, int leaderCommitLen, List<LogItem> suffix) {
        this.leaderID = leaderID;
        this.term = term;
        this.prefixLen = prefixLen;
        this.prefixTerm = prefixTerm;
        this.leaderCommitLen = leaderCommitLen;
        this.suffix = suffix;
    }

    public int getLeaderID() {
        return leaderID;
    }

    public int getTerm() {
        return term;
    }

    public int getPrefixLen() {
        return prefixLen;
    }

    public int getPrefixTerm() {
        return prefixTerm;
    }

    public int getLeaderCommitLen() {
        return leaderCommitLen;
    }

    public List<LogItem> getSuffix() {
        return suffix;
    }

    @Override
    public String toString() {
        return "LogRequest - LeaderID: " + Integer.toString(leaderID) +  ", Term: " + Integer.toString(term) + ", Prefixlen: " + Integer.toString(prefixLen) + ", Prefixterm: " + Integer.toString(prefixTerm) + ". Leadercommitlen: " + Integer.toString(leaderCommitLen) + ", " + suffix;
    }

}