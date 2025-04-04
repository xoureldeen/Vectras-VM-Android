package com.vectras.vm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class IOApplication {
    public static boolean isPortOpen(String host, int port, int timeout) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or refused connection.
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Handle close exception, or ignore.
            }
        }
    }
}
