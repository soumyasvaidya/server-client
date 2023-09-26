/*
 * Server.java
 */

//import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;

public class Server {
    public static final int SERVER_PORT = 9591;
    static public int messageCount;
    static Socket socket;
    static String[] messageOfTheDay = new String[20];
    static boolean serverContinue = true;

    static String activeUser= new String();

    /* To send message in a cyclic message of the day to the client .
    Message can be sent even if the user is not logged in*/
    static int sendMessageOfTheDay(DataOutputStream outputStream, String[] message, int currentMessage, int messageCount, Socket client) {
        try {

            outputStream = new DataOutputStream(client.getOutputStream());
            outputStream.writeUTF("200 OK");
            outputStream.writeUTF(message[currentMessage]);
            currentMessage++;
            if( currentMessage == messageCount)
                currentMessage = 0;
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return currentMessage;
    }


    /* Login : Logs in the user by checking to see if their credentials match any users in the file usersDir.txt and will print an error if they are already logged in */
    static String loginUser(DataOutputStream outputStream, String activeUser, String loginArgs, Socket clientSocket) {
        try {
            Scanner inputScanner;
            String  storedUser;
            String[] inputLoginPassword, loginPasswordRecord;

            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            if(!activeUser.equals("")) {
                outputStream.writeUTF("You are already logged in as " + activeUser);
            }
            else {
                try {
                    inputScanner = new Scanner(new File("usersDir.txt")); // this will check the usernames stored in the server
                    inputLoginPassword = loginArgs.split(" ");
                    if (inputLoginPassword.length != 3) {
                        try {
                            //Returning error If Username and password are not sent
                            outputStream.writeUTF("410 Wrong UserID or password");
                        } catch (IOException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println(" Wrong UserID or Password ");
                    }
                    else {
                        /*Checking users file for the login details*/
                        String tempPassword;
                        tempPassword = inputLoginPassword[1];
                        inputLoginPassword[1] = inputLoginPassword[2];
                        inputLoginPassword[0] = tempPassword;

                        while(inputScanner.hasNextLine()){
                            storedUser = inputScanner.nextLine();
                            loginPasswordRecord = storedUser.split(" ");
                            if((inputLoginPassword[0].equals(loginPasswordRecord[0])) && (inputLoginPassword[1].equals(loginPasswordRecord[1]))) {
                                int i = 0;
                                while(storedUser.charAt(i) != ' ') {
                                    activeUser += storedUser.charAt(i);
                                    i++;
                                }
                            }
                        }
                        inputScanner.close();
                        if(!activeUser.equals("")) {
                            try {
                                outputStream.writeUTF("200 OK");
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println("Logged in successfull, User: " + activeUser);
                        }
                        else {
                            try {
                                outputStream.writeUTF("410 Wrong UserID or password");
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println("Login failed for the user");
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return activeUser;
    }

    /* To logout user from the server.
    Throws error if trying to logout without login */
    static String userLogout(DataOutputStream dataOutputStream, String currentUser, Socket client) {

        try {
            dataOutputStream = new DataOutputStream(client.getOutputStream());
            if (currentUser.equals(""))
                dataOutputStream.writeUTF("405 You are not logged in.");
            else {
                dataOutputStream.writeUTF("200 OK");
                System.out.println("user "+currentUser + " logged out.");
                currentUser = "";
            }

        } catch (IOException ex) {
            System.out.println("User logged out due to Exception" + ex.getLocalizedMessage());
            currentUser= "";
        }
        return currentUser;
    }


    /* Saves message provided by users */
    static String saveUserMessage(DataInputStream dataInputStream, DataOutputStream dataOutputStream, String[] messageOfTheDay, int messageCount, String message, String currentUser, Socket client) {
        try {
            dataOutputStream = new DataOutputStream(client.getOutputStream());
            if(currentUser.equals("")) {
                dataOutputStream.writeUTF("401 You are not logged in, please log in first.");
                //dataOutputStream.writeUTF("");
            }
            else {
                dataOutputStream.writeUTF("200 OK");
                dataOutputStream.writeUTF("Please input new Message of the day to be added.");
                message = dataInputStream.readUTF();
                if(messageCount>=20){
                    dataOutputStream.writeUTF("429 Limit exceed for storing message: maxLimit=20 messages");
                }
                else {
                    dataOutputStream.writeUTF("200 OK");
                    Writer wr = new FileWriter("messageDir.txt", true);
                    BufferedWriter br = new BufferedWriter(wr);
                    br.write(message);
                    br.newLine();
                    br.close();
                }


            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return message;
    }


    /* Shutdown server allowed only for root user.
     * Throws error if non root user tries to shutdown */
    static boolean shutdown(DataOutputStream dataOutputStream, boolean proceed, String currentUser, Socket client) {
        try {

            dataOutputStream = new DataOutputStream(client.getOutputStream());
            if(currentUser.equals("root")){
                dataOutputStream.writeUTF("200 OK");
                serverContinue = false;
                //proceed = false;
            }
            else {
                dataOutputStream.writeUTF("402 User not allowed to execute this command.");
                serverContinue = true;
                //proceed = true;
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return serverContinue;
    }
    /* Quit : Prints an acknowledgement message to the user before they close the socket */
    static void quitClient(DataOutputStream dataOutputStream, Socket client) {

        try {
            dataOutputStream = new DataOutputStream(client.getOutputStream());
            dataOutputStream.writeUTF("200 OK");
            dataOutputStream.writeUTF("Thank you for using the server.");

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public static void main(String args[]) throws IOException
    {
        String message;
        socket = new Socket();
        Scanner scanner;
        try {
            activeUser="";
            scanner = new Scanner(new File("messageDir.txt"));
            while ((scanner.hasNextLine())) {
                message = scanner.nextLine();
                messageOfTheDay[messageCount] = message;
                messageCount++; //this is kept track of so we know when to loop back to the start
            }
            scanner.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }


        try {
            Socket socketClient;
            ServerSocket serverSocket;
            serverSocket = new ServerSocket(SERVER_PORT);
            DataOutputStream os;
            while(serverContinue) {
                socketClient = serverSocket.accept();
                DataOutputStream out = new DataOutputStream(socketClient.getOutputStream());
                InetAddress inetAddress = socketClient.getInetAddress();
                out.writeInt(0);
                new clientHandler(socketClient).Socketrun();

            }

        }
        catch (IOException e) {
            System.out.println("Exception: "+e.getLocalizedMessage());
        }

    }


    static class clientHandler {
        final private Socket client;
        boolean proceed = true;
        int activeMessageCount;
        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;
        String message;
        InetAddress inetAddress;
        clientHandler(Socket socket) {
            client = socket;
            socket = client;
            inetAddress = client.getInetAddress();
        }
        public void Socketrun() {
            try {
                dataInputStream = new DataInputStream(client.getInputStream());
                dataOutputStream = new DataOutputStream(client.getOutputStream());
                System.out.println("client port"+ client.getPort());

                dataOutputStream.writeUTF("Connected to Server.");

                List<String> inputValidation= Arrays.asList("MSGGET","MSGSTORE","QUIT","SHUTDOWN","LOGOUT");

                System.out.println(inetAddress.getHostAddress()+ "  connected ");
                while ((message = dataInputStream.readUTF()) != null && proceed == true)
                {

                    if(message.startsWith("LOGIN",0)) {
                        activeUser = loginUser(dataOutputStream, activeUser, message, client);
                    }
                    else {
                        if(!inputValidation.contains(message)){
                            System.out.println("INVALID COMMAND PROVIDED"); //validating if any invalid command are provided
                        }
                        switch(message) {
                            case "MSGGET" :
                                activeMessageCount = sendMessageOfTheDay(dataOutputStream, messageOfTheDay, activeMessageCount, messageCount, client);
                                break;
                            case "LOGOUT" :
                                activeUser = userLogout(dataOutputStream, activeUser, client);
                                break;
                            case "MSGSTORE" :
                                String msg= saveUserMessage(dataInputStream, dataOutputStream, messageOfTheDay, messageCount, message, activeUser, client); //the next empty element in array is filled with a string
                                if(msg!=null && !msg.equals(" ") && !msg.equals("")) {
                                    messageOfTheDay[messageCount] = msg;
                                    messageCount++;
                                }
                                break;
                            case "SHUTDOWN" :
                                proceed = shutdown(dataOutputStream, proceed, activeUser, client);
                                if (proceed == false) {
                                    System.out.println("root user called SHUTDOWN.");
                                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                    dataOutputStream.writeUTF("Server shutting down...");
                                    dataOutputStream.flush();
                                    System.exit(2); //using exit 2 code for SHUTDOWN command
                                }
                                break;
                            case "QUIT" :
                                quitClient(dataOutputStream, client);
                                activeUser=""; //user will get logged out on quit
                                break;
                            default :

                                if (!activeUser.equals("")) {
                                    if(socket != client) {
                                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                        dataOutputStream.writeUTF(activeUser + ": " + message);
                                    }
                                    else {
                                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                        dataOutputStream.writeUTF("");
                                    }

                                    System.out.println(activeUser + ": " + message);
                                }
                                else{

                                    if(socket != client) {
                                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                        dataOutputStream.writeUTF(message);
                                    }
                                    else {
                                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                        dataOutputStream.writeUTF("You: " + message);
                                    }

                                    System.out.println(activeUser + ":"+ message);
                                }
                        }
                    }
                }
                //close input and output stream and socket. we only reach this if the user calls quit() successfully
                client.close();
            } catch (IOException ex) { //handling disconnections here ensures that we can clean up the user's info even if the disconnection was abrupt
                System.out.println("User disconnected.");

                if(socket == client) {
                    if(!activeUser.equals("")) {
                        activeUser = userLogout(dataOutputStream, activeUser, client);// user gets logged out if they are logged in
                    }

                }
            }

        }

    }
}
