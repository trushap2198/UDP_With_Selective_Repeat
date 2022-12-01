import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.*;

/**
 * This class contains the implementation of UDP Server.
 *
 */
public class UDPServer {

    private static int statusCode = 200;;
    static boolean debugFlag = false;
    static String dir = System.getProperty("user.dir");
    static List<String> filelist = new ArrayList<>();

    static File currentFolder;
    static File parentDirectory;
    static int timeout = 3000;
    static int port = 8080;
    List<String> clientRequestList;


    public static void main(String[] args) throws Exception {
        String request;
        List<String> requestList = new ArrayList<>();

        //String dir = System.getProperty("user.dir");

        System.out.println("\nCurrent Directory is -> " + dir + "");
        System.out.print("-->");
        Scanner sc = new Scanner(System.in);

        request = sc.nextLine();
        if (request.isEmpty()) {
            System.out.println("Invalid Command Please try again!!");
        }
        String[] requestArray = request.split(" ");

        requestList.addAll(Arrays.asList(requestArray));

        if (requestList.contains("-v")) {
            debugFlag = true;
        }

        if (requestList.contains("-p")) {
            String portStr = requestList.get(requestList.indexOf("-p") + 1).trim();
            port = Integer.parseInt(portStr);
        }

        if (requestList.contains("-d")) {
            dir = request.substring(request.indexOf("-d")+3);
            System.out.println("Selected directory for operations : " + dir + "\n");
        }

        //debugFlag = true;
        if (debugFlag)
            System.out.println("Server is up and it assign to port Number: " + port);

        currentFolder = new File(dir);

        UDPServer server = new UDPServer();

        Runnable task = () -> {
            try {
                server.listenAndServe(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(task);
        thread.start();


       // server.listenAndServe(port);



    }

    /**
     * This method will extract payload from client request
     *
     */
    private void listenAndServe(int port) throws Exception
    {
        try (DatagramChannel channel = DatagramChannel.open())
        {
            channel.bind(new InetSocketAddress(port));  //will open datagram channel that will receive packets on port 8080
            Packet response;
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);

            for (;;) {
                buf.clear();
                SocketAddress router = channel.receive(buf); //The receive() method will copy the content of a received packet of data into the given Buffer.
                if (router != null) {
                    // Parse a packet from the received raw data.
                    buf.flip();
                    Packet packet = Packet.fromBuffer(buf);
                    buf.flip();

                    String requestPayload = new String(packet.getPayload(), UTF_8);
                    // Send the response to the router not the client.
                    // The peer address of the packet is the address of the client already.
                    // We can use toBuilder to copy properties of the current packet.
                    // This demonstrate how to create a new packet from an existing packet.

                    if (requestPayload.equals("Hi S"))
                    {
                        System.out.println("Client: " + requestPayload);
                        response = packet.toBuilder().setPayload("Hi C".getBytes()).create();
                        channel.send(response.toBuffer(), router);
                        System.out.println("Sending Hi from Server");
                    }
                    else if (requestPayload.contains("httpfs") || requestPayload.contains("httpc"))
                    {
                        System.out.println("Client: " + requestPayload);
                        String responsePayload = processPayloadRequest(requestPayload);
                        if(responsePayload.getBytes().length> Packet.MAX_LEN)
                            response = packet.toBuilder().setPayload("Data size exceeds allowed limit".getBytes()).create();
                        else
                            response = packet.toBuilder().setPayload(responsePayload.getBytes()).create();
                        channel.send(response.toBuffer(), router);

                    }
                    else if (requestPayload.equals("Received"))
                    {
                        System.out.println("Client: " + requestPayload + "\nSending Close");
                        response = packet.toBuilder().setPayload("Close".getBytes()).create();
                        channel.send(response.toBuffer(), router);

                    }
                    else if (requestPayload.equals("Ok"))
                    {
                        System.out.println("Client: " + requestPayload);
                        System.out.println(requestPayload + " received..!");

                    }
                }
            }
        }

    }

    /**
     * This method proccesses the payload request from the client's input and will return the response body.
     *
     * @param request client's request
     * @return response body
     */
    private String processPayloadRequest(String request) throws Exception {

        String url = "";
        String response = "";
        String verboseBody = "";
        boolean verbose = false;

        List<String> requestData = Arrays.asList(request.split(" "));

        if(debugFlag)
            System.out.println("Server is processing Payload Request");


        for(String d : requestData)
        {
            if(d.startsWith("http://"))
                url = d;
        }

        if(url.contains(" "))
        {
            url = url.split(" ")[0];
        }

        URI uri = new URI(url);

        String host = uri.getHost();


        if(requestData.contains("-v"))
            verbose = true;



        String body = "{\n";
        body = body + "\t\"args\":";
        body = body + "{},\n";
        body = body + "\t\"headers\": {";


        for (int i = 0; i < requestData.size(); i++)
        {
            if (requestData.get(i).equals("-h")) {

                String t1 = requestData.get(i+1).split(":")[0];
                String t2 = requestData.get(i+1).split(":")[1];
                body = body + "\n\t\t\"" + t1 + "\": \"" + t2 + "\",";
            }
        }


        body = body + "\n\t\t\"Connection\": \"close\",\n";
        body = body + "\t\t\"Host\": \"" + host + "\"\n";



        body = body + "\t},\n";

        // GET or POST
        String requestType;

        if(url.endsWith("get/"))
            requestType = "GetFilesList";
        else if(url.contains("get"))
            requestType = "GetFileContent";
        else
            requestType = "POST";


        //Get list of files in the current directory
        if(requestType.equals("GetFilesList"))
        {

//            parentDirectory = currentFolder;
            filelist = new ArrayList<>();
            body = body + "\t\"files\": { ";
            List<String> files = getFilesFromDir(currentFolder,filelist);

            //Can use files directly
            List<String> fileFilterList = new ArrayList<String>();
            fileFilterList.addAll(files);

            for (int i = 0; i < fileFilterList.size() - 1; i++) {
                body = body + files.get(i) + " ,\n\t\t\t    ";
            }

            body = body + fileFilterList.get(fileFilterList.size() - 1) + " },\n";
            statusCode = 200;

        }


        // Get file content of the requested file
        else if(requestType.equals("GetFileContent"))
        {
            String fileContent = "";

            String requestedFile;
            requestedFile = url.substring(url.indexOf("get/") + 4);

            List<String> files = getFilesFromDir(currentFolder,filelist);
            System.out.println("Final file list:" + files);


            if (!files.contains(requestedFile))
            {
                statusCode = 404;
            }
            else
            {

                File file = new File(dir + "/" + requestedFile);
                fileContent = readDataFromFile(file);
                body = body + "\t\"data\": \"" + fileContent +"\",\n";

                statusCode = 200;
            }


        }

        // Post request
        else if(requestType.equals("POST"))
        {

            String requestedFile;
            String data = "";

            requestedFile = url.substring(url.indexOf("post/") + 5);
            List<String> files = getFilesFromDir(currentFolder,filelist);


            if (!files.contains(requestedFile))
                statusCode = 202;
            else
                statusCode = 201;

            int index = requestData.indexOf("-d");

            for(int i = index + 1 ; i < requestData.size() ; i++)
            {
                data = data + requestData.get(i) + " ";
            }


            File file = new File(dir + "/" + requestedFile);
            writeResponseToFile(file, data);

        }

        if(statusCode == 200)
        {
            body = body + "\t\"status\": \"" + "HTTP/1.1 200 OK" + "\",\n";
        }
        else if(statusCode == 201)
        {
            body = body + "\t\"status\": \"" + "HTTP/1.1 201 FILE OVER-WRITTEN" + "\",\n";
        }
        else if(statusCode == 202)
        {
            body = body + "\t\"status\": \"" + "HTTP/1.1 202 NEW FILE CREATED" + "\",\n";
        }
        else if(statusCode == 404)
        {
            body = body + "\t\"status\": \"" + "HTTP/1.1 404 FILE NOT FOUND" + "\",\n";
        }


        body = body + "\t\"origin\": \"" + InetAddress.getLocalHost().getHostAddress() + "\",\n";
        body = body + "\t\"url\": \"" + url + "\"\n";
        body = body + "}\n";



        if(verbose)
        {
            verboseBody = verboseBody + "HTTP/1.1 200 OK\n";
            verboseBody = verboseBody + "Date: " + java.util.Calendar.getInstance().getTime() + "\n";
            verboseBody = verboseBody + "Content-Type: application/json\n";
            verboseBody = verboseBody + "Content-Length: "+ body.length() +"\n";
            verboseBody = verboseBody + "Connection: close\n";
            verboseBody = verboseBody + "Server: Localhost\n";
            verboseBody = verboseBody + "Access-Control-Allow-Origin: *\n";
            verboseBody = verboseBody + "Access-Control-Allow-Credentials: true\n";

            response = verboseBody;
            response = response + body;
        }
        else
        {
            response = body;
        }


        if(debugFlag)
        {
            System.out.println("Sending response to Client..");

        }

        System.out.println(url);
        System.out.println("response size:" + response.getBytes().length);
        System.out.println("response:" + response);
        return response;
    }

    /**
     * This method will give list of files from specific directory
     *
     * @return List of files
     */
    static private List<String> getFilesFromDir(File currentDir, List<String> filelist ) {

        if(currentDir == null){
            return null;
        }

        for (File file : currentDir.listFiles()) {
            if (!file.isDirectory()) {
                if(Arrays.asList(currentFolder.listFiles()).contains(file))
                {
                    if(!filelist.contains(file.getName()))
                        filelist.add(file.getName());
                    else
                        ;
                }
                else{
                    file.setExecutable(false);
                    file.setReadable(false);
                    file.setWritable(false);
                    if(!filelist.contains(file.getName()))
                        filelist.add(file.getName());
                }
            }
            else
            {
                if(!filelist.contains(file.getName()))
                    filelist.add(file.getName());

                //System.out.println("dir is recusive" + filelist);
                getFilesFromDir(file,filelist);
            }
            //to list the files and the subdirectories, but the packet size is an issue!!!
           // filelist.add(file.getName());
        }
        return filelist;
    }



    static public void writeResponseToFile(File fileName, String data)
    {
        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));

            bufferedWriter.write(data);
            bufferedWriter.close();

            if(debugFlag)
                System.out.println("Response successfully saved to " + fileName);

        } catch (IOException ex) {
            if(debugFlag)
                System.out.println("Error Writing file named '" + fileName + "'" + ex);
        }
    }



    static public String readDataFromFile(File fileName)
    {
        StringBuilder lines = new StringBuilder("");
        String line = null;

        try
        {

            if(fileName.canRead() == false){
                lines.append("Cannot read the file");

            }
            else {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));

                while ((line = bufferedReader.readLine()) != null) {
                    lines.append(line);

                }
                bufferedReader.close();
            }
        }
        catch(IOException ex)
        {
            if(debugFlag)
                System.out.println("Error reading file named '" + fileName + "'" + ex);
        }

        return lines.toString();
    }
}
