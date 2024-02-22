import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Wire {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ExecutorService executor;

    public Wire(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.executor = Executors.newSingleThreadExecutor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to send a message
    public void sendMessage(Object message) {
        try {
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to receive a message with a callback
    public void receiveMessage(Consumer<Object> messageHandler) {
        executor.submit(() -> {
            try {
                Object message = in.readObject();
                messageHandler.accept(message);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
