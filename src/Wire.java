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
            System.out.println("Socket has been Created ... ");
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.executor = Executors.newSingleThreadExecutor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Wire(Socket socket) {
        try {
            this.socket = socket;
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

    public boolean isEquivalentTo(Wire otherWire) {
        // Check if both sockets are connected
        if (this.socket == null || otherWire.socket == null) {
            return false;
        }

        // Compare remote IP addresses and ports
        boolean isSameRemoteIP = this.socket.getInetAddress().equals(otherWire.socket.getInetAddress());
        boolean isSameRemotePort = this.socket.getPort() == otherWire.socket.getPort();

        return isSameRemoteIP && isSameRemotePort;
    }


}