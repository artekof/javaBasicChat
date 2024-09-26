package ru.otus.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private static int userCount = 0;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        userCount++;
        username = "user" + userCount;
        new Thread(() -> {
            try {
                System.out.println(username + " подключился ");
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exit")){
                            sendMessage("/exitok");
                            System.out.println(username + " отключился");
                            break;
                        }
                        //Реализуйте возможность отправки личных сообщений: если клиент пишет «/w tom Hello»,
                        // то сообщение Hello должно быть отправлено только клиенту с ником tom

                        if (message.startsWith("/w")){
                            String[] wordsInMessage = message.split(" ", 3);
                            if(wordsInMessage.length == 3){
                                String user = wordsInMessage[1];
                                String userMessage = wordsInMessage[2];
                                server.sendMessageClient(userMessage,user);
                            }
                            else {
                                sendMessage(message);
                            }
                        }

                    } else {
                        server.broadcastMessage(username + " : " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(){
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
