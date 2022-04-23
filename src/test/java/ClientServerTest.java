import client.Client;
import client.ClientImp;
import client.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @implNote Это интеграционный тест клиента и сервера, мне показалось бессмысленным писать тесты отдельно
 * так как, по сути, проверяется одно и тоже.
 */
class ClientServerTest {
    private final static String HOST = "localhost";
    private final static int PORT = 8088;

    private ClientImp sut;
    private Server server;
    private Thread serverThread;
    private ByteBuffer buffer;

    @BeforeEach
    void init() throws InterruptedException {
        sut = new ClientImp(HOST, PORT);
        server = new Server(HOST, PORT);
        buffer = ByteBuffer.allocate(2 << 10);

        serverThread = new Thread(() -> server.start());
        serverThread.start();
        Thread.sleep(500); // добавлено, чтобы успеть переподключиться XD
    }

    @Test
    void accept() {
        boolean expected = true;

        boolean actual = sut.accept();

        assertEquals(expected, actual);
    }

    @Test
    void accept_IllegalArgumentException() {
        Client problemClient = new ClientImp(null, 0);

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            problemClient.accept();
        });

        assertNotNull(thrown.getMessage());
    }

    @Test
    void auth() {
        ByteArrayInputStream in = new ByteArrayInputStream("test".getBytes());
        System.setIn(in);
        String expected = "test";

        User actual = sut.auth(new Scanner(System.in));

        assertEquals(expected, actual.getName());
    }

    @Test
    void read(){
        String expected = "Пользователь тест: привет";
        sut.accept();
        setUser(sut);
        ClientImp otherClient = new ClientImp(HOST, PORT);
        otherClient.accept();
        setUser(otherClient);
        otherClient.send("привет", ByteBuffer.allocate(2 << 10));

        String actual = sut.read(ByteBuffer.allocate(2 << 10));

        assertEquals(expected, actual);
    }

    @Test
    void send() {
        boolean expected = true;
        sut.accept();
        setUser(sut);

        boolean actual = sut.send("Test", buffer);

        assertEquals(expected, actual);
    }

    @Test
    void close() {
        boolean expected = false;
        sut.accept();
        setUser(sut);
        sut.send("/exit", buffer);

        boolean actual = sut.isConnected();

        assertEquals(expected, actual);
    }

    @AfterEach
    void finish() {
        serverThread.interrupt();
    }

    /**
     * @implNote Данный костыль добавлен, чтобы заполнить поле user клиента,
     * иначе будет ошибка при формировании логов. К сожалению, замокать не получится,
     * т.к поле привратное. Добавить его в конструктор или добавить сеттер тоже не вариант,
     * тогда получиться, что ради тестов переписана логика программы. Может это и архитектурная ошибка,
     * Бог мне судья.
     */
    private void setUser(Client client) {
        ByteArrayInputStream in = new ByteArrayInputStream("Пользователь тест".getBytes());
        System.setIn(in);

        client.auth(new Scanner(System.in));
    }
}