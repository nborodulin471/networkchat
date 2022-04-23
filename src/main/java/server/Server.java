package server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Server {
    private static final Map<SocketChannel, ByteBuffer> sockets = new ConcurrentHashMap<>();
    private final String host;
    private final int port;

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private static void accept(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        log.info("Выполнено соединение " + socketChannel.getRemoteAddress());
        sockets.put(socketChannel, ByteBuffer.allocate(2 << 10));
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private static void write(SelectionKey key, Selector selector) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = sockets.get(socketChannel);

        buffer.flip();
        String clientMessage = new String(buffer.array(), buffer.position(), buffer.limit());
        String response = clientMessage.replace("\r\n", "");

        buffer.clear();
        buffer.put(ByteBuffer.wrap(response.getBytes()));
        buffer.flip();

        socketChannel.write(buffer);
        if (!buffer.hasRemaining()) {
            buffer.compact();
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
    }

    private static void read(SelectionKey key, Selector selector) throws IOException {
        ByteBuffer newMessage = null;
        SocketChannel currentSocketChannel = (SocketChannel) key.channel();
        ByteBuffer currentBuffer = sockets.get(currentSocketChannel);

        int currentBytesRead = currentSocketChannel.read(currentBuffer);
        if (currentBytesRead == -1) {
            log.info("Соединение закрыто " + currentSocketChannel.getRemoteAddress());
            sockets.remove(currentSocketChannel);
            currentSocketChannel.close();
            return;
        }

        if (currentBytesRead != 0) {
            newMessage = ByteBuffer.allocate(currentBytesRead);
            newMessage.put(ByteBuffer.wrap(currentBuffer.array(), 0, currentBytesRead));
            currentBuffer.clear();
        }

        for (Entry<SocketChannel, ByteBuffer> entry : sockets.entrySet()) {
            SocketChannel socketChannel = entry.getKey();
            if (socketChannel == currentSocketChannel) {
                continue;
            }
            ByteBuffer buffer = entry.getValue();
            int bytesRead = socketChannel.read(buffer);
            if (bytesRead == 0 && newMessage != null) {
                buffer.put(ByteBuffer.wrap(newMessage.array()));
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            }
        }
    }

    public void start() {
        log.debug("Старт сервера");
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(host, port));
            serverChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            handle(selector, serverChannel);
        } catch (IOException err) {
            System.out.println(err.getMessage());
        }
        log.debug("Сервер закрыт");
    }

    private void handle(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        while (true) {
            selector.select();
            for (SelectionKey key : selector.selectedKeys()) {
                if (key.isValid()) {
                    try {
                        if (key.isAcceptable()) {
                            accept(selector, serverChannel);
                        } else if (key.isReadable()) {
                            read(key, selector);
                        } else if (key.isWritable()) {
                            write(key, selector);
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
            selector.selectedKeys().clear();
        }
    }
}