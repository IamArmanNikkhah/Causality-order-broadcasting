import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

public class Main {

    int processID;
    Process process;
    int noOtherProcesses;
    public void main(String[] args) {
        // Example arguments: Process ID, IPs, and Ports for other processes
        if (args.length < 2) {
            System.out.println("Usage: Process <ProcessID> <ServerPort> <IP1:Port1> <IP2:Port2> <IP3:Port3>");
            return;
        }

        this.processID = Integer.parseInt(args[0]);
        int serverPort = Integer.parseInt(args[1]);
        String[] ipsAndPorts = Arrays.copyOfRange(args, 2, args.length);
        this.noOtherProcesses = ipsAndPorts.length;
        String[] ips = new String[ipsAndPorts.length];
        int[] ports = new int[ipsAndPorts.length];

        for (int i = 0; i < ipsAndPorts.length; i++) {
            String[] ipAndPort = ipsAndPorts[i].split(":");
            ips[i] = ipAndPort[0];
            ports[i] = Integer.parseInt(ipAndPort[1]);
        }

        this.process = new Process(processID,serverPort, ips, ports);

        System.out.println("Connections via wires have been successfully established for Process " + processID + ".");

        waitForTrigger(1234);

        //process.startReceivingMessages();
        //Scanner scanner = new Scanner(System.in);
        //System.out.println("Do you wish to start sending messages? (yes/no)");
        //String userResponse = scanner.nextLine().trim().toLowerCase();

        //if (userResponse.equals("yes")) {
        //    // Logic to start sending messages
        //    startSendingMessages();

        //} else {
        //    System.out.println("Process " + processID + " will not send messages.");
        //}

        // Terminate the process or clean up resources here if necessary
    }

    private void startSendingMessages() {
        System.out.println("Starting to send messages from Process " + processID + ".");

        final int totalMessages = 100;
        int messageCount = 0;

        Map<Integer, Integer> receivedMessageCount = new HashMap<>();

        for (int i = 1; i <= noOtherProcesses + 1; i++) {
            if (i != processID) receivedMessageCount.put(i, 0);
        }

        while (messageCount < totalMessages) {
            System.out.println("Sending Message number: " + messageCount);
            process.broadcastMessage("Message " + (messageCount + 1) + " from Process " + processID);
            messageCount++;

            try {
                Thread.sleep(100); // 100 milliseconds wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        boolean allMessagesReceived;
        do {
            allMessagesReceived = receivedMessageCount.values().stream().allMatch(count -> count >= totalMessages);
            try {
                Thread.sleep(500); // Wait for half a second before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (!allMessagesReceived);

        System.out.println("Process " + processID + " has completed its message exchange.");
    }


    private void waitForTrigger(int triggerPort) {
        try (ServerSocket serverSocket = new ServerSocket(triggerPort)) {
            System.out.println("Waiting for trigger on port " + triggerPort + "...");
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String triggerMessage = in.readLine();
                if ("START".equals(triggerMessage)) {
                    System.out.println("Trigger received. Starting to send messages...");
                    // Place the logic here to start sending messages
                    startSendingMessages();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}