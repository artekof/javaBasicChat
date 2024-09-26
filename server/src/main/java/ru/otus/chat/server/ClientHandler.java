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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                System.out.println("Клиент подключился");
                //цикл аутентификации
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exit ")) {
                            sendMessage("/exitok");
                            break;
                        }
                        // /auth login password
                        if (message.startsWith("/auth ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 3) {
                                sendMessage("Неверный формат команды /auth");
                                continue;
                            }

                            if (server.getAuthenticatedProvider()
                                    .authenticate(this,elements[1], elements[2])){
                                break;
                            }
                            continue;
                        }
                        // /reg login password username
                        if (message.startsWith("/reg ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 4) {
                                sendMessage("Неверный формат команды /reg");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .registration(this,elements[1], elements[2], elements[3])){
                                break;
                            }
                            continue;
                        }
                    }
                    sendMessage("Перед работой необходимо пройти аутентификацию командой " +
                            "/auth login password или регистрацию командой /reg login password username");
                }
                System.out.println("Клиент "+ username+ " успешно прошел аутентификацию");

                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exit ")){
                            sendMessage("/exitok");
                            System.out.println(username + " отключился");
                            break;
                        }
                        if (message.startsWith("/w ")){
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
