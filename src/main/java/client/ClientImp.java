package client;

import client.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static java.lang.String.format;

@Slf4j
@RequiredArgsConstructor
public class ClientImp implements Client {
    private final String host;
    private final int port;

    private User user;
    private SocketChannel socketChannel;

    @Override
    public boolean accept() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
        } catch (IOException e) {
            log.error("Ошибка при подключении к серверу: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public User auth(Scanner scanner) {
        if (user == null) {
            log.info("Выберите имя пользователя...");
            String msg = scanner.nextLine().trim();
            user = new User(msg);
            log.info("Вы выбрали имя " + user.getName());
        }
        return user;
    }

    @Override
    public String read(ByteBuffer buffer) {
        int bytesCount = 0;
        try {
            bytesCount = socketChannel.read(buffer);
            if (bytesCount == -1) {
                socketChannel.close();
            }
        } catch (IOException e) {
            log.error("Не удалось прочитать сообщение, по причине: " + e.getMessage());
            return null;
        }

        String message = new String(buffer.array(), 0, bytesCount, StandardCharsets.UTF_8).trim();
        log.info("Получено сообщение от " + message);
        buffer.clear();

        return message;
    }

    @Override
    public boolean send(String message, ByteBuffer buffer) {
        if (isExitChat(message)) {
            close();
        }

        String messageUser = user.getName() + ": " + message;
        try {
            socketChannel.write(
                    ByteBuffer.wrap(
                            messageUser.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.error("Ошибка при отправке сообщения: " + e.getMessage());
            return false;
        }
        log.info("Отправлено сообщение от " + messageUser);
        buffer.clear();
        return true;
    }

    @Override
    public boolean isConnected() {
        return socketChannel.isConnected();
    }

    @Override
    public void close() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            log.error("Не удалось закрыть соединение по причине: " + e.getMessage());
        }
    }

    /**
     * Данный метод создает отдельный поток на чтение сообщений от сервера,
     * а также отдельный поток на отправку сообщений.
     */
    public void start() {
        if (!accept()) {
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            ByteBuffer buffer = ByteBuffer.allocate(2 << 10);
            auth(scanner);
            new Thread(() -> readConnect(buffer)).start();
            while (isConnected()) {
                System.out.println("Введите сообщение...");
                String message = scanner.nextLine().trim() + "\r\n";
                if (isExitChat(message)) {
                    close();
                    return;
                }
                send(message, buffer);
            }
        }
    }

    private void readConnect(ByteBuffer buffer) {
        while (isConnected()) {
            read(buffer);
        }
    }

    private boolean isExitChat(String message) {
        String exitText = message.replace("\r\n", "");
        if ("/exit".equals(exitText)) {
            log.info(format("Пользователь %s вышел из чата", user.getName()));
            return true;
        }
        return false;
    }
}
