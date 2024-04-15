package main;

import java.io.*;
import java.net.*;
import java.util.*;
public class ServerSocketExample {
    private static int port;
    public static void main(String[] args) {
        Object o;
        o = new PreservedData();
        System.out.println(o);
        /* 
        int portNumber = Integer.parseInt(args[0]);
        port = portNumber;
        try (
            ServerSocket serverSocket = new ServerSocket(portNumber);
        ) {
            System.out.println("Server1 started. Listening for incoming connections...");

            Timer timer =  new Timer();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    System.out.println("Checking we can still do other stuff");
                    connectToOtherServer(Integer.parseInt(args[1]));
                }
            };
            timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        thread.start();
                    }
                }, 5500);


            // Accept incoming connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Server1: Connected to client.");

                // Create a thread to handle communication with the client
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting client connection: " + e.getMessage());
        }
        */
    }

    public static void connectToOtherServer(int portNumber) {
        Socket s = null;
        try {
            System.out.println("We are able to connect to the other server: " + Boolean.toString(checkAvailability(portNumber)));
            s = new Socket("127.0.0.1", portNumber);
            System.out.println("We managed to connect to a server");

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);

            out.println("Hello mate from: " + Integer.toString(port));
            String msg;
            while ((msg = in.readLine())!= null) {
                out.println(msg);
            }
             
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }

    public static boolean checkAvailability(int portNumber) {
        try {
            Socket s = new Socket("127.0.0.1", portNumber);
            return true;
        } catch (IOException e) {
            System.out.println("Server socket not available for connection");
            
        }
        return false;
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            // Read input from client and send it to Server2
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Server1 received: " + inputLine);

            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        }
    }
}

