import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public static void main(String[] args) {
    // Example arguments: Process ID, IPs, and Ports for other processes
    if (args.length < 2) {
        System.out.println("Usage: Process <ProcessID> <IP1:Port1> <IP2:Port2> <IP3:Port3>");
        return;
    }

    int processID = Integer.parseInt(args[0]);
    String[] ipsAndPorts = Arrays.copyOfRange(args, 1, args.length);
    String[] ips = new String[ipsAndPorts.length];
    int[] ports = new int[ipsAndPorts.length];

    for (int i = 0; i < ipsAndPorts.length; i++) {
        String[] ipAndPort = ipsAndPorts[i].split(":");
        ips[i] = ipAndPort[0];
        ports[i] = Integer.parseInt(ipAndPort[1]);
    }

    Process process = new Process(processID, ips, ports);
    process.startReceivingMessages();

    final int totalMessages = 100;
    int messageCount = 0;
    Map<Integer, Integer> receivedMessageCount = new HashMap<>();
    // Initialize the count of received messages from each process to 0
    for (int i = 1; i <= ips.length + 1; i++) {
        if (i != processID) receivedMessageCount.put(i, 0);
    }

    // Send messages
    while (messageCount < totalMessages) {
        process.broadcastMessage(STR."Message \{messageCount + 1} from Process \{processID}");
        messageCount++;

        // Simulate some work or wait to avoid flooding
        try {
            Thread.sleep(100); // 100 milliseconds wait
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Check if all messages have been delivered
    boolean allMessagesReceived;
    do {
        allMessagesReceived = receivedMessageCount.values().stream().allMatch(count -> count >= totalMessages);
        // Optionally, sleep for a while to reduce CPU usage in this polling loop
        try {
            Thread.sleep(500); // Wait for half a second before checking again
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    } while (!allMessagesReceived);

    System.out.println(STR."Process \{processID} has completed its message exchange.");
    // Terminate the process or clean up resources here if necessary
}
