/*
 * Client.java
 */

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client
{
    public static final int SERVER_PORT = 6562;

    /* Get Message: receives Server's output as a MOTD*/
    static void get_message(String serverInput, DataInputStream is){
        System.out.println(serverInput); //just inputting from server
    }

    /* Store Message: Receives Server's output on whether user is logged in or not, then sends output to server for new MOTD */
    static void store_message(String serverInput, String userInput, DataInputStream is, DataOutputStream os, BufferedReader stdInput) {
        try {
            System.out.println(serverInput);    //inputting from server
            serverInput = is.readUTF();        //
            System.out.println(serverInput);    //
            if(!serverInput.equals("")) {
                userInput = stdInput.readLine(); //acknowledgement
                os.writeUTF(userInput);           //user's new motd
                serverInput = is.readUTF();     //aknowledgement
                System.out.println(serverInput); //
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* Quit: Receives input from the server, closes all sockets and ends the application */
    static boolean quit(DataInputStream is, DataOutputStream os, String serverInput, Socket clientSocket, boolean keep_going) {
        try {
            System.out.println(serverInput); //aknowledgement
            serverInput = is.readUTF();
            System.out.println(serverInput);
            os.close(); //closing streams
            is.close();
            clientSocket.close(); //close socket
            keep_going = false;   //end program
            System.exit(SERVER_PORT);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return keep_going;
    }

    /* Shutdown: Receives input from server whether it was successful, then closes all sockets and application*/
    static void shutdown(DataInputStream is, DataOutputStream os, String serverInput, Socket clientSocket, boolean keep_going) {
        try {
            System.out.println(serverInput);
            if(serverInput.equals("200 OK")) { //if successfully shuts down
                os.close();           //close streams
                is.close();           //
                clientSocket.close(); //close socket
                System.exit(2); //end program
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args)
    {
        Socket clientSocket = null;
        DataOutputStream os = null;
        DataInputStream is = null;
        String userInput;
        String serverInput;
        BufferedReader stdInput = null;
        boolean keep_going = true;
        //Check the number of command line parameters
        if (args.length < 1)
        {
            System.out.println("Usage: client <Server IP Address>");
            System.exit(1);
        }

        // Try to open a socket on SERVER_PORT
        // Try to open input and output streams
        try
        {
            clientSocket = new Socket(args[0], SERVER_PORT);
            os = new DataOutputStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
            stdInput = new BufferedReader(new InputStreamReader(System.in));
            new Thread(new ChatHandler(clientSocket)).start();

        }
        catch (UnknownHostException e)
        {
            System.err.println("Don't know about host: hostname");
        }
        catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to: hostname");
        }

        // If everything has been initialized then we want to write some data
        // to the socket we have opened a connection to on port 25

        if (clientSocket != null && os != null && is != null)
        {
            try
            {
                while (/*(userInput = stdInput.readLine())!= null && */keep_going == true)
                {
                    userInput = stdInput.readLine();
                    os.writeUTF(userInput);
                    serverInput = is.readUTF();
                    if (userInput.startsWith("LOGIN", 0)) { //have to do this with login because cases cannot only check the start
                        System.out.println(serverInput);
                    }
                    else if (userInput.startsWith("SEND", 0)) { //have to do this with SEND for the same reason as login
                        System.out.println(serverInput);
                    }
                    else {
                        switch(userInput) { //this switch-case checks what the user's input was, in case it is a command
                            case "MSGGET":
                                get_message(serverInput, is);
                                break;
                            case "MSGSTORE":
                                store_message(serverInput, userInput, is, os, stdInput);
                                break;
                            case "QUIT" :
                                keep_going = quit(is, os, serverInput, clientSocket, keep_going);
                                break;
                            case "SHUTDOWN" :
                                shutdown(is, os, serverInput, clientSocket, keep_going);
                                break;
                            case "LOGOUT" :
                                System.out.println(serverInput);
                                break;
                            case "WHO" :
                                System.out.println(serverInput);
                                break;
                            default :
                                os.flush();
                                break;
                        }
                    }
                }

                // close the input and output stream
                // close the socket

                //os.close(); //we only reach this if keep_going is set to 0 by shutdown() or quit()
                //is.close();
                //clientSocket.close();
            }
            catch (IOException e)
            {
                System.err.println("IOException:  " + e);
            }
        }
    }
    static class ChatHandler implements Runnable { //this handles receiving messages
        final private Socket clientSocket;
        ChatHandler(Socket socket) {
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
                    System.out.println("The server is full!\n");
                }
                else {
                    System.out.println("Connected. \n");
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
                                System.exit(1);
                        }

                    }
                }
            }
            catch (IOException ex) { //When the server fails to communicate to the client
                System.out.println("Lost connection to the server.");

            }
        }


    }
}