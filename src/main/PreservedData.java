package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PreservedData class is used to preserve the critical state of the Raft node in the case of a crash.
 * We serialize the relevant state to a file each time the variables are updated, and deserialize upon crash recovery.
 * Each node has it's own serialization file.
 */

public class PreservedData implements Serializable {

    private static final long serialVersionUID = -919904573733076189L;
    private final static Logger LOGGER = Logger.getLogger("PreservedData");

    private int currTerm;
    private Integer votedFor;
    private List<LogItem> log;
    private int commitLength;

    public PreservedData() {
        LOGGER.setLevel(Level.INFO);
        this.currTerm = 0;
        this.votedFor = 0;
        this.log = new ArrayList<LogItem>();
        this.commitLength = 0;
    }

    public void saveState(int nodeID) {
        String fileName = Integer.toString(nodeID) + ".ser";
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            fos.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IOException when trying to save state to the file " + fileName, e);
        }
    }

    public static PreservedData restoreState(int nodeID) {
        PreservedData pd = null;

        String fileName = Integer.toString(nodeID) + ".ser";
        try  {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            pd = (PreservedData) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "IOException when trying to deserialize file " + fileName, ioe);
        } catch (ClassNotFoundException cne) {
            LOGGER.log(Level.WARNING, "ClassNotFoundException when trying to deserialize file " + fileName, cne);
        }
        return pd;
    }

    public static boolean fileExists(int nodeID) {
        String fileName = Integer.toString(nodeID) + ".ser";
        File f  = new File(fileName);
        return f.isFile();
    }

    public void updateCurrTerm(int c, int nodeID) {
        currTerm = c;
        saveState(nodeID);
    }

    public void updateVotedFor(Integer v, int nodeID) {
        votedFor = v;
        saveState(nodeID);
    }

    public void updateLog(List<LogItem> l, int nodeID) {
        log = l;
        saveState(nodeID);
    }

    public void updateCommitLength(int c, int nodeID) {
        commitLength = c;
        saveState(nodeID);
    }

    public int getCurrTerm() {
        return currTerm;
    }

    public Integer getVotedFor() {
        return votedFor;
    }

    public List<LogItem> getLog() {
        return log;
    }

    public int getCommitLength() {
        return commitLength;
    }
    
    @Override
    public String toString() {
        return "PreservedData - CurrTerm: " + Integer.toString(currTerm) + ", VotedFor: " + Integer.toString(votedFor) + ", Log: " + log + ", CommitLength: " + Integer.toString(commitLength) + ". ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreservedData that = (PreservedData) o;
        return currTerm == that.currTerm &&
                commitLength == that.commitLength &&
                votedFor.equals(that.votedFor) &&
                log.equals(that.log);
    }
}
