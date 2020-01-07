package edu.berkeley.cs162.test;


import edu.berkeley.cs162.KVMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Yongbing Hu
 * @version 0.0.0
 * @time 2019-12-26 11:16 a.m.
 * @description
 */
public class TestServer {
    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            Socket client = serverSocket.accept();
            ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
            Object o = objectInputStream.readObject();
            KVMessage kv = (KVMessage)o;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
}
