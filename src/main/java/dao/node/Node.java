package dao.node;

import com.alibaba.fastjson.JSON;
import config.AllNodeCommonMsg;
import dao.bean.ReplayJson;
import dao.pbft.MsgCollection;
import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import p2p.client.ClientAction;
import p2p.common.Const;
import p2p.common.MsgPacket;
import sample.*;
import util.ClientUtil;
import util.MsgUtil;
import util.PBFT;
import util.PBFTUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.BlockingQueue;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class Node extends Thread implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static Map<String, PublicKey> publicKeyMap = AllNodeCommonMsg.publicKeyMap;

    private boolean isRun = false;
    private volatile boolean viewOK;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    private String userName;
    private int index;
    protected int port = Const.PORT;

    public BlockChain blockChain;

    private BlockingQueue<PBFTMsg> msgQueue = MsgCollection.getInstance().getMsgQueue();

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

        PBFTMsg msg = new PBFTMsg(MsgType.NEW_USER, index);

        String pubKey = SerializeObject.serializeObject(publicKey);
        Main.controllerHandle.setLOG(pubKey);

        msg.setBody(pubKey);
        msg.setUserName(userName);

        broadCastMessage(msg);
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

        /*
        PublicKey receiverKey = getUserPublicKey(receiverName);
        if (receiverKey == null) {
            System.out.println("RECEIVER " + receiverName + " DOES NOT EXIST");
            return;
        }
        byte[] cipherText = MessageCodec.encrypt(receiverKey, plainMsg);
        System.out.println(Arrays.toString(cipherText));
        Message m = new Message(cipherText, receiverName);
        */
        PBFTMsg msg = new PBFTMsg(MsgType.PRE_PREPARE, 0);
        msg.setBody(plainMsg);
        msg.setReceiverName(receiverName);
        //ClientUtil.prePrepare(msg);

        msg.setNode(Node.getInstance().getIndex());
        msg.setToNode(-1);
        msg.setViewNum(AllNodeCommonMsg.view);

        if(this.index != AllNodeCommonMsg.getPriIndex()){
            log.warn("The node is not the primary node and cannot send pre-prepare messages");
            return;
        }

        msg.setMsgType(MsgType.PRE_PREPARE);
        broadCastMessage(msg);

        //broadCastMessage("MESSAGE," + SerializeObject.serializeObject(m));
    }

    private void broadCastMessage(PBFTMsg msg) throws IOException {


        if (msg.getMsgType() != MsgType.NEW_USER && msg.getMsgType() != MsgType.GET_VIEW) {
            if (!MsgUtil.preMsg(msg)) {
                log.error("Message encryption failed");
                return;
            }
        }

        String json = JSON.toJSONString(msg);
        Broadcast.broadcast(json, Network.availableInterfaces().get(0), port);
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

    /*public PublicKey getUserPublicKey(String receiverName) {
        if (!publicKeys.containsKey(receiverName)) return null;
        return publicKeys.get(receiverName);
    }*/

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
                String packet = new String(receivePacket.getData(), 0, receivePacket.getLength());

                System.out.println("\nRECEIVED --> " + packet);

                PBFTMsg pbftMsg = JSON.parseObject(packet, PBFTMsg.class);

                if (pbftMsg.getMsgType() == MsgType.BLOCKCHAIN) {
                    blockChain = (BlockChain) SerializeObject.deserializeObject(pbftMsg.getBody());
                } else if (pbftMsg.getMsgType() == MsgType.HASHTABLE) {
                    publicKeyMap = (Map<String, PublicKey>) SerializeObject.deserializeObject(pbftMsg.getBody());
                }else{
                    handler(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doAction(PBFTMsg msg){
        switch (msg.getMsgType()) {
            case MsgType.GET_VIEW:
                getView();
                break;
            case MsgType.CHANGE_VIEW:
                changeView();
                break;
            case MsgType.PRE_PREPARE:
                prePrepare();
                break;
            case MsgType.PREPARE:
                prepare();
                break;
            case MsgType.COMMIT:
                commit();
            case MsgType.CLIENT_REPLAY:
                addClient();
                break;
            default:
                break;
        }
    }

    private void addClient() {

    }

    private void getView() {

    }

    private void prePrepare() {

    }

    private void prepare(){

    }

    private void commit(){

    }

    private void changeView(){

    }

    public void handler(String packet) throws Exception {
        if (!JSON.isValid(packet)) {
            return;
        }
        PBFTMsg pbftMsg = JSON.parseObject(packet, PBFTMsg.class);
        if (pbftMsg == null) {
            log.error("Error");
            return;
        }

        if (pbftMsg.getMsgType() != MsgType.GET_VIEW && !MsgUtil.afterMsg(pbftMsg)) {
            log.warn("Warning");
            return;
        }
        //this.msgQueue.put(pbftMsg);
        doAction(pbftMsg);
    }
}
