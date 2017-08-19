import ecs100.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JTextArea;

/**
 * Basic IRC Chat Client 
 */

public class ChatClient extends JFrame implements UIButtonListener, UITextFieldListener {
	private static final long serialVersionUID = 1L;
	private String server = "irc.ecs.vuw.ac.nz";  // default IRC server for testing.
    private static final int IRC_PORT = 6667;     // The standard IRC port number.

    /*# YOUR CODE HERE */
    private boolean isLoggedIn = false;     //boolean that prevents multiple log-ins
    private Socket  socket;
    private Scanner input;
    private PrintStream output;

    private String username;
    private String loginFeedback;

    //thread which listens to server messages
    public volatile Thread listernerThread = new Thread(new Runnable(){ 
                public void run(){
                    listenToServer();
                }
            });

    private String msg;
    private String recentChannel;

    private JTextArea serverText = new TextWindow().createNewFrame("Other Output From SERVER");

    //For Challenge
    HashMap<String, JTextArea>  map = new HashMap <String, JTextArea>(new Hashtable<String, JTextArea>());

    /**
     * main: construct a new ChatClient
     */
    public static void main(String[] args) throws IOException {
        new ChatClient();
    }

    /* 
     * Sets up the user interface.
     */
    public ChatClient (){ 
        UI.addButton("Connect", this);
        /*# YOUR CODE HERE */
        UI.addButton("Server's Message of the Day", this);
        UI.addButton("The Rules", this);
        UI.addButton("USERS", this);
        UI.addButton("List Channels", this);
        UI.addTextField("Channel Name To Join/Make", this);
        UI.addButton("Names of Most Recent Channel Joined", this);
        UI.addTextField("Channel to Send Message to ALL", this);
        UI.addTextField("User to Send Message To", this);
        UI.addTextField("Leave Channel", this);
        UI.addTextField("PING Server", this);
        UI.addTextField("Who Is", this);
        UI.addButton("INFO", this);
        UI.addTextField("Is On", this);
        UI.addButton("Network Map of Server", this);
        UI.addButton("Help", this);
        UI.addButton("What's the Time", this);
        UI.addButton("Log Out", this);
        UI.addButton("Quit", this);
    }

    /**
     * Respond to the buttons
     */
    public void buttonPerformed(String button){
        if (button.equals("Connect")) {
            this.connect();
        }
        else if (button.equals("Server's Message of the Day")) {
            this.MOTD();
        }
        else if (button.equals("The Rules")) {
            this.theRules();
        }
        else if (button.equals("USERS")) {
            this.usersInfo();
        }
        else if (button.equals("List Channels")) {
            this.listChannel();
        }
        else if (button.equals("Names of Most Recent Channel Joined")) {
            this.listNames();
        }
        else if (button.equals("INFO")) {
            this.info();
        }
        else if (button.equals("Help")) {
            this.help();
        }
        else if (button.equals("Network Map of Server")) {
            this.netMap();
        }
        else if (button.equals("What's the Time")) {
            this.time();
        }
        else if (button.equals("Log Out")) {
            this.closeConnection();
        }
        else if (button.equals("Quit")) {
            this.quitClient();
        }
    }

    public void textFieldPerformed(String name, String Text) {
        if (name.equals("Channel Name To Join/Make")) {
            this.joinChannel(Text);
        }
        else if (name.equals("Channel to Send Message to ALL")) {
            this.sendToAll(Text);
        }
        else if (name.equals("User to Send Message To")) {
            this.sendToOne(Text);
        }
        else if (name.equals("Is On")) {
            this.isOn(Text);
        }
        else if (name.equals("PING Server")) {
            this.pingServer(Text);
        }
        else if (name.equals("Who Is")) {
            this.whoIs(Text);
        }
        else if (name.equals("Leave Channel")) {
            this.leaveChannel(Text);
        }
    }

    /**
     * If there is currently an active socket, it should close the
     *  connection and set the socket to null.
     * Creates a socket connected to the server. 
     * Creates a Scanner on the input stream of the socket, 
     *  and a PrintStream on the output stream of the socket.
     * Logs in to the server (calling the loginToServer Message)
     * Once login is successful, starts a separate thread to
     *  listen to the server and process the messages from it.
     */
    public void connect(){
        try {
            if (isLoggedIn == false) {      //prevents double log-in
                if (socket != null) {       //if there is an active socket
                    socket.close();
                    socket = null;
                }
                socket = new Socket(server, IRC_PORT);
                input = new Scanner(socket.getInputStream());
                output = new PrintStream(socket.getOutputStream());

                if (login() == true) {      //if login successfull then start listener thread
                    listernerThread.start();
                }
            } else {
                UI.println("You are already logged in");
            }
        } catch (IOException e){System.out.println("connection failure"+e); }
    }

    /**
     * Attempt to log in to the Server and return true if successful, false if not.
     *  Ask user for username and real name
     *  Send info to server (NICK command and USER command)
     *  Read lines from server until get a message containing 004 (success) or
     *   a message containing 433 (failure - nickname in use)
     *  (For debugging, at least, print out all lines from the server)
     */
    private boolean login(){
        username = UI.askToken("Enter your usercode: ");
        String realname = UI.askString("Enter your real name: ");
        /*# YOUR CODE HERE */
        msg = "NICK " +username;
        send(msg);

        msg = "USER "+ username + " 0 * :" +realname;
        send(msg);

        while (input.hasNext()) {
            loginFeedback = input.next();
            if (loginFeedback.equals("004")) {      //if no duplicate nickname
                UI.println ("Login successfull");
                isLoggedIn = true;
                return true;
            } else if (loginFeedback.equals("433")) {
                UI.println ("Nickname in use");
                return false;
            }
        }
        return false;
    }

    /**
     * Send a message to the current server:
     *  - check that the socket and the serverOut are not null
     *  - print the message with a \r\n at the end to serverOut
     *  - flush serverOut (to ensure the message is sent)
     */
    private void send(String msg){
        /*# YOUR CODE HERE */
        if (socket != null && output != null) {
            output.print(msg+" \r\n");
            output.flush();
        } else {
            UI.println("Connetion Failure");
        }
    }

    /**
     * Method run in the the thread that is listening to the server.
     * Loop as long as there is anything in the serverIn scanner:
     *   Get and process the next line of input from the scanner
     *   Simple version: 
     *    prints the line out for the user
     *    Checks if the line contains "SQUIT",
     *       if so, close the socket, set serverIn and serverOut set the quit the program.
     *      if the line contains "PING", send a PONG message back
     *        (must be identical to the line, but with "PING" replaced by "PONG")
     *   Better version parses the line into source, command, params, finalParam
     *    (where the source and finalParam are optional, and params may be empty)
     *    Then deals with the message appropriately.
     */
    private void listenToServer() {
        while (input.hasNext()) {

            String serverIn = input.nextLine();     //remembers every line that the server sends

            String [] inspectToken = serverIn.split(" ");   //remembers every token in an array seperated by space   

            String source = "";     //source may be optional
            String command = inspectToken[0];

            if (command.startsWith(":") ){  //source may be omitited
                source = inspectToken[0];
                command = inspectToken[1];
            }

            if (command.equals("SQUIT")) {  //server had quit
                closeConnection();
            } 
            else if (command.equals("PING")) {  //server pingged me
                msg = "PONG ";
                for (int token = 1; token < inspectToken.length; token++) {
                    msg = msg+inspectToken[token]+" ";
                }
                send(msg);      //auto reply
            } 
            else if (command.equals("JOIN")) {  
                char [] inspectChar = source.toCharArray();     //put source to character array
                serverText.append("\n");
                //parses nickname of source/sender
                for (int letter = 1; letter < inspectChar.length; letter++) {
                    if ((Character.toString(inspectChar[letter]).equals("!")) && (Character.toString(inspectChar[letter+1]).equals("~"))) {
                        break;
                    } else {
                        String letterString = Character.toString(inspectChar[letter]);
                        serverText.append(letterString);
                    }
                }

                serverText.append(" joined the "+inspectToken[2]+" channel \n");
            } 
            else if (command.equals("PRIVMSG")) {
                if (inspectToken[2].startsWith("#")) {  //if it's a message to a channel
                    //ensures that the ONLY messages I get is from the channel that I joined
                    //e.g. If Michael sends PRIVMSG to #irc channel, SERVER would say "Michael!~123.121.2 JOIN #irc"
                    //but I NEVER joined it so program should ignore this
                    if (map.containsKey(inspectToken[2])) {
                        JTextArea textBox = map.get(inspectToken[2]);
                        char [] inspectChar = source.toCharArray();     //put source to character array
                        //parses nickname of source/sender
                        for (int letter = 1; letter < inspectChar.length; letter++) {
                            if ((Character.toString(inspectChar[letter]).equals("!")) && (Character.toString(inspectChar[letter+1]).equals("~"))) {
                                break;
                            } else {
                                String letterString = Character.toString(inspectChar[letter]);
                                textBox.append(letterString);
                            }
                        }
                        textBox.append(" ");

                        int token = 3;
                        if (source == "") {     //if source is omitted
                            token = 2;
                        }
                        while (token < inspectToken.length) {   //shows everything after :
                            textBox.append(inspectToken[token]+" ");
                            token++;
                        }

                        textBox.append(" \n");
                    }
                } else if (inspectToken[2].equals(username)) {  //if someone PRIVMSGs me
                    char [] inspectChar = source.toCharArray();     //put source to character array
                    String nameString="";
                    //parses nickname of source/sender
                    for (int letter = 1; letter < inspectChar.length; letter++) {
                        if ((Character.toString(inspectChar[letter]).equals("!")) && (Character.toString(inspectChar[letter+1]).equals("~"))) {
                            break;
                        } else {
                            nameString = nameString+Character.toString(inspectChar[letter]);
                        }
                    }

                    if (!map.containsKey(nameString)) { //if someone PRIVMSGs me before I PRIVMSG them, pop up new window
                        map.put(nameString, new TextWindow().createNewFrame("Private Messages from "+nameString));
                    }
                    JTextArea textBox = map.get(nameString);

                    textBox.append(nameString+ ": ");

                    int token = 3;
                    if (source == "") {     //if source is omitted
                        token = 2;
                    }
                    while (token < inspectToken.length) {   //shows everything after :
                        textBox.append(inspectToken[token]+" ");
                        token++;
                    }

                    textBox.append(" \n");
                }
            } 
            else if (command.equals("322")) {   //list command reply
                if (source == "") {     //if source is omitted
                    serverText.append(inspectToken[2]+ " with " +inspectToken[3]+ " people in it \n");
                } else {
                    serverText.append(inspectToken[3]+ " with " +inspectToken[4]+ " people in it \n");
                }
            } 
            else if (command.equals("353")) {   //names command reply
                int token = 6;
                if (source == "") {     //if source is omitted
                    token = 5;
                }
                while (token < inspectToken.length) {   //shows everything after :
                    serverText.append (inspectToken[token]+ " \n");
                    token++;
                }
            }
            else if (command.equals("303")) {   //ISON command reply
                serverText.append("SERVER ");

                if (inspectToken.length==1) {   //if char array consist only of : --> meaning 1 charcter only
                    serverText.append("None of the usernames specified are online \n");
                } else {
                    int token = 3;
                    if (source == "") {     //if source is omitted
                        token = 2;
                    }
                    while (token < inspectToken.length) {
                        serverText.append(inspectToken[token]+" ");
                        token++;
                    }
                    serverText.append(" is/are online\n");
                }
                serverText.append(" \n");
            } 
            else if (command.equals("PART")) {
                char [] inspectChar = source.toCharArray();     //put source to character array
                serverText.append("\n");
                String nameString="";
                //parses nickname of source/sender
                for (int letter = 1; letter < inspectChar.length; letter++) {
                    if ((Character.toString(inspectChar[letter]).equals("!")) && (Character.toString(inspectChar[letter+1]).equals("~"))) {
                        break;
                    } else {
                        nameString = nameString+Character.toString(inspectChar[letter]);
                    }
                }
                serverText.append(nameString);
                serverText.append(" left the "+inspectToken[2]+" channel \n");

                int token = 2;
                if (source == "") {     //if source is omitted
                    token = 1;
                }

                if (map.containsKey(nameString)) {  //if someone you have message or has messaged you AND then left the channel
                    JTextArea textBox = map.get(inspectToken[token]);
                    textBox.append(nameString+" has LEFT the "+inspectToken[token]+" channel. THIS WINDOW IS INACTIVE. PLEASE CLOSE IT \n");
                    map.remove(nameString);     //remove from hashmap
                }
                //if YOU leave a channel
                if (nameString == username && map.containsKey(inspectToken[token])) {
                    JTextArea textBox = map.get(inspectToken[token]);
                    textBox.append("You have LEFT the "+inspectToken[token]+" channel. THIS WINDOW IS INACTIVE. PLEASE CLOSE IT \n");
                    map.remove(inspectToken[token]);    //remove from hashmap
                }
            } 
            else if (command.equals("PONG")) {      //when you PING server, it will output this
                serverText.append("SERVER: "+serverIn+"\n");
            } 
            else if (command.equals("QUIT")) {  //quit command reply
                char [] inspectChar = source.toCharArray();      //put source to character array
                String nameString="";
                //parses nickname of source/sender
                for (int letter = 1; letter < inspectChar.length; letter++) {
                    if ((Character.toString(inspectChar[letter]).equals("!")) && (Character.toString(inspectChar[letter+1]).equals("~"))) {
                        break;
                    } else {
                        nameString = nameString+Character.toString(inspectChar[letter]);
                    }
                }
                serverText.append(nameString);
                serverText.append(" had quit \n");

                if (map.containsKey(nameString)) {  //if someone you have message or has messaged you AND then they quit
                    JTextArea textBox = map.get(nameString);
                    textBox.append(nameString+" had QUIT. THIS WINDOW IS INACTIVE. PLEASE CLOSE IT \n");
                    map.remove(nameString); //remove from hashmap
                }

            } 
            else if ( (command.equals("251")) || (command.equals("255")) ||     //USERS command reply 
            (command.equals("265")) || (command.equals("266")) ||   //number of local and global users
            (command.equals("250")) ||  //server statistics
            (command.equals("371")) ||  //INFO command reply
            (command.equals("263")) ||     //SERVER overload
            (command.equals("015"))) {      //MAP command reply
                char [] inspectChar = inspectToken[3].toCharArray();
                for (int letter = 1; letter < inspectChar.length; letter++) {
                    String letterString = Character.toString(inspectChar[letter]);
                    serverText.append(letterString);
                }

                for (int token = 4; token < inspectToken.length; token++) {
                    serverText.append(" "+inspectToken[token]);
                }
                serverText.append("\n");
            }

            //displays ERROR messages to SERVER window
            try {   //if possible...
                int numberCmd = Integer.parseInt(command);  //convert command to integer
                if (numberCmd >=401) {      //if it is indeed an error
                    serverText.append("ERROR "+numberCmd+" ");

                    int token = 3;
                    if (source == "") {     //if source is omitted
                        token = 2;
                    }

                    //show the extended argument of it in the SERVER window
                    while (token < inspectToken.length) {
                        serverText.append(inspectToken[token]+" ");
                        token++;
                    }
                    serverText.append("\n");
                }
            } catch (NumberFormatException e) {}
        } 
    }

    /**
     * Close the connection:
     *  - close the socket,
     *  - set the serverIn and serverOut to null
     *  - print a message to the user
     */
    public void closeConnection(){
        try {
            if (socket != null){
                msg="QUIT :Bye bye for now";    // sends QUIT
                send(msg);
                socket.close();     //closes socket
                input = null;       //set values to null
                output = null;
                socket = null;

                listernerThread.interrupt();    //stop thread
                listernerThread = null;

                isLoggedIn = false;             //set login boolean to false
                UI.println("You have successfully logged out");
            }
        } catch (IOException e){System.out.println("connection failure"+e); }
    }

    // Other methods which sends most of the commands
    public void listChannel() {
        serverText.append(" \n");
        serverText.append("Here are the list of Channels \n");
        msg="LIST";
        send(msg);
    }

    public void listNames() {
        if (recentChannel != null) {
            serverText.append(" \n");
            serverText.append("Here are the list of names of other people on the "+recentChannel+" Channel \n");
            msg="NAMES "+recentChannel;
            send(msg);
        } 
        else if (recentChannel == null) {
            serverText.append("No selected channel");
        }
    }

    public void joinChannel(String channel) {
        boolean validName = true;
        for(int i = 0; i < channel.length(); i++){
            if(Character.isWhitespace(channel.charAt(i))){
                validName = false;
            }
        }

        if (((channel.startsWith("#")) || (channel.startsWith("&")) && validName==true)){
            if (channel != null) {
                serverText.append(" \n");
                serverText.append("Here are the list of names of other people on the "+channel+" Channel \n");
                msg="JOIN "+channel;
                send(msg);
                recentChannel = channel;

                //create new window and put channel as key in hashmap
                map.put(channel, new TextWindow().createNewFrame("Messages to ALL on "+channel));
            } 
        }
    }

    public void sendToAll(String channel) {
        boolean validName = true;
        for(int i = 0; i < channel.length(); i++){
            if(Character.isWhitespace(channel.charAt(i))){
                validName = false;
            }
        }

        if (((channel.startsWith("#")) || (channel.startsWith("&")) && validName==true)){
            if (channel != null) {
                UI.println("");
                UI.println("Enter message to all");

                UI.print(">>> ");
                String textToSend = UI.nextLine();     // read a line from the user
                msg="PRIVMSG "+channel+ " :"+textToSend;
                if (map.containsKey(channel)) {
                    JTextArea textBox = map.get(channel);

                    textBox.append(username+": "+textToSend);

                    textBox.append(" \n");
                }
                send(msg);
            } 
        }
    }

    public void sendToOne(String User) {
        boolean validName = true;
        for(int i = 0; i < User.length(); i++){
            if(Character.isWhitespace(User.charAt(i))){
                validName = false;
            }
        }

        if (User != null && validName==true) {
            UI.println("");
            UI.println("Enter message to "+User);

            UI.print(">>> ");
            String textToSend = UI.nextLine();     // read a line from the user
            msg="PRIVMSG "+User+ " :"+textToSend;

            //if not in hashmap, create new window and put the nickname as key in hashmap
            if (!map.containsKey(User)) {
                map.put(User, new TextWindow().createNewFrame("Private Messages from "+User));
            }
            JTextArea textBox = map.get(User);

            textBox.append(username+": "+textToSend);
            textBox.append(" \n");
            send(msg);
        } 
    }

    public void leaveChannel(String channel) {
        if (channel != null) {
            serverText.append(" \n");
            msg="PART "+channel;
            send(msg);
        }
    }

    public void pingServer(String text) {
        msg="PING "+text;
        send(msg);
    }

    public void quitClient() {
        closeConnection();
        UI.quit();
    }

    public void info() {
        msg="INFO ";
        send(msg);
    }

    public void whoIs (String user) {
        msg="WHOIS "+user;
        send(msg);
    }

    public void help() {
        msg="HELP ";
        send(msg);
    }

    public void isOn(String user) {
        msg="ISON "+user;
        send(msg);
    }

    public void MOTD() {
        msg="MOTD ";
        send(msg);
    }

    public void usersInfo() {
        msg="USERS ";
        send(msg);
    }

    public void theRules() {
        msg="RULES";
        send(msg);
    }

    public void netMap() {
        msg="MAP";
        send(msg);
    }

    public void time() {
        msg="TIME";
        send(msg);
    }
}