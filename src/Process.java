import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.Serializable;

public class Process {
    private final int processID;
    private Wire[] wires; // Connections to other processes
    private int[] vectorClock; // Vector clock for this process
    private Random randomTimeGenerator = new Random();
    private final ConcurrentLinkedQueue<Message> messageBuffer; // Thread-safe message buffer

    public Process(int id, String[] ips, int[] ports) {
        this.processID = id;
        // Check if ips and ports arrays have the same length
        if (ips.length != ports.length) {
            throw new IllegalArgumentException("The lengths of IPs and ports arrays must be the same.");
        }
        int totalProcesses = ips.length + 1; // Including this process
        this.vectorClock = new int[totalProcesses]; // Initialize vector clock with zeros for all processes
        this.wires = new Wire[totalProcesses - 1]; // Wires for connections to other processes, excluding self
        this.messageBuffer = new ConcurrentLinkedQueue<>(); // Initialize the message buffer

        for (int i = 0, j = 0; i < totalProcesses - 1; i++) {
            if (i != processID - 1) { // Exclude self connection
                wires[j++] = new Wire(ips[i], ports[i]);
            }
        }
    }


    // Broadcast a message with the current vector clock
    public void broadcastMessage(String message) {

        randomWait();
        vectorClock[processID - 1]++; // Increment own position in vector clock
        Message broadcastMessage = new Message(processID, message, vectorClock.clone());

        for (Wire wire : wires) {
            wire.sendMessage(broadcastMessage);
        }
    }

    // Method to start receiving messages and handle them based on vector clock algorithm
    public void startReceivingMessages() {
        for (Wire wire : wires) {
            wire.receiveMessage(this::handleReceivedMessage);
        }
    }

    // Check if a message is deliverable based on vector clock comparison
    private boolean isDeliverable(Message message) {
        boolean deliverable = true; // Assume the message is deliverable initially

        for (int i = 0; i < vectorClock.length; i++) {
            if (i == message.getSenderId() - 1) {
                // For the sender, its clock must be exactly one more than this process's clock
                if (message.getVectorClock()[i] != vectorClock[i] + 1) {
                    deliverable = false;
                    break;
                }
            } else {
                // For all other processes, their clock should not be more than this process's clock
                if (message.getVectorClock()[i] > vectorClock[i]) {
                    deliverable = false;
                    break;
                }
            }
        }
        return deliverable;
    }

    // Deliver the message and update the process's vector clock
    private void deliverMessage(Message message) {
        // Update vector clock to the pointwise maximum
        for (int i = 0; i < vectorClock.length; i++) {
            vectorClock[i] = Math.max(vectorClock[i], message.getVectorClock()[i]);
        }
        System.out.println("Message delivered to Process " + processID + ": " + message.getContent());

    }


    // Check and deliver any buffered messages that are now deliverable
    private void checkAndDeliverBufferedMessages() {
        Iterator<Message> iterator = messageBuffer.iterator();
        while (iterator.hasNext()) {
            Message bufferedMessage = iterator.next();
            if (isDeliverable(bufferedMessage)) {
                iterator.remove();
                deliverMessage(bufferedMessage);
            }
        }
    }

    // Handle the logic for receiving and delivering messages based on vector clocks
    private void handleReceivedMessage(Object message) {
        synchronized (this) {
            if (message instanceof Message) {
                Message typedMessage = (Message) message;
                if (isDeliverable(typedMessage)) {
                    deliverMessage(typedMessage);
                    checkAndDeliverBufferedMessages();
                } else {
                    messageBuffer.add(typedMessage);
                }
            } else {
                System.out.println("Received object is not of type Message");
            }
        }
    }


    // Generate a random wait time for message sending
    private void randomWait() {
        try {
            Thread.sleep(randomTimeGenerator.nextInt(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // Nested class for messages with vector clocks
    public static class Message implements Serializable {
        private final int senderId;
        private final String content;
        private final int[] vectorClock;

        public Message(int senderId, String content, int[] vectorClock) {
            this.senderId = senderId;
            this.content = content;
            this.vectorClock = vectorClock;
        }

        public int getSenderId() {
            return senderId;
        }

        public String getContent() {
            return content;
        }

        public int[] getVectorClock() {
            return vectorClock;
        }
    }
}

