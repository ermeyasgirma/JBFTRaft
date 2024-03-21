package main;
import java.io.IOException;

public class JBFTRaft {

    /* 
     * 
     * TO-DO: watch yt vid on how to add log4j dependency
     * 
     * TO-DO: add a docstring for each file explaining what it does
     * 
     * TO-DO: extension task - only send heartbeat messages when there has been no activity for a certain amount of time, as this reduces overhead
     */


    public static void main(String[] args) throws IOException {

        // the user will enter a list of ports for each node (the ip address will be localhost) 
        // the first port will be the port for the current node instance, the rest will be for the other nodes
        // instruct the Node class created, to connect with the other nodes
        
        int currPort = Integer.parseInt(args[0]);
        int[] otherNodes = new int[16];

        for (int i = 1; i < args.length; i++) {
            otherNodes[i-1] = Integer.parseInt(args[i]);
        }

        new Node(currPort, otherNodes);
    }
}