package edu.berkeley.cs162.test;

import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author Yongbing Hu
 * @version 0.0.0
 * @time 2019-12-26 1:59 p.m.
 * @description
 */
public class TestKVStore {
    public static void main(String[] args) throws KVException {
        Dictionary<String, String> dictionary = new Hashtable<>();
        dictionary.put("k1", "v1");
        dictionary.put("k2", "v2");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream("/Users/huyongbing/Desktop/text.txt",true);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(dictionary);
            objectOutputStream.close();

            FileInputStream fileInputStream = new FileInputStream("/Users/huyongbing/Desktop/text.txt");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object o = objectInputStream.readObject();
            Dictionary cast = dictionary.getClass().cast(o);
            System.out.println(cast.get("k1"));
        } catch (ClassNotFoundException |FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
