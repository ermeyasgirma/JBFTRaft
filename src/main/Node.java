package main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import messages.*;

/**
 * The Node class is used to represent each Raft node in the network. This includes deciding what to do when we receive various messages
 * from the other nodes in the network. As well as scheduling elections when necessary and performing the relevant 
 * actions when we are a leader node. The node class also delegates data persistence and network set up to the PreservedData and Server classes
 */
public class Node {

    private final static Logger LOGGER = Logger.getLogger("Node");

    PreservedData presData;
    // local copies of preserved variables
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
    private List<Integer> otherNodes;
    private Timer replicateLogTimer = new Timer();
    private Server server;

    private int connectionDelay;
    private HeartBeat lhb;
    private int lhbPeriod;
    private boolean crashRecovery;

    private Scanner scanner;
    private ExecutorService nodeThreadPool;

    private Set<Integer> aliveNodes;

    public Timer electionTimer;
    public int electionTimeout;

    public Node(int port, List<Integer> otherNodes) {

        LOGGER.setLevel(Level.INFO);

        this.otherNodes = otherNodes;
        nodeID = port;

        // initialise unpreserved variables
        currRole = State.FOLLOWER;
        currLeader = null;
        votesReceived = new HashSet<Integer>();
        sentLength = new HashMap<Integer, Integer>();
        ackedLength = new HashMap<Integer, Integer>();

        aliveNodes = new HashSet<>(otherNodes);
        LOGGER.log(Level.INFO, "Alive nodes: " + aliveNodes);
        crashRecovery = false;

        electionTimer = new Timer();
        electionTimeout = (int) ((Math.random() * (25000 - 18000)) + 18000);

        connectionDelay = (int) ((Math.random() * (15000 - 10000)) + 10000);

        /* 
         * The servers wait for some time before they connect to one another so we don't
         *  we don't want to start sending VoteRequests before the other nodes can actually
         *  respond, so give them some time to discover one another
        */

        handlePersistentStorage();

        nodeThreadPool = Executors.newCachedThreadPool();

        nodeThreadPool.execute(() -> handleConnections());

        scanner = new Scanner(System.in);
        Runnable userInputTask = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String input = scanner.nextLine();
                    if (input.equals("exit")) {
                        CloseConnection cc = new CloseConnection(port);
                        server.broadcastMessageToAllNodes(cc);
                        server.closeServer();
                        closeNode();
                    } else if (input.equals("getlog")) {
                        System.out.println("Log at node " + Integer.toString(port) + ": " + log);   
                    } else if (input == "") {
                        System.out.println("Please enter a non empty input!");
                    } else {
                        onReceivingBroadcastRequest(input, port);
                    }
                }
            }
        };
        nodeThreadPool.execute(userInputTask);
    }

    public void handlePersistentStorage() {
        if (PreservedData.fileExists(nodeID)) {
            crashRecovery = true;
            recoverData();
        } else {
            initialise();
        }
    }

    public void initialise() {
        presData = new PreservedData();
        currTerm = 0;
        votedFor = 0;
        log = new ArrayList<LogItem>();
        commitLength = 0;
    }

    public void handleConnections() {
        try {
            server = new Server(otherNodes, this, nodeID, connectionDelay, crashRecovery);
            server.run();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IOException when trying to create server at node " + Integer.toString(nodeID), e);
        }

    }

    // this method retrieves the values of the 4 variables which were preserved in the crash and updates current fields
    public void recoverData() {
        presData = PreservedData.restoreState(nodeID);
        currTerm = presData.getCurrTerm();
        votedFor = presData.getVotedFor();
        log = presData.getLog();
        commitLength = presData.getCommitLength();
    }

    // this method is called either when we suspect leader failure or an election timeout
    public void becomeCandidate() {
        if (currRole == State.LEADER) {
            restartElectionTimer();
            return;
        }
        LOGGER.log(Level.WARNING, "Have note received a heartbeat message from the leader within last " + Integer.toString(electionTimeout) + "ms.");
        setCurrTerm(currTerm+ 1);
        currRole = State.CANDIDATE;
        setVotedFor(nodeID);
        votesReceived.clear();
        votesReceived.add(nodeID);

        int lastTerm = 0;
        if (log.size() > 0) {
            lastTerm = log.get(log.size() - 1).getTerm();
        }
        VoteRequest vreq = new VoteRequest(nodeID, currTerm, log.size(), lastTerm);
        
        LOGGER.log(Level.INFO, "Sending vote request to all nodes. ");
        server.broadcastMessageToAllNodes(vreq);

        restartElectionTimer();
    }

    public void beginElectionTimer() {
        if (!hasCrashed && currLeader == null) {
            int randomInitialElectionTimeout = (int) ((Math.random() * (25000 - 10000)) + 10000);
            electionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    becomeCandidate();
                }
            }, randomInitialElectionTimeout);
        }
    }

    public void restartElectionTimer() {
        LOGGER.log(Level.INFO, "Restarting election timer");
        if (electionTimer != null) {
            electionTimer.cancel();
        }
        electionTimer = new Timer();
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                becomeCandidate();
            }
        }, electionTimeout);
    }


    public State getCurrRole() {
        return currRole;
    }

    public void setCurrLeader(int leader) {
        currLeader = leader;
    }

    public Integer getCurrLeader() {
        return currLeader;
    }

    private void setCurrTerm(int c) {
        presData.updateCurrTerm(c, nodeID);
        currTerm = c;
    }

    private void setVotedFor(Integer v) {
        presData.updateVotedFor(v, nodeID);
        votedFor = v;
    }

    private void setLog(List<LogItem> l) {
        presData.updateLog(l, nodeID);
        log = l;
    }

    private void appendLog(LogItem li) {
        log.add(li);
        presData.updateLog(log, nodeID);
    }

    private void setCommitLength(int c) {
        presData.updateCommitLength(c, nodeID);
        commitLength = c;
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
        VoteResponse vresp = null;
        if ((candidateTerm == currTerm) && (logOK) && ((votedFor == null) || (votedFor == candidateID))) {
            setVotedFor(candidateID);
            vresp = new VoteResponse(nodeID, currTerm, true); 

        } else {
            vresp = new VoteResponse(nodeID, currTerm, false);
        }
        LOGGER.log(Level.FINE, "Node " + Integer.toString(nodeID) + " sending VoteResponse to candidate at port " + Integer.toString(candidateID));
        LOGGER.log(Level.FINE, vresp.toString());
        server.sendMessageToNode(vresp, candidateID);
    }

    /*  when we receive a vote request use the getter methods then pass the variables as parameters to this method
    *   as this prevents passing the VoteRequest object by reference, saving a little memory
    */
    public void onReceivingVoteResponse(int voterID, int term, boolean granted) {
        if (currRole == State.CANDIDATE & term == currTerm & granted) {
            LOGGER.log(Level.INFO, "Received a vote from node " + Integer.toString(voterID));
            votesReceived.add(voterID);

            if (votesReceived.size() > Math.ceil((aliveNodes.size() + 1)/2)) {

                if (currRole != State.LEADER) {LOGGER.log(Level.INFO, "Node " + Integer.toString(nodeID) + " is now the leader");}

                currRole = State.LEADER;
                currLeader = nodeID;

                lhbPeriod = (int) ((Math.random() * (15000 - 10000)) + 10000);
                lhb = new HeartBeat(nodeID, lhbPeriod);

                server.broadcastMessageToAllNodes(lhb);
                LOGGER.log(Level.INFO, "Sending leader heartbeat messages");

                scheduleLeaderHeartbeatMessages();

                replicateLogTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (!currRole.equals(State.LEADER)) {
                            replicateLogTimer.cancel();
                        } else {
                            periodicallyReplicateLog();
                        }
                    }
                }, 350, lhbPeriod);

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
        }
    }

    /** 
     * Testing doc comment
     * **/
    public void scheduleLeaderHeartbeatMessages() {
        Timer lhbScheduler = new Timer();
        lhbScheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currRole == State.LEADER) {
                    server.broadcastMessageToAllNodes(lhb);
                } else {
                    lhbScheduler.cancel();
                }
            }
        }, (long) 0, (long) lhbPeriod);

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
            LOGGER.log(Level.FINE, "Forwarding broadcast request to leader at port " + Integer.toString(currLeader));
            LOGGER.log(Level.FINE, breq.toString());
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
        }
        if (term == currTerm) {
            currRole = State.FOLLOWER;
            currLeader = leaderID;
        }
        boolean logOK = (log.size() >= prefixLen) & (prefixLen == 0 || log.get(prefixLen - 1).getTerm() == prefixTerm);
        LOGGER.log(Level.WARNING, "Log response values are " + Integer.toString(log.size()) + ", " + Integer.toString(prefixLen) + ", log is " + log + ", "+ Integer.toString(prefixTerm));
        LOGGER.log(Level.WARNING, "Term and currTerm are" + Integer.toString(term) + ", " + Integer.toString(currTerm));
        LOGGER.log(Level.WARNING, "Suffix is of size " + Integer.toString(suffix.size()) + "and value" + suffix);
        LogResponse lresp;
        if (term == currTerm & logOK) {
            appendEntries(prefixLen, leaderCommitLen, suffix);
            int ackLength = prefixLen + suffix.size();
            lresp = new LogResponse(nodeID, currTerm, ackLength, true);
        } else {
            lresp = new LogResponse(nodeID, currTerm, 0, false);
        }
        LOGGER.log(Level.FINE, "Node :" + Integer.toString(nodeID) + " sending LogResponse to leader at port " + Integer.toString(leaderID));
        LOGGER.log(Level.FINE, lresp.toString());
        server.sendMessageToNode(lresp, leaderID);
    }

    public void onReceivingLogResponse(int followerID, int term, int ack, boolean granted) {
        if (term == currTerm & currRole.equals(State.LEADER)) {
            if (granted & ack >= ackedLength.get(followerID)) {
                sentLength.put(followerID, ack);
                ackedLength.put(followerID, ack);
                commitLogEntries();
            } else if (sentLength.get(followerID) > 0) {
                sentLength.put(followerID, sentLength.get(followerID) - 1);
                replicateLog(followerID);
            }
        } else if (term > currTerm) {
            setCurrTerm(term);
            currRole = State.FOLLOWER;
            setVotedFor(null);
        }
    }

    // serves as a heartbeat reminder to followers that leader is still alive, and ensures logs are up to date in case of dropped messages
    public void periodicallyReplicateLog() {
        for (int followerID : otherNodes) {
            replicateLog(followerID);
        }
    }


    public void replicateLog(int followerID){
        if (log.size() == 0) {
            return;
        }
        int prefixLen = sentLength.get(followerID);
        LOGGER.log(Level.WARNING, "Prefix length is " + Integer.toString(prefixLen) + " and log size is " + Integer.toString(log.size()));
        // list returned by sublist() is an instance of 'RandomAccessSubList' which is not serializable. Therefore you need to create a new ArrayList object from the list returned by the subList().
        List<LogItem> suffix;
        if (prefixLen == 0 && log.size() <= 1) {
            suffix = new ArrayList<>();
            suffix.add(log.get(0));
        } else if (prefixLen > log.size() - 1) {
            return;
        } else if (log.size() - prefixLen == 1) {
            suffix = new ArrayList<>();
            suffix.add(log.get(prefixLen));
        } else {
            suffix = new ArrayList<>(log.subList(prefixLen, log.size()));
        }
        int prefixTerm = 0;
        if (prefixLen > 0) {
            prefixTerm = log.get(log.size()-1).getTerm();
        }
        LogRequest lr = new LogRequest(nodeID , currTerm, prefixLen, prefixTerm, commitLength, suffix);
        LOGGER.log(Level.FINE, "Node :" + Integer.toString(nodeID) + " sending LogRequest to follower at port " + Integer.toString(followerID));
        LOGGER.log(Level.FINE, lr.toString());
        server.sendMessageToNode(lr, followerID);
    }

    public void appendEntries(int prefixLen, int leaderCommitLen, List<LogItem> suffix) {
        if (suffix.size() > 0 & log.size() >= prefixLen) {
            int index = Math.min(log.size(), prefixLen + suffix.size()) - 1;
            if (index < 0) {index = 0;}

            // TODO
            /* 
            if (log.size() != 0 && log.get(index).getTerm() != suffix.get(index - prefixLen).getTerm()) {
                List<LogItem> truncatedLog = log.subList(0, prefixLen - 1);
                setLog(truncatedLog);
            }*/
        }

        if (prefixLen + suffix.size() > log.size()) {
            for (int i = log.size() - prefixLen; i < suffix.size(); i++) {
                LOGGER.log(Level.INFO, "We append the log value");
                appendLog(suffix.get(i));
            }
        }

        if (leaderCommitLen > commitLength) {
            for (int i = commitLength; i < leaderCommitLen; i++) {
                LOGGER.log(Level.INFO, log.get(i).getMsg());
                // Can replace print statement with any other method of replicating data, eg storing in a database
            }

            setCommitLength(leaderCommitLen);
        }
    }

    public void commitLogEntries() {
        int acks;
        while (commitLength < log.size()) {
            acks = 0;
            for (int node : otherNodes) {
                if (ackedLength.get(node) > commitLength) {
                    acks += 1;
                }
            }
            if (acks >= Math.ceil((otherNodes.size() + 1)/2)) {
                LOGGER.log(Level.INFO, log.get(commitLength).getMsg());
                setCommitLength(commitLength + 1);
                // Can replace print statement with any other method of data replication
            } else {
                break;
            }
        }
    }

    public void removeNodeFromAliveNodes(int portNumber) {
        aliveNodes.remove(portNumber);
    }

    public void reAddNodeToAliveNodes(int portNumber) {
        aliveNodes.add(portNumber);
    }

    public void closeNode() {
        scanner.close();
        nodeThreadPool.shutdown();
    }


}