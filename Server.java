/*
 * Server.java
 */

//import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;

public class Server {
    public static final int SERVER_PORT = 6562;
    static public int num_clients = 0, motd_count;
    //static Socket[] sockets;
    static Socket socket;
    static ServerSocket myServerice = null;
    static String[] motd = new String[20]; //active_users = new String[5];
    static boolean server_continue = true;

    static String activeUser= new String();

    /* Get Message: prints the next motd in the list to the user and prepares the next motd to be accessed */
    static int get_message(DataOutputStream os, String[] motd, int active_motd, int motd_count, Socket clientSocket) {
        int userQuery = -1;
        try {
            /*for(int i = 0; i < num_clients; i++) {
                if (sockets[i] == clientSocket) {
                    userQuery = i;
                    break;
                }
            }*/
            os = new DataOutputStream(clientSocket.getOutputStream());
            os.writeUTF("200 OK");
            os.writeUTF(motd[active_motd]);
            active_motd++;//get the next motd ready
            if( active_motd == motd_count) //if at end of array
                active_motd = 0;               //loop back to beginning
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return active_motd;
    }
    /* Login : Logs in the user by checking to see if their credentials match any users in the file users.txt and will print an error if they are already logged in */
    static String login(DataOutputStream os, String active_user, String line, Socket clientSocket) {
        try {
            Scanner inFS;
            //int userQuery = -1; //this determines which socket is attempting the login
            String temp, stored_user; //temp is used for array swapping, stored_user is what credentials will be compared against
            String[] user_password, stored_user_password; //used to split up string input for comparison
            /*for(int i = 0; i < num_clients; i++) {
                if (sockets[i] == clientSocket) {
                    userQuery = i;
                    break;
                }
            }*/
            os = new DataOutputStream(clientSocket.getOutputStream());
            if(!active_user.equals("")) {
                //failure, can't log in if already logged in
                os.writeUTF("You are already logged in as " + active_user);
            }
            else {
                try {
                    inFS = new Scanner(new File("src/users.txt")); // this will check the usernames stored in the server
                    user_password = line.split(" ");
                    if (user_password.length != 3) {
                        try {
                            //failure, username and password only are required
                            os.writeUTF("410 Wrong UserID or password");
                        } catch (IOException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println("User failed to log in.");
                    }
                    else {//intermediate step, now checking if credentials match any records
                        temp = user_password[1];                   //we have to get LOGIN out of the string
                        user_password[1] = user_password[2];
                        user_password[0] = temp;

                        while(inFS.hasNextLine()){
                            stored_user = inFS.nextLine();         //stored_user is the user we are looking at
                            stored_user_password = stored_user.split(" ");
                            if((user_password[0].equals(stored_user_password[0])) && (user_password[1].equals(stored_user_password[1]))) {
                                int i = 0;
                                while(stored_user.charAt(i) != ' ') {
                                    active_user += stored_user.charAt(i);
                                    i++;
                                }
                            }
                        }
                        inFS.close();
                        if(!active_user.equals("")) {
                            try {
                                //success, found a match and user is now logged in
                                os.writeUTF("200 OK");
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println("User logged in as " + active_user);
                        }
                        else {
                            try {
                                //failure, no matching records
                                os.writeUTF("410 Wrong UserID or password");
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println("User failed to log in.");
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return active_user;
    }
    /* Who: returns a list of users that are currently logged into the server */
    static void who(DataOutputStream os, Socket clientSocket){
        int userQuery = -1;
        try {

            os = new DataOutputStream(clientSocket.getOutputStream());
            os.writeUTF("200 OK \nThe list of the active users:");
                os.writeUTF(activeUser + "\t" + socket.getInetAddress());
            }
         catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*Send: sends a private message to the recipient of both the sender and receiver are logged in */
    static void send(DataOutputStream os, DataInputStream is, String message, String active_user, Socket clientSocket) {
        String[] recipient;
        boolean recipient_found = false;
        try {
            System.out.println("message");
            recipient = message.split(" ");
            if(recipient.length != 2) {
                os.writeUTF("syntax: SEND <target>");
            }
            else {
                    if (active_user.equals(recipient[1])) {
                        recipient_found = true;
                    }

                int userQuery = -1;

                os = new DataOutputStream(clientSocket.getOutputStream());
                if(active_user.equals("")) {
                    // fails, user is not logged in which is required
                    os.writeUTF("401 You are not logged in, please log in first.");
                    os.writeUTF("");
                }
                else if (recipient_found == false) { //server could not find the username of person to send message to
                    os.writeUTF("420 either the user does not exist or is not logged in.");
                }
                else {
                    //success
                    os.writeUTF("200 OK \n Please type message to target."); //success acknowledgement
                    //os.writeUTF("Please type message to target.");
                    message = is.readUTF(); //receive message from the user
                    os.writeUTF("200 OK");
                        if(active_user.equals(recipient[1])) {
                            os = new DataOutputStream(socket.getOutputStream());
                            os.writeUTF("(PM) " + active_user+ ": " + message);
                            System.out.println(active_user + " -> " + active_user + ": " + message);

                    }

                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /* Logout: Logs out the user if they are logged in and prints an error if they are not logged in */
    static String logout(DataOutputStream os, String active_user, Socket clientSocket) {

        try {
            os = new DataOutputStream(clientSocket.getOutputStream());
            if (active_user.equals(""))//failure, can't log out if not logged in
                os.writeUTF("405 You are not logged in.");
            else {//success, active_user resets
                os.writeUTF("200 OK");
                System.out.println(active_user + " logged out.");
                active_user = "";
            }

        } catch (IOException ex) {
            System.out.println("Active user exited without using command.");
            active_user= "";
        }
        return active_user;
    }


    /* Store message: If the user is logged in, takes their input as a new motd. Prints an error if they are not logged in. */
    static String store_message(DataInputStream is, DataOutputStream os, String[] motd, int motd_count, String line, String active_user, Socket clientSocket) {
        // message store is needed and validation
        try {

            os = new DataOutputStream(clientSocket.getOutputStream());
            if(active_user.equals("")) {
                // fails, user is not logged in which is required
                os.writeUTF("401 You are not logged in, please log in first.");
                os.writeUTF("");
            }
            else {
                //success
                os.writeUTF("200 OK"); //success acknowledgement
                os.writeUTF("Please input new MOTD to be added.");
                line = is.readUTF(); //read input from the user
                os.writeUTF("200 OK");
                Writer wr = new FileWriter("src/motd.txt",true);
                BufferedWriter br = new BufferedWriter(wr);
                br.write(line);
                br.newLine();
                br.close();


            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return line;
    }
    /* Shutdown: If user calling this command is root user, shuts down the server by closing all sockets/streams and ending the application */
    static boolean shutdown(DataOutputStream os, boolean keep_going, String active_user, Socket clientSocket) {
        try {

            os = new DataOutputStream(clientSocket.getOutputStream());
            if(active_user.equals("root")){
                //success, checks if user is root user
                os.writeUTF("200 OK");
                server_continue = false;
                keep_going = false;
            }
            else {
                //user is not root, therefore program continues
                os.writeUTF("402 User not allowed to execute this command.");
                server_continue = true;
                keep_going = true;
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return server_continue;
    }
    /* Quit : Prints an acknowledgement message to the user before they close the socket */
    static void quit(DataOutputStream os, Socket clientSocket) {

        try { //sends message to user. when client receives this message it knows to shut down
            os = new DataOutputStream(clientSocket.getOutputStream());
            os.writeUTF("200 OK");
            os.writeUTF("Thank you for using the server.");
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



    public static void main(String args[]) throws IOException
    {
        String line;
        socket = new Socket();
        Scanner inFS; //file input
        try { //populating the array of default MOTDs
            activeUser="";
            inFS = new Scanner(new File("src/motd.txt"));
            while ((inFS.hasNextLine())) {
                line = inFS.nextLine();
                motd[motd_count] = line;
                motd_count++; //this is kept track of so we know when to loop back to the start
            }
            inFS.close();
            for(int i = 0; i < motd_count; i++) {
                System.out.println(motd[i]);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }



        // Try to open a server socket and client sockets
        try {
            Socket socket;
            ServerSocket server;
            server = new ServerSocket(SERVER_PORT);
            DataOutputStream os;
            while(server_continue) { //the job of the main thread is to sit and listen for new socket connections
                socket = server.accept();
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                InetAddress inetAddress = socket.getInetAddress();
                out.writeInt(0);
                new ChatHandler(socket).Socketrun();

            }

        }
        catch (IOException e) {
            System.out.println(e);
        }

    }


    static class ChatHandler {
        final private Socket clientSocket;
        boolean keep_going = true;
        int active_motd;
        DataInputStream is;
        DataOutputStream os;
        String line;
        InetAddress inetAddress;
        ChatHandler(Socket socket) {
            clientSocket = socket;
            socket = clientSocket;
            inetAddress = clientSocket.getInetAddress();
        }
        public void Socketrun() {
            try {
                is = new DataInputStream(clientSocket.getInputStream());
                os = new DataOutputStream(clientSocket.getOutputStream());
                System.out.println(clientSocket.getPort());

                //os = new DataOutputStream(clientSocket.getOutputStream());
                //os = new DataOutputStream(clientSocket.getOutputStream());
                os.writeUTF(inetAddress.getCanonicalHostName() + " has connected to the server.");


                System.out.println(inetAddress.getHostAddress()+ " has connected to the server.");
                while ((line = is.readUTF()) != null && keep_going == true)
                {
                    if(line.startsWith("LOGIN",0)) { //login cannot be included in the switch case because it needs to check only the first section of the string which is not possible in switch
                        activeUser = login(os, activeUser, line, clientSocket);
                    }
                    else if(line.startsWith("SEND",0)) {
                        send(os, is, line, activeUser, clientSocket); //SEND cannot be included in the switch case because it needs to check only the first section of the string
                    }
                    else {
                        switch(line) { //this switch-case checks what the user's input was in case it was a command, and if so, executes the proper command
                            case "MSGGET" :
                                active_motd = get_message(os, motd, active_motd, motd_count, clientSocket);
                                break;
                            case "LOGOUT" :
                                activeUser = logout(os, activeUser, clientSocket);// user gets logged out
                                break;
                            case "MSGSTORE" :
                                motd[motd_count] = store_message(is, os, motd, motd_count, line, activeUser, clientSocket); //the next empty element in array is filled with a string
                                motd_count++; //new value to find end of array
                                break;
                            case "SHUTDOWN" :
                                keep_going = shutdown( os, keep_going, activeUser, clientSocket);//if false the program will end
                                if (keep_going == false) {
                                    System.out.println("root user called SHUTDOWN.");
                                    for(int i = 0; i < num_clients; i++){
                                        //if(sockets[i] != clientSocket) {
                                        os = new DataOutputStream(socket.getOutputStream());
                                        os.writeUTF("Server shutting down...");
                                        os.flush();
                                        //}
                                        //else {
                                        //os = new DataOutputStream(sockets[i].getOutputStream());
                                        //os.writeUTF("");
                                        // }
                                    }
                                    System.exit(2);
                                }
                                break;
                            case "QUIT" :
                                quit(os, clientSocket);
                                break;
                            case "WHO" :
                                who(os, clientSocket);
                                break;
                            default : //we reach this case when the user doesn't input a command

                                    if (!activeUser.equals("")) { //if user is logged in, show name for stamp
                                        if(socket != clientSocket) {
                                            os = new DataOutputStream(socket.getOutputStream());
                                            os.writeUTF(activeUser + ": " + line);
                                        }
                                        else {
                                            os = new DataOutputStream(socket.getOutputStream());
                                            os.writeUTF("");
                                        }

                                    System.out.println(activeUser + ": " + line);
                                }
                                else{// if user is not logged in, show anonymous for stamp

                                        if(socket != clientSocket) {
                                            os = new DataOutputStream(socket.getOutputStream());
                                            os.writeUTF( line);
                                        }
                                        else {
                                            os = new DataOutputStream(socket.getOutputStream());
                                            os.writeUTF("You: " + line);
                                        }

                                    System.out.println(activeUser + ":"+ line);
                                }
                        }
                    }
                }
                //close input and output stream and socket. we only reach this if the user calls quit() successfully
                clientSocket.close();
            } catch (IOException ex) { //handling disconnections here ensures that we can clean up the user's info even if the disconnection was abrupt
                System.out.println("User disconnected.");

                    if(socket == clientSocket) {
                        if(!activeUser.equals("")) {
                            activeUser = logout(os, activeUser, clientSocket);// user gets logged out if they are logged in
                        }

                }
            }

        }

    }
}
