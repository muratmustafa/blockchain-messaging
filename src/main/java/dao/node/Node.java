package dao.node;

import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.Data;
import lombok.EqualsAndHashCode;
import p2p.common.Const;
import sample.*;
import util.ClientUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Scanner;

@EqualsAndHashCode(callSuper = true)
@Data
public class Node extends Thread implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Hashtable<String, PublicKey> publicKeys = new Hashtable<>();

    private boolean isRun = false;
    private volatile boolean viewOK;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    private String userName;
    private int index;
    protected int port = Const.PORT;

    public BlockChain blockChain;

    private static Node node;

    static {
        try {
            node = new Node();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static Node getInstance() {
        return node;
    }

    protected Node() throws NoSuchAlgorithmException {
        KeyPair keyPair = RSA_ALgos.buildKeyPair();

        this.setPrivateKey(keyPair.getPrivate());
        this.setPublicKey(keyPair.getPublic());

        this.blockChain = new BlockChain(3);
    }

    public void broadcastPublicKey() throws IOException {
        String pubKey = SerializeObject.serializeObject(publicKey);
        Main.controllerHandle.setLOG(pubKey);
        broadCastMessage("NEWUSER," + userName + "," + pubKey);
    }

    @Override
    public void run() {
        recieve(port);
    }

    public void createMessage(String plainText, String receiverName) throws Exception {
        Date createTimestamp = new Date();
        String plainMsg = "Sender    : " + userName + "\n"
                        + "Body      : " + plainText + "\n"
                        + "Timestamp : " + createTimestamp;

        PublicKey receiverKey = getUserPublicKey(receiverName);
        if (receiverKey == null) {
            System.out.println("RECEIVER " + receiverName + " DOES NOT EXIST");
            return;
        }
        byte[] cipherText = MessageCodec.encrypt(receiverKey, plainMsg);
        System.out.println(Arrays.toString(cipherText));
        Message m = new Message(cipherText, receiverName);

        PBFTMsg msg = new PBFTMsg(MsgType.PRE_PREPARE, 0);
        msg.setBody(Arrays.toString(cipherText));
        ClientUtil.prePrepare(msg);

        broadCastMessage("MESSAGE," + SerializeObject.serializeObject(m));
    }

    private void broadCastMessage(String m) throws IOException {
        Broadcast.broadcast(m, Network.availableInterfaces().get(0), port);
    }

    String decryptMessage(byte[] cipherText) throws Exception {
        return MessageCodec.decrypt(privateKey, cipherText);
    }

    void printMyMessages() throws Exception {
        StringBuilder myMessages = new StringBuilder();
        String messages = "";
        System.out.println("----------- MY MESSAGES -----------------");
        for (Block b : blockChain.blockChain) {
            for (Message m : b.blockMessages) {
                if (m.receiver.equals(userName)) {
                    System.out.println(decryptMessage(m.cipherText) + "\n");

                    String text = decryptMessage(m.cipherText);

                    Scanner scanner = new Scanner(text);
                    String s = scanner.nextLine();
                    String ms = scanner.nextLine();
                    String d = scanner.nextLine();

                    String sender = s.substring(s.lastIndexOf(":") + 1);
                    String message = ms.substring(ms.lastIndexOf(":") + 1);
                    //String date = d.substring(d.lastIndexOf(":") + 1);

                    //Date timeStamp = new Date(date);

                    //messages += "[" + s + "] :" +  message + " (" + timeStamp.getTime() + ")" + "\n";
                    messages += "[" + sender + "] :" +  message + "\n";

                    // close the scanner
                    scanner.close();

                    myMessages.append(text).append("\n--------------------\n");
                    Main.controllerHandle.setChat(messages);
                }
            }
        }
        System.out.println("-----------------------------------------");
    }

    public PublicKey getUserPublicKey(String receiverName) {
        if (!publicKeys.containsKey(receiverName)) return null;
        return publicKeys.get(receiverName);
    }

    @SuppressWarnings("unchecked")
    public void recieve(int port) {
        try {
            @SuppressWarnings("resource")
            DatagramSocket serverSocket = new DatagramSocket(port);
            byte[] receiveData = new byte[65507];

            System.out.printf("Listening on udp:%s:%d%n", InetAddress.getLocalHost().getHostAddress(), port);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (true) {
                serverSocket.receive(receivePacket);
                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());

                System.out.println("\nRECEIVED --> " + sentence);

                if (sentence.startsWith("BLOCKCHAIN")) {
                    String[] data = sentence.split(",");
                    blockChain = (BlockChain) SerializeObject.deserializeObject(data[1]);
                } else if (sentence.startsWith("HASHTABLE")) {
                    String[] data = sentence.split(",");
                    publicKeys = (Hashtable<String, PublicKey>) SerializeObject.deserializeObject(data[1]);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
