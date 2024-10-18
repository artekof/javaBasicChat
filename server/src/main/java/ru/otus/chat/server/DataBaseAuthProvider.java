package ru.otus.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataBaseAuthProvider implements AuthenticatedProvider{
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres";
    private Server server;
    private Connection connection;


    public DataBaseAuthProvider(Server server) throws SQLException {
        this.server = server;
        this.connection = DriverManager.getConnection(DATABASE_URL, "postgres", "1234567890");
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: DataBase режим");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
            String usernameQuery = "select username from users where login = ? and password = ?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(usernameQuery)){
            preparedStatement.setString(1,login);
            preparedStatement.setString(2,password);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                while (resultSet.next()){
                    return resultSet.getString("username");
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authName = getUsernameByLoginAndPassword(login, password);
        if (authName == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authName)) {
            clientHandler.sendMessage("Учетная запись уже занята");
            return false;
        }

        clientHandler.setUsername(authName);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authName);
        return true;
    }

    private boolean isLoginAlreadyExist(String login) {
        String loginAlreadyExistQuery = "select login from users where login = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(loginAlreadyExistQuery)){
            preparedStatement.setString(1,login);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                while (resultSet.next()){
                    return resultSet.getString(1) != login;
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        String usernameAlreadyExist = "select username from users where username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(usernameAlreadyExist)){
            preparedStatement.setString(1,username);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                while (resultSet.next()){
                    return resultSet.getString(1) != username;
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username, String role) {
        if (login.trim().length() < 3 || password.trim().length() < 5
                || username.trim().length() < 2)
        {
            clientHandler.sendMessage("Требования не выполнены: \n1) Логин должен содержать от 3х символов;\n2) Пароль должен содержать от 5ти символов;" +
                    "\n3) Имя пользователя должно содержать от 2х символов;\n4) Необходимо задать роль 'user' или 'admin'.");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }

        String regNewUserQuery = "insert into users(login,username,password,role) values(?,?,?,?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(regNewUserQuery)){
            preparedStatement.setString(1,login);
            preparedStatement.setString(2,username);
            preparedStatement.setString(3,password);
            preparedStatement.setString(4,role);
            preparedStatement.executeUpdate();
            }
        catch (SQLException e){
            e.printStackTrace();
            System.err.println("Ошибка при регистрации");
            clientHandler.sendMessage("Ошибка при регистрации");
        }
        clientHandler.setUsername(username);
        clientHandler.setRole(role);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);

        return true;
    }
}
