package main;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;

import messages.*;

/*  TO DO:
 * 
 *  A: IMPLEMENT PEER TO PEER NETWORK FOR NODES
 *  B: WRITE TO PERSISTENT MEMORY FOR UPDATER METHODS
 * 
 * QUESTION - Should electionTimer be called at a fixedRate? Do I need to check the electionTimer is running before I cancel it
 * 
 * Add log statements for the following events
 * 
 * 1. A new leader is chosen
 * 2. A new election is started
 * 3. etc
 */

public class Node {

    PreservedData presData;
    
    // following 4 variables for each Raft node have to be stored in stable storage
    private int currTerm;
    private Integer votedFor;
    private List<LogItem> log;
    private int commitLength;


    // following 5 variables for each Raft node do not need to be preserved in the case of a crash
    private State currRole;
    private Integer currLeader;
    private Set<Integer> votesReceived;
    private Map<Integer, Integer> sentLength; // a mapping of follower nodes to the number of log entries we have sent them to replicate
    private Map<Integer, Integer> ackedLength; // a mapping of follower nodes to the number of log entries that follower has acknowledged receiving

    private int nodeID;
    boolean hasCrashed = false;
    private int[] otherNodes;
    private String localFileName;
    private Timer electionTimer;
    private Timer replicateLogTimer = new Timer();
    private Server server;

    /*
        TO-DO: need to confirm whether or not electiontimer should be restarted each time it is canceled
     */

    /* 
        TO-DO: change periodicallyreplicatelog timer so we only trigger it once our state changes to leader to save compute power
    */

    public Node(int port, int[] otherNodes) {
        nodeID = port;

        // initialise unpreserved variables
        currRole = State.FOLLOWER;
        currLeader = null;
        votesReceived = new HashSet<Integer>();
        sentLength = new HashMap<Integer, Integer>();
        ackedLength = new HashMap<Integer, Integer>();


        this.otherNodes = otherNodes;
        handleConnections();

        handlePersistentStorage();

        becomeCandidate();
        long randomTimer =(long) ((Math.random() * (450 - 250)) + 250);
        replicateLogTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                periodicallyReplicateLog();
            }
        }, randomTimer, randomTimer);

        Scanner scanner = new Scanner(System.in);
        Thread userInputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    onReceivingBroadcastRequest(scanner.nextLine(), port);
                }
            }
        });
        userInputThread.start();
    }

    public void handlePersistentStorage() {
        try {
            localFileName  = Integer.toString(nodeID) + ".txt";
            File f = new File(localFileName);
            boolean hasCrashed = f.createNewFile();
            if (hasCrashed) {
                recoverData();
            } else {
                initialise();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialise() {
        presData = new PreservedData(0, null, new ArrayList<>(), 0);

        setCurrTerm(0);
        setVotedFor(null);
        setLog(new ArrayList<>());
        setCommitLength(0);
    }

    public void handleConnections() {
        /* Create a way that when a node is rebooted it sends each node a message to connect to it */
        try {
            server = new Server(otherNodes, this, nodeID);
            server.run();
            // now actually connect to the other node's sockets
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // this method retrieves the values of the 4 variables which were preserved in the crash and updates current fields
    public void recoverData() {
        /* TO-DO */
    }

    // we trigger this method each time one of the 4 variables are updated
    private void updateCurrTerm(int c) throws IOException {

    }

    private void updateVotedFor(Integer v) throws IOException {

    }

    private void updateLog(List<LogItem> l) throws IOException {

    }

    private void updateCommitLength(int c) throws IOException {

    }

    // this method is called either when we suspect leader failure or an election timeout
    public void becomeCandidate() {
        setCurrTerm(currTerm + 1);
        currRole = State.CANDIDATE;
        setVotedFor(nodeID);
        votesReceived.clear();
        votesReceived.add(nodeID);

        int lastTerm = 0;
        if (log.size() > 0) {
            lastTerm = log.get(log.size() - 1).getTerm();
        }
        VoteRequest vreq = new VoteRequest(nodeID, currTerm, log.size(), lastTerm);
        
        for (int node: otherNodes) {
            server.sendMessageToNode(vreq, node);
        }

        long randomTimer =(long) ((Math.random() * (350 - 150)) + 150);
        electionTimer = new Timer();
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                becomeCandidate();
            }
        }, randomTimer);
        System.out.println("\n Election Timer started \n");
    }


    public State getCurrRole() {
        return currRole;
    }

    private void setCurrTerm(int c) {
        presData.updateCurrTerm(c);
    }

    private void setVotedFor(Integer v) {
        presData.updateVotedFor(v);
    }

    private void setLog(List<LogItem> l) {
        presData.updateLog(l);
    }

    private void appendLog(LogItem li) {
        presData.getLog().add(li);
        presData.updateLog(presData.getLog());
    }

    private void setCommitLength(int c) {
        /* TO-DO: Figure out why this method is only called once */
        presData.updateCommitLength(c);
    }

    /*  when we receive a vote request use the getter methods then pass the variables as parameters to this method
    *   as this prevents passing the VoteRequest object by reference, saving a little memory
    */
    public void onReceivingVoteRequest(int candidateTerm, int candidateID, int logLength) {
        if (candidateTerm > currTerm) {
            setCurrTerm(candidateTerm);
            currRole = State.FOLLOWER;
            setVotedFor(null);

        }
        int lastTerm = 0;
        if (log.size() > 0) {
            lastTerm = log.get(log.size()-1).getTerm();
        }
        boolean logOK = (candidateTerm > lastTerm) | (candidateTerm == lastTerm & logLength >= log.size());
        VoteResponse vresp;
        if (candidateTerm == currTerm & logOK & (votedFor == null | votedFor == candidateID)) {
            setVotedFor(candidateID);
            vresp = new VoteResponse(nodeID, currTerm, true); 

        } else {
            vresp = new VoteResponse(nodeID, currTerm, false);
        }

        server.sendMessageToNode(vresp, candidateID);
    }

    /*  when we receive a vote request use the getter methods then pass the variables as parameters to this method
    *   as this prevents passing the VoteRequest object by reference, saving a little memory
    */
    public void onReceivingVoteResponse(int voterID, int term, boolean granted) {
        if (currRole == State.CANDIDATE & term == currTerm & granted) {
            votesReceived.add(voterID);

            if (votesReceived.size() > Math.ceil((otherNodes.length + 1 + 1)/2)) {
                currRole = State.LEADER;
                currLeader = nodeID;

                electionTimer.cancel();

                for (int i : otherNodes) {
                    sentLength.put(i, log.size());
                    ackedLength.put(i, 0);
                    replicateLog(i);
                }
            }
        } else if (term > currTerm) {
            setCurrTerm(term);
            currRole = State.FOLLOWER;
            setVotedFor(null);
            electionTimer.cancel();
        }
    }

    public void onReceivingBroadcastRequest(String msg, int requestingNodeID) {
        if (currRole == State.LEADER) {
            LogItem li = new LogItem(msg, currTerm);
            appendLog(li);
            ackedLength.put(requestingNodeID, log.size());

            for (int i : otherNodes) {
                replicateLog(i);
            }
        } else {
            BroadcastRequest breq = new BroadcastRequest(nodeID, msg);
            server.sendMessageToNode(breq, currLeader);

            /*
                NOTE: a broadcast request is sent when the leader receives a message from the client, e.g. user input 
                if a follower receives a broadcast request, we forward it to the leader 
            */

        }
    }

    public void onReceivingLogRequest(int leaderID, int term, int prefixLen, int prefixTerm, int leaderCommitLen, List<LogItem> suffix) {
        if (term > currTerm) {
            setCurrTerm(term);
            currRole = State.FOLLOWER;
            setVotedFor(null);
            electionTimer.cancel();
        }
        if (term == currTerm) {
            currRole = State.FOLLOWER;
            currLeader = leaderID;
        }
        boolean logOK = (log.size() > prefixLen) & (prefixLen == 0 | log.get(prefixLen - 1).getTerm() == prefixTerm);
        LogResponse lresp;
        if (term == currTerm & logOK) {
            appendEntries(prefixLen, leaderCommitLen, suffix);
            int ackLength = prefixLen + suffix.size();
            lresp = new LogResponse(nodeID, currTerm, ackLength, true);
        } else {
            lresp = new LogResponse(nodeID, currTerm, 0, false);
        }
        server.sendMessageToNode(lresp, leaderID);
    }

    public void onReceivingLogResponse(int followerID, int term, int ack, boolean granted) {
        if (term == currTerm & currRole.equals(State.LEADER)) {
            if (granted & ack >= ackedLength.get(followerID)) {
                sentLength.put(followerID, ack);
                ackedLength.put(followerID, ack);
                /* TO-DO: COMMITLOGENTRIES() */
            } else if (sentLength.get(followerID) > 0) {
                sentLength.put(followerID, sentLength.get(followerID) - 1);
                replicateLog(followerID);
            }
        } else if (term > currTerm) {
            setCurrTerm(term);
            currRole = State.FOLLOWER;
            setVotedFor(null);
            electionTimer.cancel();
        }
    }

    // serves as a heartbeat reminder to followers that leader is still alive, and ensures logs are up to date in case of dropped messages
    public void periodicallyReplicateLog() {
        if (!currRole.equals(State.LEADER)) {return;}
        for (int followerID : otherNodes) {
            /* TO-DO: send log to node i to be replicated */
            replicateLog(followerID);
        }
    }

    public void replicateLog(int followerID){
        int prefixLen = sentLength.get(followerID);
        List<LogItem> suffix = log.subList(prefixLen, log.size()-1);
        int prefixTerm = 0;
        if (prefixLen > 0) {
            prefixTerm = log.get(log.size()-1).getTerm();
        }
        LogRequest lr = new LogRequest(nodeID , currTerm, prefixLen, prefixTerm, commitLength, suffix);
        server.sendMessageToNode(lr, followerID);
    }

    public void appendEntries(int prefixLen, int leaderCommitLen, List<LogItem> suffix) {
        if (suffix.size() > 0 & log.size() > prefixLen) {
            int index = Math.min(log.size(), prefixLen + suffix.size()) - 1;
            /* 
                TO-DO: sort out if statement below - no body
            */
            if (log.get(index).getTerm() != suffix.get( - prefixLen).getTerm());
            List<LogItem> truncatedLog = log.subList(0, prefixLen - 1);
            setLog(truncatedLog);
        }

        if (prefixLen + suffix.size() > log.size()) {
            for (int i = log.size() - prefixLen; i < suffix.size(); i++) {
                appendLog(suffix.get(i));
            }
        }

        if (leaderCommitLen > commitLength) {
            for (int i = commitLength; i < leaderCommitLen; i++) {
                System.out.println(log.get(i).getMsg());
            }
            commitLength = leaderCommitLen;
        }
    }


}