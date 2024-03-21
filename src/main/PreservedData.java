package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PreservedData implements Serializable {

    private int currTerm;
    private Integer votedFor;
    private List<LogItem> log;
    private int commitLength;

    public PreservedData(int currTerm, Integer votedFor, List<LogItem> log, int commitLength) {
        this.currTerm = currTerm;
        this.votedFor = votedFor;
        this.log = log;
        this.commitLength = commitLength;
    }

    public void updateCurrTerm(int c) {

    }

    public void updateVotedFor(int v) {

    }

    public void updateLog(List<LogItem> l) {
        
    }

    public void updateCommitLength(int c) {
        
    }

    public int getCurrTerm() {
        return 0;
    }

    public Integer getVotedFor() {
        return 0;
    }

    public List<LogItem> getLog() {
        return new ArrayList<>();
    }

    public int getCommitLength() {
        return 0;
    }
    
}
