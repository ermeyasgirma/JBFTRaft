package main;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import messages.BroadcastRequest;
import messages.ConnectionRequest;
import messages.HeartBeat;
import messages.LogRequest;
import messages.LogResponse;
import messages.ReplicateLog;
import messages.VoteRequest;
import messages.VoteResponse;


public class Server implements Runnable{

    private ServerSocket serverSocket;
    private int[] otherNodes;
    private int port;
    private Node node;
    private ExecutorService threadPool;
    private boolean closeServer;
    private Map<Integer, Timer> timeoutMap;
    private Map<Integer, Timer> outboundTimerMap;
    private Map<Integer, ClientHandler> nameToConnectionMap;
    private Map<Integer, ObjectOutputStream> nameToOOS;
    private HeartBeat hb;

    public Server(int[] otherNodes, Node node, int port) throws IOException {
        this.otherNodes = otherNodes;
        this.node = node;
        this.port = port;
        threadPool = Executors.newCachedThreadPool();
        closeServer = false;
        int timeout =  (int) ((Math.random() * (3500 - 3000)) + 3000);
        hb = new HeartBeat(port, timeout);
    }

    @Override
    public void run() {
        while (!closeServer) {
            try {
                serverSocket = new ServerSocket(port);
                new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            connectToNodes();
                        }
                    },
                    15000
                );
                while(!closeServer) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler cHandler = new ClientHandler(clientSocket);
                    threadPool.execute(cHandler);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void connectToNodes() {
        Socket s = null;
        ObjectOutputStream oos = null;
        for (int n : otherNodes) {
            try {
                s = new Socket("127.0.0.1", n);
                oos = new ObjectOutputStream(s.getOutputStream());
                nameToOOS.put(n, oos);
                ConnectionRequest cr = new ConnectionRequest(port);
                oos.writeObject(cr);
                oos.writeObject(hb);

                Timer tm = new Timer();
                tm.scheduleAtFixedRate(
                    new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                nameToOOS.get(n).writeObject(hb);;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 
                    2000, 
                    2000);
                    outboundTimerMap.put(n, tm);
            } catch (IOException e) {
                try {
                    oos.close();
                    if (!s.isClosed()) {
                        s.close();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                
            }
        }
    }

    public void sendMessageToNode(Object msg, int port) {
        try {
            nameToOOS.get(port).writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeServer() {
        System.out.println("Node at port " + port + " is being closed");
        closeServer = true;
        threadPool.shutdown();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ClientHandler implements Runnable {

        private Socket clientSocket;
        private ObjectInputStream ois;
        private int nodeID;


        public ClientHandler(Socket socket) {
            clientSocket = socket;
        }

        public void run() {

            try {

                ois = new ObjectInputStream(clientSocket.getInputStream());

                try {
                    Object message;
                    while((message = ois.readObject()) != null) {
                        System.out.println("Node " + Integer.toString(port) + " receiving message:" + message);
                        if (message instanceof ConnectionRequest) {
                            ConnectionRequest cr = (ConnectionRequest) message;
                            nodeID = cr.getNodeID();
                        } 
                        if (message instanceof HeartBeat) {
                            HeartBeat hbMsg = (HeartBeat) message;
                            int hbTimeout = hbMsg.getTimeout();
                            if (hbMsg.getNodeID() != nodeID) {
                                System.out.println("Node ID from ConnectionRequest and HeartBeat messages do not match");
                                System.exit(-1);
                            }
                            if (timeoutMap.get(nodeID) == null) {
                                Timer t = new Timer();
                                t.schedule(new TimerTask() {
                                    @Override
                                    public void run () {
                                        closeConnection();;
                                    }
                                }, (long) hbTimeout);
                                timeoutMap.put(nodeID, t);
                            } else {
                                restartTimer(timeoutMap.get(nodeID), hbTimeout, nodeID);
                            }
                            
                        } 
                        if (message instanceof LogRequest) {
                            LogRequest lreq = (LogRequest) message;
                            node.onReceivingLogRequest(lreq.getLeaderID(), lreq.getTerm(), lreq.getPrefixLen(), lreq.getPrefixTerm(), lreq.getLeaderCommitLen(), lreq.getSuffix());
                        } 
                        if (message instanceof LogResponse) {
                            LogResponse lresp = (LogResponse) message;
                            node.onReceivingLogResponse(lresp.getNodeID(), lresp.getTerm(), lresp.getAckLength(), lresp.getGranted());;
                        }
                        if (message instanceof ReplicateLog) {

                        }
                        if (message instanceof VoteRequest) {
                            VoteRequest vreq = (VoteRequest) message;
                            node.onReceivingVoteRequest(vreq.getCurrTerm(), vreq.getNodeID(), vreq.getLogLength());
                        }
                        if (message instanceof VoteResponse) {
                            VoteResponse vresp = (VoteResponse) message;
                            node.onReceivingVoteResponse(vresp.getNodeID(), vresp.getCurrTerm(), vresp.getGranted());
                        }
                        if (message instanceof BroadcastRequest) {
                            BroadcastRequest breq = (BroadcastRequest) message;
                            node.onReceivingBroadcastRequest(breq.getMessage(), breq.getNodeID());
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                closeConnection();
                e.printStackTrace();
            }
        }

        public void restartTimer(Timer t, int timeout, int nodeID) {
            t.cancel();
            t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run () {
                    closeConnection();
                }
            }, (long) timeout);
        }

        public void closeConnection() {
            try {
                timeoutMap.get(nodeID).cancel();;
                outboundTimerMap.get(nodeID).cancel();
                ois.close();
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}