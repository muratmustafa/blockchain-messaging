package sample;

import dao.pbft.MsgType;
import dao.pbft.PbftMsg;
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


public class User extends Thread implements Serializable {
	private static final long serialVersionUID = 1L;

	Hashtable<String, PublicKey> publicKeys = new Hashtable<>();

	String userName;
	int port;
	private final PrivateKey privateKey;
	Controller controller;
	public PublicKey publicKey;
	public BlockChain blockChain;

	public User(String userName, int port, Controller controller) throws NoSuchAlgorithmException {
		this.userName = userName;
		this.port = port;
		this.controller = controller;
		blockChain = new BlockChain(3);

		KeyPair keyPair = RSA_ALgos.buildKeyPair();
		this.privateKey = keyPair.getPrivate();
		this.publicKey = keyPair.getPublic();
	}

	public void broadcastPublicKey() throws IOException {
		String pubKey = SerializeObject.serializeObject(publicKey);
		controller.setLOG(pubKey);
		broadCastMessage("NEWUSER," + userName + "," + pubKey);
	}

	@Override
	public void run() {
		recieve(port);
	}

	void createMessage(String plainText, String receiverName) throws Exception {
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

		PbftMsg msg = new PbftMsg(MsgType.PRE_PREPARE, 0);
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
					controller.setChat(messages);
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
