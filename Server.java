package HW3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Server {
    int port = 3001;
    // connected clients
    private List<ServerThread> clients = new ArrayList<ServerThread>();
    private int numberToGuess = -1; // 7.1 solution

    private void start(int port) {
        this.port = port;
        // server listening
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            Socket incoming_client = null;
            System.out.println("Server is listening on port " + port);
            do {
                System.out.println("waiting for next client");
                if (incoming_client != null) {
                    System.out.println("Client connected");
                    ServerThread sClient = new ServerThread(incoming_client, this);

                    clients.add(sClient);
                    sClient.start();
                    incoming_client = null;

                }
            } while ((incoming_client = serverSocket.accept()) != null);
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    protected synchronized void disconnect(ServerThread client) {
        long id = client.getId();
        client.disconnect();
        broadcast("Disconnected", id);
    }

    protected synchronized void broadcast(String message, long id) {
        if (processCommand(message, id)) {

            return;
        }
        // let's temporarily use the thread id as the client identifier to
        // show in all client's chat. This isn't good practice since it's subject to
        // change as clients connect/disconnect
        message = String.format("User[%d]: %s", id, message);
        // end temp identifier

        // loop over clients and send out the message
        Iterator<ServerThread> it = clients.iterator();
        while (it.hasNext()) {
            ServerThread client = it.next();
            boolean wasSuccessful = client.send(message);
            if (!wasSuccessful) {
                System.out.println(String.format("Removing disconnected client[%s] from list", client.getId()));
                it.remove();
                broadcast("Disconnected", id);
            }
        }
    }

    private void sendToOne(String message, long to) {
        Iterator<ServerThread> it = clients.iterator();
        while (it.hasNext()) {
            ServerThread client = it.next();
            if (client.getId() == to) {
                client.send(message);

                break;
            }
        }
    }

    private boolean processCommand(String message, long clientId) {
        System.out.println("Checking command: " + message);
        if (message.equalsIgnoreCase("disconnect")) {
            Iterator<ServerThread> it = clients.iterator();
            while (it.hasNext()) {
                ServerThread client = it.next();
                if (client.getId() == clientId) {
                    it.remove();
                    disconnect(client);

                    break;
                }
            }
            return true;
        }
     
        // 7.2 coin toss command
        else if (message.equalsIgnoreCase("flip")) {
            broadcast("Flipped a coin and got " + (Math.random() > .5 ? "heads" : "tails"), clientId);
            return true;
        }
        // end 7.2

        // 7.3 dice roller with special multi-dice format
        else if (message.startsWith("roll")) {
            String[] parts = message.split(" ");
            if (parts.length > 1) {
                String[] dice = parts[1].split("d");
                int numOfDice = 0;
                int maxValue = 0;
                try {
                    numOfDice = Integer.parseInt(dice[0]);
                } catch (NumberFormatException e) {
                }
                try {
                    maxValue = Integer.parseInt(dice[1]);
                } catch (NumberFormatException e) {
                }
                int total = 0;
                for (int i = 0; i < numOfDice; i++) {
                    total += new Random().nextInt(maxValue) + 1;
                }
                broadcast("Rolled a " + parts[1] + " and got " + total, clientId);
                return true;
            }
        }
        // end 7.3

        return false;
    }

    public static void main(String[] args) {
        System.out.println("Starting Server");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
