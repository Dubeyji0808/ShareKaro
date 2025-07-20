package ayush.sharekaro.Services;

import ayush.sharekaro.Utils.UploadUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class FileSharer {

    //so we will get a file path and we have to assign a port to it
    // ok so we have to make port unique not file
    // HashMap< Integer,String > existingfile = new HashMap<>(); // dont create normal instead add it in constructor
    HashMap< Integer,String > existingfile;
    public FileSharer(){
        existingfile = new HashMap<>();
    }
    public int offerfile( String filepath) {
        int port;
        while (true) {
            port = UploadUtils.generateport();
            if (existingfile.containsKey(port)) {
                System.out.println("Unable to provide port");
            } else {
                existingfile.put(port, filepath);
                return port;
            }
        }

    }

    // so now we will check for the start file server function on port
    public void startfileserver(int port) {
        //we want file related to it
        String filepath = existingfile.get(port);
        //error checking
        if (filepath == null) {
            System.out.println("No file associated to this port: " + port);
            return;
        }

        //we will create socket programming now
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            //ServerSocket is used by the server to listen for connections.
            //Socket is used by both server and client to send and receive data once connected.
            System.out.println("Serving File : " + new File(filepath).getName() + "on port : " + port);
            //create client socket
            Socket clientsocket = serverSocket.accept();
            System.out.println("Client connected: " + clientsocket.getInetAddress());
            new Thread(new FileSenderHandler(clientsocket, filepath)).start();
            //The thread:
            //Reads the file.
            //Sends it over the socket to the connected client.
            //Closes the socket when done.
        } catch (IOException ex) {
            System.err.println("Error starting file server on port " + port + ": " + ex.getMessage());

        }
    }
    // part of your app that actually sends the file to a connected client. --> FileSenderHandler
    //Use static when the inner class doesn’t need to touch anything from the outer class — like an independent helper.
    private static class FileSenderHandler implements Runnable{
        private final Socket clientsocket;
        private final String filepath;

        public FileSenderHandler(Socket clientsocket, String filepath) {
            this.clientsocket = clientsocket;
            this.filepath = filepath;
        }

        // override run method
        @Override
        public void run(){

//🟢 InputStream → for reading data (input coming into your program) read the file
//🔴 OutputStream → for writing data (output going out from your program) send to the client
            try(FileInputStream fis = new FileInputStream(filepath);
                OutputStream oss = clientsocket.getOutputStream()){
                // Send the filename as a header
                String filename = new File(filepath).getName();
                String header = "Filename: " + filename + "\n";
                oss.write(header.getBytes());

                // Send the file content
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    oss.write(buffer, 0, bytesRead);
                    //Even if the buffer was filled with 4096 bytes earlier, you only send the bytes you actually read.
                }
                //🔍 What happens in this loop?
                //The file exists on disk — e.g., resume.pdf is on your computer.
                //fis.read(buffer) does:
                //Goes to the file on disk.
                //Reads bytes from the file.
                //Fills the beginning of buffer[] with that file data.
                //Returns how many bytes it copied into the buffer. fis se file path mila use file ka content

                System.out.println("File '" + filename + "' sent to " + clientsocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error sending file to client: " + e.getMessage());
            } finally {
                try {
                    clientsocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }

            }
        }

    }

}
