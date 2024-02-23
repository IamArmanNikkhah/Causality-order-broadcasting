import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TriggerClient {

    private List<String> processAddresses;
    private List<Integer> processPorts;

    public TriggerClient() {
        this.processAddresses = new ArrayList<>();
        this.processPorts = new ArrayList<>();
    }

    /**
     * Adds a process address and port for triggering.
     *
     * @param address the IP address of the process
     * @param port    the port on which the process listens for triggers
     */
    public void addProcess(String address, int port) {
        processAddresses.add(address);
        processPorts.add(port);
    }

    /**
     * Sends the trigger to all registered processes to start their operations.
     */
    public void sendTriggers() {
        for (int i = 0; i < processAddresses.size(); i++) {
            try {
                sendTrigger(processAddresses.get(i), processPorts.get(i));
                System.out.println("Trigger sent to " + processAddresses.get(i) + ":" + processPorts.get(i));
            } catch (IOException e) {
                System.err.println("Failed to send trigger to " + processAddresses.get(i) + ":" + processPorts.get(i));
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a trigger message to a single process.
     *
     * @param address the IP address of the process
     * @param port    the port on which the process listens for triggers
     * @throws IOException if an I/O error occurs when sending the trigger
     */
    private void sendTrigger(String address, int port) throws IOException {
        try (Socket socket = new Socket(address, port);
             OutputStream out = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true)) {
            writer.println("START");
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TriggerClient <IP1:Port1> <IP2:Port2> ...");
            return;
        }

        TriggerClient client = new TriggerClient();

        // Parse command-line arguments for IP addresses and ports
        for (String arg : args) {
            try {
                String[] parts = arg.split(":");
                if (parts.length != 2) {
                    System.out.println("Invalid address format: " + arg);
                    continue;
                }
                String address = parts[0];
                int port = Integer.parseInt(parts[1]);
                client.addProcess(address, port);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number in argument: " + arg);
            }
        }

        // Send triggers
        if (!client.processAddresses.isEmpty()) {
            client.sendTriggers();
        } else {
            System.out.println("No valid process addresses provided. Exiting.");
        }
    }

}
