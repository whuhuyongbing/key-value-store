package edu.berkeley.cs162.test;

import edu.berkeley.cs162.KVClient;
import edu.berkeley.cs162.KVException;

/**
 * @author Yongbing Hu
 * @version 0.0.0
 * @time 2019-12-26 11:13 a.m.
 * @description
 */
public class TestClient {
    public static void main(String[] args) {
        KVClient kvClient = new KVClient("localhost", 8080);

        try {
            kvClient.put("key","value");
        } catch (KVException e) {
            e.printStackTrace();
        }
    }
}
