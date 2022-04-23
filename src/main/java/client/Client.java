package client;

import client.model.User;

import java.nio.ByteBuffer;
import java.util.Scanner;

public interface Client {
    User auth(Scanner scanner);

    String read(ByteBuffer buffer);

    boolean send(String message, ByteBuffer buffer);

    boolean isConnected();

    boolean accept();

    void close();
}
