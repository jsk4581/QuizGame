import java.io.*;
import java.net.*;

public class QuizClient {

    public static void main(String[] args) {
        String serverIP = "localhost"; // Default IP
        int serverPort = 8888; // Default port

        // Attempt to read server details from configuration file
        File configFile = new File("server_info.dat");
        if (configFile.exists()) {
            try (BufferedReader configReader = new BufferedReader(new FileReader(configFile))) {
                serverIP = configReader.readLine().trim(); // Read IP from the first line
                String portLine = configReader.readLine().trim(); // Read port from the second line
                try {
                    serverPort = Integer.parseInt(portLine); // Parse port as an integer
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port in configuration file. Using default port 8888.");
                }
            } catch (IOException e) {
                System.out.println("Error reading configuration file. Using default values.");
            }
        } else {
            System.out.println("Configuration file not found. Using default values.");
        }

        try (Socket socket = new Socket(serverIP, serverPort)) { // Connect to server with IP and port
            System.out.println("Connected to Quiz Server");

            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // To receive data from the server
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in)); // To read user input from the console
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())) // To send data to the server
            ) {
                String serverMessage; // Variable to store messages from the server

                // Loop to handle the quiz interaction
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.startsWith("QUESTION")) {
                        // Display the question to the user
                        System.out.println(serverMessage);
                        System.out.print("Your answer: ");
                        String answer = userInput.readLine(); // Read the user's answer
                        out.write("ANSWER:" + answer + "\n"); // Send the answer to the server
                        out.flush();
                    } else if (serverMessage.startsWith("RESULT:")) {
                        if (serverMessage.startsWith("RESULT:CORRECT")) {
                            System.out.println("Correct!");
                        } else if (serverMessage.startsWith("RESULT:WRONG:")) {
                            System.out.println("Wrong! The correct answer is: " + serverMessage.substring(13));
                        } else if (serverMessage.startsWith("RESULT:INVALID:")) {
                            System.out.println("Invalid answer: " + serverMessage.substring(15));
                        } else if (serverMessage.startsWith("RESULT:ERROR:")) {
                            System.out.println("Error: " + serverMessage.substring(13));
                        }
                    } else if (serverMessage.startsWith("QUIZ_END:")) {
                        // If the quiz has ended, display the final score
                        System.out.println(serverMessage);
                        break; // Exit the loop
                    }
                }

            }
        } catch (IOException e) {
            // Handle client-side errors
            System.out.println("Client error: Unable to connect to Quiz Server");
            System.out.println("Error: " + e.getMessage());
        }
    }
}
