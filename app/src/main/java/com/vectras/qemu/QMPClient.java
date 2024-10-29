package com.vectras.qemu;

import java.io.*;
import java.net.*;
import org.json.*;

public class QMPClient {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public QMPClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
        // Read initial QMP greeting
        System.out.println("QMP Greeting: " + reader.readLine());
        
        // Send QMP capabilities negotiation
        try {
            sendCommand(new JSONObject().put("execute", "qmp_capabilities"));
        } catch (JSONException e) {
            com.vectras.vm.logger.VectrasStatus.logError("<font color='red'>VTERM ERROR: >" + e.getMessage().toString() + "</font>");
        }
    }

    private void sendCommand(JSONObject command) throws IOException {
        writer.write(command.toString() + "\r\n");
        writer.flush();
    }

    public String receiveResponse() throws IOException {
        return reader.readLine();
    }

    public void close() throws IOException {
        reader.close();
        writer.close();
        socket.close();
    }

    public static void main(String[] args) {
        try {
            QMPClient client = new QMPClient("localhost", 4444);
            
            client.sendCommand(new JSONObject().put("execute", "query-status"));
            System.out.println("Response: " + client.receiveResponse());
            com.vectras.vm.logger.VectrasStatus.logError("<font color='yellow'>VTERM: >" + "Response: " + client.receiveResponse() + "</font>");
            
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}