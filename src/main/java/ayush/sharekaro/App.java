package ayush.sharekaro;

import ayush.sharekaro.Controller.FileController;

import java.io.IOException;

/**
 * ShareKaro - P2P File Sharing Application
 */
public class App {
    public static void main(String[] args) {
        try {
            // Start the API server on port 8080
            FileController fileController = new FileController(8080);
            fileController.start();

            System.out.println("ShareKaro server started on port 8080");
            System.out.println("UI available at http://localhost:3000");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                fileController.stop();
            }));

            System.out.println("Press Enter to stop the server");
            System.in.read();

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
