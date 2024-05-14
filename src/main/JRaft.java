package main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: change file path for serialization files


public class JRaft {
 
    public static void main(String[] args) throws IOException {
        
        int currPort = Integer.parseInt(args[0]);
        List<Integer> otherNodes = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            otherNodes.add(Integer.parseInt(args[i]));
        }

        new Node(currPort, otherNodes);
    }
}