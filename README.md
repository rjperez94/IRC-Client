# IRC-Client

## Compiling Java files using Eclipse IDE

1. Download this repository as ZIP
2. Create new `Java Project` in `Eclipse`
3. Right click on your `Java Project` --> `Import`
4. Choose `General` --> `Archive File`
5. Put directory where you downloaded ZIP in `From archive file`
6. Put `ProjectName/src` in `Into folder`
7. Click `Finish`

### Linking the UI Library

8. Right click on your `Java Project` --> `Build Path` --> `Add External Archives`
9. Select `ecs100.jar` and link it to the project. That JAR will be in the directory where you downloaded ZIP

## Running the program

1. Right click on your `Java Project` --> `Run As` --> `Java Application` --> `ChatClient`
2. This opens two windows: one for incoming server messages and one to send messages to server
3. Adjust hostname and port number as required

## Miscellaneous

Connect (to host at port)
Logout (from server)
Quit - logout froms server and exit program

## Features

**This is a client ONLY**

Message of the Day - see MOTD
Rules - see server's rules
Users - current users in server
List Channels - current chanels in server
Channel Name to Join/Make - join channel with `name`. (make it if there is none)
Name of Recent Channel Joined - current or latest channel joined
Channel to Send meassage to All - sends a message to all users in that channel. (type channel name)
User to Send Message To - sends a message to individual. (type user login)
Who Is - send `WHOIS` command with user login as argument
Info - get information about the server
Is On - see if user is logged in
Network Map of Server - send `MAP` command to server
Help - display server's help text
What's the Time - send `TIME` command to server
