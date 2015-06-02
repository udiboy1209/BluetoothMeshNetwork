package com.learning.sprihabiswas.bluetooth_trial2;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Packet {
    String content;
    String id;
    String sender;

    public Packet(String ctnt, String sndr){
        content=ctnt;
        sender=sndr;

        try {
            id = System.currentTimeMillis() + content + sender;
            id = new String(MessageDigest.getInstance("SHA1").digest(id.getBytes()));
        } catch (NoSuchAlgorithmException e){

        }
    }

    // {0...255}   -> SHA1 id
    // {256...511} -> sender name
    // {512...}    -> msg data
    public Packet(byte[] data){
        id = new String(data,0,256);
        sender = new String(data, 256, 256).trim();
        content = new String(data, 512, data.length-512);
    }

    public byte[] getBytes(){
        byte[] bytes = new byte[512+content.length()];

        id.getBytes(0,256,bytes,0);
        sender.getBytes(0,sender.length(),bytes,256);
        content.getBytes(0,content.length(),bytes,512);

        return bytes;
    }
}
