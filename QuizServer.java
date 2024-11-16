import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class QuizServer {

    // Updated set of questions and their answers
    private static final String[][] QUESTIONS = {
            {"Enter the name of matching layer - Protocols that are part of a distributed network application.", "application layer"},
            {"Enter the name of matching layer - Transfer of data between one process and another process (typically on different hosts)", "transport layer"},
            {"Enter the name of matching layer - Delivery of datagrams from a source host to a destination host (typically).", "network layer"},
            {"Enter the name of matching layer - Transfer of data between neighboring network devices.", "link layer"},
            {"Enter the name of matching layer - Transfer of a bit into and out of a transmission media.", "physical layer"}
    };

    // Atomic counter for client numbers
    private static final AtomicInteger clientCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(5); // ThreadPool with 5 threads
        try (ServerSocket serverSocket = new ServerSocket(8888)) { // Create server socket on port 8888
            System.out.println("Quiz Server is running...");

            while (true) { // Keep server running for multiple clients
                System.out.println("Waiting for a client...");
                Socket clientSocket = serverSocket.accept(); // Wait for a client connection
                int clientNumber = clientCounter.incrementAndGet(); // Increment client counter
                System.out.println("Client " + clientNumber + " connected!");

                // Submit client handling task to the thread pool
                threadPool.submit(new ClientHandler(clientSocket, QUESTIONS, clientNumber));
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            // Shutdown thread pool gracefully
            threadPool.shutdown();
            System.out.println("Server shut down.");
        }
    }
}

// ClientHandler implements Runnable to handle each client in a separate thread
class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String[][] questions;
    private final int clientNumber;

    public ClientHandler(Socket clientSocket, String[][] questions, int clientNumber) {
        this.clientSocket = clientSocket;
        this.questions = questions;
        this.clientNumber = clientNumber;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // To read data from the client
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())) // To send data to the client
        ) {
            // Notify client of their number
            out.write("Welcome, Client " + clientNumber + "!\n");
            out.flush();

            // Create a list of question indices and shuffle for random order
            List<Integer> questionOrder = new ArrayList<>();
            for (int i = 0; i < questions.length; i++) {
                questionOrder.add(i);
            }
            Collections.shuffle(questionOrder); // Shuffle the question order

            int score = 0; // Initialize score

            // Iterate through the shuffled question indices
            for (int i = 0; i < questionOrder.size(); i++) {
                int questionIndex = questionOrder.get(i);
                String[] question = questions[questionIndex];

                // Send question to client with its unique number in ASCII-based format
                out.write("QUESTION" + (i + 1) + ":" + question[0] + "\n");
                out.flush();

                // Read client's answer
                String clientAnswer = in.readLine();
                if (clientAnswer == null) {
                    System.out.println("Client " + clientNumber + " disconnected unexpectedly.");
                    break; // End the quiz for this client
                }

                // Check if the client's message starts with "ANSWER:"
                if (!clientAnswer.startsWith("ANSWER:")) {
                    out.write("RESULT:INVALID:Invalid answer format. Please use 'ANSWER:<your answer>'\n");
                    out.flush();
                    continue; // Skip to the next question
                }

                String answer = clientAnswer.substring(7).trim().toLowerCase();
                if (answer.isEmpty()) {
                    out.write("RESULT:INVALID:Answer cannot be empty\n");
                    out.flush();
                    continue; // Skip to the next question
                }

                String correctAnswer = question[1];
                if (correctAnswer == null) {
                    out.write("RESULT:ERROR:Server-side issue. Skipping this question.\n");
                    out.flush();
                    continue; // Skip to the next question
                }

                // Normalize and compare the answers
                answer = answer.replace("layer", "").trim();
                correctAnswer = correctAnswer.toLowerCase().replace("layer", "").trim();

                if (answer.equals(correctAnswer)) {
                    // If the answer is correct
                    score++;
                    out.write("RESULT:CORRECT\n");
                } else {
                    // If the answer is incorrect, send the correct answer
                    out.write("RESULT:WRONG:" + question[1] + "\n");
                }
                out.flush();
            }

            // Send the final score to the client
            out.write("QUIZ_END:Your total score: " + score + "/" + questions.length + "\n");
            out.flush();

        } catch (IOException e) {
            System.out.println("Error handling Client " + clientNumber + ": " + e.getMessage());
        } finally {
            // Ensure the client socket is closed
            try {
                clientSocket.close();
                System.out.println("Client " + clientNumber + " connection closed.");
            } catch (IOException e) {
                System.out.println("Error closing Client " + clientNumber + "'s socket: " + e.getMessage());
            }
        }
    }
}
