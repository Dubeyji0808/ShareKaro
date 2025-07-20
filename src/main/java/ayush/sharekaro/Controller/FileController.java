package ayush.sharekaro.Controller;

import ayush.sharekaro.Services.FileSharer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;


public class FileController {

    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    //fileSharer: An object that will handle offering and serving files (probably sharing files over sockets).
    //server: An instance of HttpServer from Java’s built-in lightweight HTTP server (not like Spring Boot).
    //uploadDir: A path to a temporary folder where uploaded files will be saved.
    //executorService: A thread pool to handle multiple HTTP requests concurrently (up to 10 at once).

    public FileController(int port) throws IOException {
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
        if (!uploadfiledir.exists()) {
            uploadfiledir.mkdirs();
        }
        //making the endpoints
        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
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
        public void handle(HttpExchange exchange /*http exchnage conatain all important function like getRequestMethod() it has response reading methods*/) throws IOException {

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


    //now lets implement parser
    private static class MultipartParser {

        private final byte[] data;
        //byte[] data:
        //The raw HTTP request body. For file uploads, this contains a mix of:
        //headers (like filename, content-type),
        //boundary separators, and actual binary file content.
        private final String boundary; // its the seperator in http request

        private MultipartParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse() {
            try {
                String dataAsString = new String(data); // we have to change this for video file sharing
                //why it still able to share .mp3 and .mp4 is because our file are small and not corrupted but if file is big it will have correpted then it will cause problem.
                String filenameMarker = "filename=\"";
                int filenameStart = dataAsString.indexOf(filenameMarker); // take one and two parameter string to find and from where to start;
                if (filenameStart == -1) {
                    return null;
                }

                filenameStart += filenameMarker.length();
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String filename = dataAsString.substring(filenameStart, filenameEnd);

                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream"; // Default
                //"application/octet-stream" is the default MIME type used when the actual content type is unknown or not specified.
                //
                //🔤 Breakdown:
                //MIME type: A label that tells the browser or system what kind of data the file is.
                //"application": Category (i.e., it's some application data — not text, image, etc.)
                //"octet-stream": Just means "raw bytes" — literally, it's just a stream of 8-bit binary data.

                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }

                String headerEndMarker = "\r\n\r\n";
                //In HTTP/multipart data, there’s always an empty line (two \r\ns) after the headers.
                //Example:
                //Content-Disposition: ...
                //Content-Type: ...
                //
                //<---- empty line ---->
                //FILE CONTENT STARTS HERE
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) {
                    return null;
                }
                int contentStart = headerEnd + headerEndMarker.length();

                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data, boundaryBytes, contentStart);

                if (contentEnd == -1) {
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }

                if (contentEnd == -1 || contentEnd <= contentStart) {
                    return null;
                }

                byte[] fileContent = new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);

                return new ParseResult(filename, contentType, fileContent);


            } catch (Exception e) {
                System.out.println("Error parsing multipart data: " + e.getMessage());
                return null;
            }
        }

        public static class ParseResult {
            public final String filename;
            public final String contentType;
            public final byte[] fileContent;

            public ParseResult(String filename, String contentType, byte[] fileContent) {
                this.filename = filename;
                this.contentType = contentType;
                this.fileContent = fileContent;
            }
        }

        private int findSequence(byte[] data, byte[] sequence, int startPos) {
            //Parameters:
            //data: The full byte array (e.g., entire HTTP request body)
            //sequence: The byte pattern you want to find (like boundary \r\n--boundary)
            //startPos: The position to start searching from

            outer:
            //You can name any loop in Java with a label like this.
            for (int i = startPos; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
    }

    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            //HttpExchange represents the entire HTTP request-response exchange.
            //exchange contains information about the request (method, headers, body) and lets you build a response.
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");

            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            try {
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();

                MultipartParser parser = new MultipartParser(requestData, boundary);
                MultipartParser.ParseResult result = parser.parse();

                if (result == null) {
                    String response = "Bad Request: Could not parse file content";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                String filename = result.filename;
                if (filename == null || filename.trim().isEmpty()) {
                    filename = "unnamed-file";
                }

                String uniqueFilename = UUID.randomUUID().toString() + "_" + new File(filename).getName();
                String filePath = uploadDir + File.separator + uniqueFilename;

                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(result.fileContent);
                }

                int port = fileSharer.offerfile(filePath);

                new Thread(() -> fileSharer.startfileserver(port)).start();

                String jsonResponse = "{\"port\": " + port + "}";
                headers.add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }

            } catch (Exception e) {
                System.err.println("Error processing file upload: " + e.getMessage());
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

    }
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*"); // Allow requests from any domain (CORS)

            // Allow only GET requests
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length); // Send 405 error
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return; // Stop execution if not GET
            }

            // Get the path from URL and extract port number (e.g., /download/12345 -> 12345)
            String path = exchange.getRequestURI().getPath();
            String portStr = path.substring(path.lastIndexOf('/') + 1);

            try {
                int port = Integer.parseInt(portStr); // Convert port to integer

                // Connect to the sender socket at localhost:<port>
                try (Socket socket = new Socket("localhost", port);
                     InputStream socketInput = socket.getInputStream()) {

                    // Create a temporary file to store incoming data
                    File tempFile = File.createTempFile("download-", ".tmp");
                    String filename = "downloaded-file"; // Default filename if none is provided

                    // Open stream to write received data to the temp file
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[4096]; // Buffer for reading data
                        int bytesRead;

                        // === Read header to get the original filename ===
                        ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
                        int b;
                        while ((b = socketInput.read()) != -1) {
                            if (b == '\n') break; // End of header
                            headerBaos.write(b);
                        }

                        // Parse filename from header if it exists
                        String header = headerBaos.toString().trim();
                        if (header.startsWith("Filename: ")) {
                            filename = header.substring("Filename: ".length());
                        }

                        // === Read the actual file content and save to temp file ===
                        while ((bytesRead = socketInput.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    // === Set response headers to make browser download the file ===
                    headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    headers.add("Content-Type", "application/octet-stream");

                    // Send HTTP 200 OK with file size
                    exchange.sendResponseHeaders(200, tempFile.length());

                    // === Send file content to the client ===
                    try (OutputStream os = exchange.getResponseBody();
                         FileInputStream fis = new FileInputStream(tempFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }

                    // Delete the temp file after sending
                    tempFile.delete();

                }
            } catch (Exception e) {
                // Handle errors (invalid port, connection failure, etc.)
                String errorResponse = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorResponse.getBytes());
                }
            }
        }
    }
}
