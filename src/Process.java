import java.io.*;
import java.net.Socket;
import java.util.*;
import java.net.ServerSocket;
import java.util.concurrent.*;


public class Process {
    private final int processID;
    private Wire[] wires; // Connections to other processes
    private int[] vectorClock; // Vector clock for this process
    
    private int currentRound; // Round Number for this process
    private int totalProcesses;
    private String fileName;
    private boolean hasMessageSentInCurrentRound;
    private PriorityQueue<Message> messagesToBroadcast;
    
    private Random randomTimeGenerator = new Random();
    private final ConcurrentLinkedQueue<Message> messageBuffer; // Thread-safe message buffer

    private final Set<Message> receivedMessages; // Set to store received messages
    private final Set<Message> deliveredMessages; // Set to store delivered messages

    // Server variables
    private ServerSocket serverSocket;
    private ExecutorService serverExecutor;
    private int serverPort;

    public Process(int id, int port, String[] ips, int[] ports) {
        this.processID = id;
        // Check if ips and ports arrays have the same length
        if (ips.length != ports.length) {
            throw new IllegalArgumentException("The lengths of IPs and ports arrays must be the same.");
        }
        this.totalProcesses  = ips.length + 1; // Including this process
        this.vectorClock    = new int[totalProcesses]; // Initialize vector clock with zeros for all processes
        this.wires          = new Wire[totalProcesses - 1]; // Wires for connections to other processes, excluding self
        
        this.currentRound   = 0;
        this.fileName       = "process_" + id + ".txt";
        this.hasMessageSentInCurrentRound = false;
        this.messagesToBroadcast = new PriorityQueue<>(Comparator.comparingInt(Message::getRound));

        this.serverExecutor = Executors.newSingleThreadExecutor();
        this.serverPort     = port;
        startServer();

        this.messageBuffer     = new ConcurrentLinkedQueue<>(); // Initialize the message buffer
        this.receivedMessages  = new HashSet<>(); // Initialize the receivedMessages set
        this.deliveredMessages = new HashSet<>(); // Initialize the deliveredMessages set

        confirmAndEstablishConnections(ips, ports);
    }

    private void confirmAndEstablishConnections(String[] ips, int[] ports) {
        Scanner scanner = new Scanner(System.in); // Create a Scanner object
        System.out.println("Do you want to start establishing connections? (yes/no)");
        String userResponse = scanner.nextLine(); // Read user response

        if ("yes".equalsIgnoreCase(userResponse.trim())) {
            System.out.println("Establishing connections...");
            // If user confirms, proceed with delayed connection attempts
            for (int i = 0; i < ips.length; i++) {
                tryAddUniqueWire(ips[i], ports[i]);
            }
        } else {
            System.out.println("User did not confirm. Exiting...");
            // Optionally, implement logic to handle this case, such as shutting down the server or retrying the confirmation.
            // This might involve closing resources or simply logging the event, based on application requirements.
        }
    }


    private void tryAddUniqueWire(String ip, int port) {
        System.out.println("Creating Wire for IP: " + ip);
        Wire newWire = new Wire(ip, port); // Assume Wire can be constructed with IP and port directly
        // Synchronize access to the wires array to ensure thread safety

        boolean exists = false;
        for (Wire wire : wires) {
            if (wire != null && wire.isEquivalentTo(newWire)) {
                exists = true;
                System.out.println("Wire did exist...");
                break;
            }
        }
        if (!exists) {
            // Add newWire to the first null position in the wires array
            System.out.println("Wire did not exist and Created...");
            for (int i = 0; i < wires.length; i++) {
                if (wires[i] == null) {
                    wires[i] = newWire;
                    break;
                }
            }
        }
    }


    private void handleClientSocket(Socket clientSocket) throws IOException {
        // First, initialize the ObjectOutputStream
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        // Flush the stream to ensure the header is sent to the client
        objectOutputStream.flush();

        // Now, initialize the ObjectInputStream
        ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

        while (!clientSocket.isClosed()) {
            Message receivedMessage = null;
            try {
                //randomWait();
                receivedMessage = (Message) objectInputStream.readObject();
                if (receivedMessage != null) {
                    handleReceivedMessage(receivedMessage);
                }
            } catch (EOFException e) {
                // End of stream reached, close the connection
                break;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace(); // Handle exceptions appropriately, but continue listening
            }
        }
    }


    // Initialize and start the server
    private void startServer() {
        serverExecutor.submit(() -> {
            try {
                this.serverSocket = new ServerSocket(serverPort); // Bind to port
                System.out.println("Server started. Listening on port: " + serverPort);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client Socket accepted !");
                    // Handle each client connection in a new thread
                    new Thread(() -> {
                        try {
                            handleClientSocket(clientSocket);
                        } catch (IOException e) {
                            System.out.println("Error handling client socket.");
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }



    // Broadcast a message with the current vector clock
    public void broadcastMessage(String message) {
        if (!hasMessageSentInCurrentRound) {
            randomWait();
            vectorClock[processID - 1]++; // Increment own position in vector clock
            Message broadcastMessage = new Message(processID, message, vectorClock.clone(), currentRound);
            hasMessageSentInCurrentRound = true;
    
            for (Wire wire : wires) {
                wire.sendMessage(broadcastMessage);
            }
        } else {
            // Add the message to the priority queue for the next round
            Message queuedMessage = new Message(processID, message, vectorClock.clone(), currentRound + 1);
            messagesToBroadcast.offer(queuedMessage);
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
        // Add the message to the list of delivered messages
        receivedMessages.add(message);
    
        if (receivedMessages.size() == totalProcesses) {
            Set<Message> deliverable = new HashSet<>();
    
            // Iterate through the received messages that have not been delivered
            Iterator<Message> iterator = receivedMessages.iterator();
            while (iterator.hasNext()) {
                Message m = iterator.next();
                if (!deliveredMessages.contains(m)) {
                    deliverable.add(m);
                    iterator.remove(); // Remove the message from receivedMessages
                }
            }
    
            // Deliver all messages in deliverable, in increasing order of (ts(m), sender(m))
            deliverMessagesInOrder(deliverable);
    
            // Add the delivered messages to the deliveredMessages set
            deliveredMessages.addAll(deliverable);
    
            // Increment the round variable
            System.out.println("Round " + currentRound + ": finished !\n");
            currentRound++;

            // Reset the flag for the new round
            hasMessageSentInCurrentRound = false;

            // Broadcast queued messages for the new round
            while (!messagesToBroadcast.isEmpty() && messagesToBroadcast.peek().getRound() == currentRound) {
                Message queuedMessage = messagesToBroadcast.poll();
                broadcastMessage(queuedMessage.getContent());
            }

            
        }
    }

    private void deliverMessagesInOrder(Set<Process.Message> deliverable) {
        // Convert the set to a list for sorting
        List<Process.Message> messageList = new ArrayList<>(deliverable);

        // Sort the messages based on the ProcessID (low to high)
        messageList.sort(Comparator.comparingInt(Process.Message::getSenderId));

        // Create a StringBuilder to store the message contents
        StringBuilder sb = new StringBuilder();

        // Iterate through the sorted messages
        for (Process.Message message : messageList) {
            // Append the message content to the StringBuilder
            sb.append(message.getContent()).append("\n");
        }

        // Write the message contents to the process's text file
        try {
            FileWriter writer = new FileWriter(fileName, true); // Open the file in append mode
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    
    private void UpdateClock(Message message) {
        // Update vector clock to the pointwise maximum
        for (int i = 0; i < vectorClock.length; i++) {
            vectorClock[i] = Math.max(vectorClock[i], message.getVectorClock()[i]);
        }
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

                UpdateClock(typedMessage);

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
        private final int round;
    
        public Message(int senderId, String content, int[] vectorClock, int round) {
            this.senderId = senderId;
            this.content = content;
            this.vectorClock = vectorClock;
            this.round = round;
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
    
        public int getRound() {
            return round;
        }
    }
}

