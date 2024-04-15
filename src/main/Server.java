package main;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import messages.*;


/**
 * The Server class is responsible for handling all network related activities for a raft node. This includes the following:
 *  - connecting to the other nodes
 *  - receiving messages from the other nodes and triggering the correct actions from the node class
 *  - sending messages to the other nodes
 *  - scheduling outbound heartbeat messages to other nodes for it's associated node
 *  - setting timers to wait for inbound heartbeat messages from other nodes especially the leader 
 */
public class Server implements Runnable{

    private final static Logger LOGGER = Logger.getLogger("Server");


    private ServerSocket serverSocket;
    private List<Integer> otherNodes;
    private int port;
    private Node node;
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean closeServer;
    /*  

    */
    private Map<Integer, Boolean> activeConnection;
    private Map<Integer, Socket> nameToSocket;
    private Map<Integer, ObjectOutputStream> nameToOOS;
    private Map<Integer, ClientHandler> nameToClientHander;
    //private Map<Integer, Timer> inboundHBTimerMap;
    //private Map<Integer, Timer> outboundHBTimerMap;

    //private HeartBeat hb;
    private int timeout; 
    private int connectionDelay;
    private boolean reconnecting;

    public Server(List<Integer> otherNodes, Node node, int port, int connectionDelay, boolean reconnecting) throws IOException {
        LOGGER.setLevel(Level.INFO);

        this.otherNodes = otherNodes;
        this.node = node;
        this.port = port;
        this.connectionDelay = connectionDelay;
        this.reconnecting = reconnecting;

        threadPool = Executors.newCachedThreadPool();
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        closeServer = false;      
        
        activeConnection = new HashMap<>();
        nameToOOS = new HashMap<>();
        //inboundHBTimerMap = new HashMap<>();
        //outboundHBTimerMap = new HashMap<>();
        nameToSocket = new HashMap<>();
        nameToClientHander = new HashMap<>();
        timeout = (int) ((Math.random() * (25000 - 20000)) + 20000);
        //hb = new HeartBeat(port, timeout);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    connectToAllNodes();
                    if (reconnecting) {
                        ConnectionRequest cr = new ConnectionRequest(port);
                        broadcastMessageToAllNodes(cr);
                    } else {
                        node.beginElectionTimer();
                    }
                }
            };
            scheduledExecutorService.schedule(task, connectionDelay, TimeUnit.MILLISECONDS);
            while(!closeServer) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler cHandler = new ClientHandler(clientSocket);
                threadPool.execute(cHandler);
            }


        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException when trying to run server", e);
            closeServer();
        }
    }

    public boolean checkAvailability(Socket sock, int portNumber) {
        if (sock != null && !sock.isClosed()) {
            return true;
        }
        LOGGER.log(Level.WARNING,"Connection to server at port " + Integer.toString(portNumber) + " is closed. \n");
        activeConnection.put(portNumber, false);
        //if (!outboundHBTimerMap.containsKey(portNumber)) {
           // outboundHBTimerMap.get(portNumber).cancel();
        //}
        if (!nameToClientHander.containsKey(portNumber)) {
            nameToClientHander.get(portNumber).closeConnection();
        }
        return false;
    }

    public void connectToAllNodes() {
        for (int n : otherNodes) {
            connectToNode(n);
        }
    }

    public void connectToNode(int portNumber) {
        Socket s = null;
        try {
            s = new Socket("127.0.0.1", portNumber);
            nameToSocket.put(portNumber, s);
            activeConnection.put(portNumber, true);
        } catch (IOException socketException) {
            LOGGER.log(Level.WARNING, "IOException when trying to connect to other node at port " + Integer.toString(portNumber), socketException);
        } 
        try {
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            nameToOOS.put(portNumber, oos);                
            oos.writeObject(Integer.toString(portNumber));
            //Timer outboundHBTimer = new Timer();
            //outboundHBTimerMap.put(portNumber, outboundHBTimer);
            //outboundHBTimer.scheduleAtFixedRate(new TimerTask() {
                //@Override
                //public void run() {
                    //sendMessageToNode(hb, portNumber);
                //}
            //}, timeout, timeout); 

        } catch (IOException writeException) {
            LOGGER.log(Level.WARNING, "IOException when trying to writeObject to server node at port " + Integer.toString(portNumber), writeException);
            try {
                nameToOOS.get(portNumber).close();
                /*if (outboundHBTimerMap.containsKey(portNumber)) {
                    outboundHBTimerMap.get(portNumber).cancel();
                } */
                if (s != null) {
                    if (!s.isClosed()) {
                        s.close();
                    }
                }
                if (!s.isClosed()) {
                    s.close();
                }
            } catch (Exception e2) {
                LOGGER.log(Level.WARNING, "Exception trying to close connection to node at port " + Integer.toString(portNumber), e2);
                closeServer();
            }
            
        }
    }

    public void sendMessageToNode(Object msg, int port) {
        if (!checkAvailability(nameToSocket.get(port), port)) {
            return;
        }
        try {
            if (activeConnection.getOrDefault(port, false)) {
                LOGGER.log(Level.FINE, "We are sending object of type: " + msg.getClass().toString() + " to node at port " + Integer.toString(port));
                nameToOOS.get(port).writeObject(msg);
            } else {
                LOGGER.log(Level.WARNING, "We are not sending object of type: " + msg.getClass().toString() + " to node at port " + Integer.toString(port) + " as we are not connected to it.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception when trying to send message: " + msg + " to node server at " + Integer.toString(port), e);
        }
    }

    public void broadcastMessageToAllNodes(Object msg) {
        for (int n : otherNodes) {
            sendMessageToNode(msg, n);
        }
    }

    public void closeServer() {
        LOGGER.log(Level.SEVERE, "Closing server node at port " + Integer.toString(port));
        //closeTimerMap(inboundHBTimerMap.values());
        //closeTimerMap(outboundHBTimerMap.values());
        for (ClientHandler ch : nameToClientHander.values()) {
            ch.closeConnection();
        }
        closeSocketMap();
        closeOutputStreams();
        closeServer = true;
        scheduledExecutorService.shutdown();
        threadPool.shutdown();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException when trying to close server socket at port  " + Integer.toString(port), e);
        }
    }

    public void closeTimerMap(Collection<Timer> timers) {
        for (Timer t : timers) {
            if (t != null) {
                t.cancel();
            }
        }
    }

    public void closeOutputStreams() {
        for (Map.Entry<Integer, ObjectOutputStream> entry : nameToOOS.entrySet()) {
            if (entry.getValue() != null) {
                try {
                    entry.getValue().close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "IOException when trying to close ObjectOutputStream to server node at port " + entry.getKey().toString(), e);
                }
            }
        }
    }

    public void closeSocketMap() {
        for (Map.Entry<Integer, Socket> entry: nameToSocket.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isClosed()) {
                try {
                    entry.getValue().close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "IOException when trying to close socket connection to server node at port " + entry.getKey().toString(), e);
                }
            }
        }
    }

    public class ClientHandler implements Runnable {

        private Socket clientSocket;
        private ObjectInputStream ois;
        private Integer nodeID;
        private ExecutorService clientThreadPool;


        public ClientHandler(Socket socket) {
            clientSocket = socket;
            clientThreadPool = Executors.newCachedThreadPool();
        }

        public void run() {

            try {

                if (clientSocket.isConnected()) {
                    ois = new ObjectInputStream(clientSocket.getInputStream());
                } else {
                    closeConnection();
                }

                Object message;

                while (true) {
                    message = readObject();
                    if (message == null) {
                        LOGGER.log(Level.WARNING, "We have read an empty object");
                        closeConnection();
                        break;
                    }
                    if (message instanceof String) {
                        nodeID = Integer.parseInt((String) message);
                    }
                    LOGGER.log(Level.FINE, "Node " + Integer.toString(port) + " receiving: " + message);
                    if (nodeID != null) {
                        activeConnection.put(nodeID, true);
                    }
                    if (nodeID != null) {
                        activeConnection.put(nodeID, true);
                    }
                    if (message instanceof ConnectionRequest) {
                        ConnectionRequest cr = (ConnectionRequest) message;

                        nodeID = cr.getNodeID();
                        activeConnection.put(nodeID, true);
                        node.reAddNodeToAliveNodes(nodeID);
                        connectToNode(nodeID);
                    } 
                    if (message instanceof HeartBeat) {
                        HeartBeat hbMsg = (HeartBeat) message;
                        LOGGER.log(Level.INFO, "Receiving leader heartbeat message");
                        node.restartElectionTimer();
                        Integer currLeader = node.getCurrLeader();
                        if (currLeader == null) {
                            node.setCurrLeader(hbMsg.getNodeID());
                        }
                        if (currLeader != null && hbMsg.getNodeID() != currLeader) {
                            LOGGER.log(Level.WARNING, "We have received a Heartbeat message from a node " + Integer.toString(nodeID) + ", which we do not believe to be the leader.");
                        }
                    }
                    if (message instanceof LogRequest) {
                        LogRequest lreq = (LogRequest) message;
                        node.restartElectionTimer();
                        clientThreadPool.execute(() -> node.onReceivingLogRequest(lreq.getLeaderID(), lreq.getTerm(), lreq.getPrefixLen(), lreq.getPrefixTerm(), lreq.getLeaderCommitLen(), lreq.getSuffix()));
                    } 
                    if (message instanceof LogResponse) {
                        LogResponse lresp = (LogResponse) message;
                        clientThreadPool.execute(() -> node.onReceivingLogResponse(lresp.getNodeID(), lresp.getTerm(), lresp.getAckLength(), lresp.getGranted()));
                    }
                    if (message instanceof VoteRequest) {
                        VoteRequest vreq = (VoteRequest) message;
                        node.restartElectionTimer();
                        LOGGER.log(Level.INFO, "Restart election timer after receiving vote request");
                        LOGGER.log(Level.INFO, "Node at port " + Integer.toString(nodeID) + " received VoteRequest from node at port " + Integer.toString(vreq.getNodeID()));
                        clientThreadPool.execute(() -> node.onReceivingVoteRequest(vreq.getCurrTerm(), vreq.getNodeID(), vreq.getLogLength()));
                        
                    }
                    if (message instanceof VoteResponse) {
                        VoteResponse vresp = (VoteResponse) message;
                        clientThreadPool.execute(() -> node.onReceivingVoteResponse(vresp.getNodeID(), vresp.getCurrTerm(), vresp.getGranted()));
                    }
                    if (message instanceof BroadcastRequest) {
                        BroadcastRequest breq = (BroadcastRequest) message;
                        clientThreadPool.execute(() -> node.onReceivingBroadcastRequest(breq.getMessage(), breq.getNodeID()));
                    }
                    if (message instanceof CloseConnection) {
                        CloseConnection cc = (CloseConnection) message;
                        LOGGER.log(Level.WARNING, "Closing connection at request of node " + Integer.toString(cc.getNodeID()));
                        clientThreadPool.shutdown();
                        closeServer();
                    }
                }

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IOException when trying to receive messages through ObjectInputStream", e);
                clientThreadPool.shutdown();
                closeConnection();
            }
        }

        public Object readObject() {
            Object msg = null;
            try {
                if (clientSocket.isConnected()) {
                    msg = ois.readObject();
                } else {
                    LOGGER.log(Level.WARNING, "Client socket is closed!");
                }
            } catch (InvalidClassException ice) {
                LOGGER.log(Level.WARNING, "InvalidClassException when trying to cast incoming object to message type", ice);
                closeConnection();
            } catch (IOException ie) {
                LOGGER.log(Level.SEVERE, "IOException when reading object from input stream", ie);
                clientThreadPool.shutdown();
                closeConnection();
            } catch (ClassNotFoundException ce) {
                LOGGER.log(Level.WARNING, "ClassNotFoundException when trying to cast incoming object to message type", ce);
            }
            return msg;
        }

        /*public void restartFollowerHeartbeatTimer(int hbTimeout) {
            if (nodeID == null) {
                LOGGER.log(Level.WARNING, "NodeID is null, cannot restart heartbeat timer");
                return;
            }
            if (inboundHBTimerMap.containsKey(nodeID)) {
                inboundHBTimerMap.get(nodeID).cancel();
            }
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    activeConnection.put(nodeID, false);
                    LOGGER.log(Level.WARNING, "Have not received communication from server node at port " + Integer.toString(nodeID) + " within the last " + Integer.toString(hbTimeout) + "ms.");
                    node.removeNodeFromAliveNodes(nodeID);
                    inboundHBTimerMap.get(nodeID).cancel();
                    clientThreadPool.shutdown();
                    closeConnection();
                }
            }, hbTimeout);
            inboundHBTimerMap.put(nodeID, t);
        } */

        public void closeConnection() {
            if (!clientThreadPool.isShutdown()) {
                clientThreadPool.shutdown();
            }
            if (nodeID == null) {
                LOGGER.log(Level.WARNING, "NodeID is null while trying to closeConnection");
            }
            try {
                LOGGER.log(Level.WARNING, "Closing connection to server at port " + Integer.toString(nodeID));
                activeConnection.put(nodeID, false);
                ois.close();
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IOException when trying to close connection to server node at port " + Integer.toString(nodeID), e);
            }
        }
    }
}