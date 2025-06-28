package src.main.java.ayush.Controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import src.main.java.ayush.Services.FileSharer;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileController  {

    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    //fileSharer: An object that will handle offering and serving files (probably sharing files over sockets).
    //server: An instance of HttpServer from Java’s built-in lightweight HTTP server (not like Spring Boot).
    //uploadDir: A path to a temporary folder where uploaded files will be saved.
    //executorService: A thread pool to handle multiple HTTP requests concurrently (up to 10 at once).

    public FileController (int port) throws IOException {
        //The constructor is initializing and wiring together all the parts of your file sharing server:
        //HTTP server
        //Endpoints (upload/download)
        //File storage path
        //Thread pool
        //File sharing service
        this.fileSharer = new FileSharer();
        this.executorService = Executors.newFixedThreadPool(10);// 10 thread ka pool banaya to mange http request
        this.server = HttpServer.create(new InetSocketAddress(port), 0);//Starts an HTTP server listening on the given port. The second parameter (0) means it uses the default backlog for pending connections.
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "sharekaro-uploads";
        //System.getProperty("java.io.tmpdir")
        //This gets the temporary directory path of your system.
        //Example values:
        //On Windows: C:\Users\<username>\AppData\Local\Temp\
        //On Linux/Mac: /tmp/
        //Java uses this path to store temporary files.
        File uploadfiledir = new File(uploadDir);
        if(!uploadfiledir.exists()){
            uploadfiledir.mkdirs();
        }
        //making the endpoints
        server.createContext("/upload" , new UploadHandler());
        server.createContext("/download" , new downloadHandler());
        server.createContext("/", new CORSHandler());

        server.setExecutor(executorService);
        //Tells the HTTP server to use the thread pool you defined earlier to handle requests in parallel.

    }
    public void start() {
        server.start();
        System.out.println("API server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server stopped");
    }
    //This method is typically called from main() or your app’s entry point to boot up the API server.

    public static class CORSHandler implements HttpHandler /*You implement this method to define how to respond to requests for a particular route.*/ {

        @Override
        public void handle(HttpExchange exchange /*http exchnage conatain all important function like getRequestMethod() it has response reading methods*/) throws IOException{

            //http header is used to read http request header
            Headers headers = new Headers();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");//OPTIONS → special method browsers use to check if a CORS request is allowed (called a preflight request) . CORS:Browsers often use OPTIONS to check if a cross-origin request is permitted before sending the actual request
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            //Content-Type → needed when you're sending JSON or form-data
            //Authorization → needed if you're sending JWT tokens or auth headers
            //If you don’t include this, the browser won’t let your frontend send those headers.

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }


    }
}