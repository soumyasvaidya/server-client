/*
 * Client.java
 */

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client
{
    public static final int SERVER_PORT = 6562;

    /*
     * getMessage(): This message receives the message of the day from server
     */
    static void getMessage(String serverInput){
        System.out.println(serverInput);
    }

    /*
     * storeMessage(): After the user is logged in, this code takes a message as input
     * and sends it to the server as a response. The server will then
     * store this message in the message directory.
     */
    static void storeMessage(String serverInput, String userInput, DataInputStream dataIpStr, DataOutputStream dataOpStr, BufferedReader input) {
        try {
            System.out.println(serverInput);
            serverInput = dataIpStr.readUTF();
            System.out.println(serverInput);
            if(!serverInput.equals("")) {
                userInput = input.readLine();
                dataOpStr.writeUTF(userInput);
                serverInput = dataIpStr.readUTF();
                System.out.println(serverInput);
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     * quit(): This method exits the application and closes any open socket for client when confirmed by server.
     */
    static boolean quit(DataInputStream dataIpStr, DataOutputStream dataOpStr, String serverInput, Socket clientSocket, boolean keepRunning) {
        try {
            System.out.println(serverInput);
            serverInput = dataIpStr.readUTF();
            System.out.println(serverInput);
            dataOpStr.close();
            dataIpStr.close();
            clientSocket.close();
            keepRunning = false;
            System.exit(0);
            //System.exit(SERVER_PORT);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return keepRunning;
    }

    /*
     * Shutdown: This method closes all sockets and exits the application at both client and server's end.
     */
    static void shutdown(DataInputStream dataIpStr, DataOutputStream dataOpStr, String serverInput, Socket clientSocket) {
        try {
            System.out.println(serverInput);
            if(serverInput.equals("200 OK")) {
                dataOpStr.close();
                dataIpStr.close();
                clientSocket.close();
                System.exit(2);
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args)
    {
        Socket clientSocket = null;
        DataOutputStream dataOpStr = null;
        DataInputStream dataIpStr = null;
        String userInput;
        String serverInput;
        BufferedReader input = null;
        boolean keepRunning = true;
        List<String> inputValidation= Arrays.asList("MSGGET","MSGSTORE","QUIT","SHUTDOWN","LOGOUT");


        if (args.length < 1)
        {
            System.out.println("Please provide IP Address to establish connection");
            System.exit(1);
        }

        try
        {
            clientSocket = new Socket(args[0], SERVER_PORT);
            dataOpStr = new DataOutputStream(clientSocket.getOutputStream());
            dataIpStr = new DataInputStream(clientSocket.getInputStream());
            input = new BufferedReader(new InputStreamReader(System.in));
            new Thread(new messageHandler(clientSocket)).start();

        }
        catch (UnknownHostException e)
        {
            System.err.println("Unknown host: The specified host is unreachable or does not exist");
        }
        catch (IOException e)
        {
            System.err.println("An error occurred when attempting to establish an I/O connection to the server");
        }

        if (clientSocket != null && dataOpStr != null && dataIpStr != null)
        {
            try {
                while (keepRunning == true) {
                    userInput = input.readLine();
                    if (!inputValidation.contains(userInput) && ! (userInput.startsWith("LOGIN", 0))) {
                        System.out.println("INVALID COMMAND");
                        keepRunning = true;
                    } else {
                        dataOpStr.writeUTF(userInput);
                        serverInput = dataIpStr.readUTF();
                        if (userInput.startsWith("LOGIN", 0)) {
                            System.out.println(serverInput);
                        } else {
                            switch (userInput) {
                                case "MSGGET":
                                    getMessage(serverInput);
                                    break;
                                case "MSGSTORE":
                                    storeMessage(serverInput, userInput, dataIpStr, dataOpStr, input);
                                    break;
                                case "QUIT":
                                    keepRunning = quit(dataIpStr, dataOpStr, serverInput, clientSocket, keepRunning);
                                    break;
                                case "SHUTDOWN":
                                    shutdown(dataIpStr, dataOpStr, serverInput, clientSocket);
                                    break;
                                case "LOGOUT":
                                    System.out.println(serverInput);
                                    break;
                                default:
                                    dataOpStr.flush();
                                    break;
                            }
                        }
                    }
                }
            }
            catch (IOException e)
            {
                System.err.println("IOException occurred:  " + e);
            }
        }
    }

    /*
     * messageHandler(): This method handles incoming chat messages and processes them.
     */
    static class messageHandler implements Runnable {

        final private Socket clientSocket;
        messageHandler(Socket socket) {
            clientSocket = socket;
        }
        @Override
        public void run() {
            DataInputStream in;
            BufferedReader is;
            try {
                is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                in = new DataInputStream(clientSocket.getInputStream());
                int result = in.readInt();
                if(result == 1){
                    System.out.println("The server is at maximum capacity and cannot accept additional connections at this time. \n");
                }
                else {
                    System.out.println("Connected to the server. \n");
                    while(true) {
                        if(!is.ready()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        else{
                            String message = in.readUTF();
                            System.out.println(message + "\n");
                            if ((message.equals("Thank you for using the server.")) || (message.equals("Server shutting down...")))
                                System.exit(0);
                        }

                    }
                }
            }
            catch (IOException ex) {
                System.out.println("Connection to the server has been lost.");

            }
        }
    }
}