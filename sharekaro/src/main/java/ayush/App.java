package src.main.java.ayush;


import src.main.java.ayush.Controller.FileController;
import java.io.IOException;

public class App {
    public static void main(String[] args) {
        try {
            // Start the API server on port 8080
            FileController fileController = new FileController(8080);
            fileController.start();

            System.out.println("PeerLink server started on port 8080");
            System.out.println("UI available at http://localhost:3000");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                fileController.stop();
            }));
            // about runtime This adds a shutdown hook, which is a special thread that runs before the JVM shuts down.
            //It's useful to do clean-up tasks like closing a server, saving data, logging, etc.
            //Without it, the server might be killed immediately (possibly leaving resources in an inconsistent state).

            System.out.println("Press Enter to stop the server");
            System.in.read();

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
