package dao.node;

import com.alibaba.fastjson.JSON;
import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import sample.*;
import util.MsgUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

@Data
@Slf4j
public class Miner extends Node {
	private static final long serialVersionUID = 1L;

	public Miner() throws NoSuchAlgorithmException {
		super();
	}

	public void broadcastEverything() throws Exception {
		String blockChainData = SerializeObject.serializeObject(blockChain);
		String message = "BLOCKCHAIN," + blockChainData;
		broadCastMessage(message);
	}

	void broadCastMessage(String m) throws IOException {		
		Broadcast.broadcast(m, Network.availableInterfaces().get(0), port);
	}

	@Override
	public void recieve(int port) {
		try {
			@SuppressWarnings("resource")
			DatagramSocket serverSocket = new DatagramSocket(port);
			byte[] receiveData = new byte[65507];

			System.out.printf("Listening on udp:%s:%d%n", InetAddress.getLocalHost().getHostAddress(), port);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			while(true){
				serverSocket.receive(receivePacket);
				String packet = new String( receivePacket.getData(), 0, receivePacket.getLength() );
				System.out.println("\nRECEIVED --> " + packet);

				PBFTMsg pbftMsg = JSON.parseObject(packet, PBFTMsg.class);

				if(pbftMsg.getMsgType() == MsgType.MESSAGE) {
					Message m = (Message) SerializeObject.deserializeObject(pbftMsg.getBody());
					blockChain.addMessage(m);
					broadcastEverything();
				}else if(pbftMsg.getMsgType() == MsgType.NEW_USER) {
					String newUserName = pbftMsg.getUserName();
					PublicKey newPublicKey = (PublicKey) SerializeObject.deserializeObject(pbftMsg.getBody());
					if(publicKeyMap.containsKey(newUserName)) {
						broadCastMessage("DENIED NEW USER: " + newUserName);
					}
					else {
						publicKeyMap.put(newUserName, newPublicKey);
						broadcastAllPublicKeys();
					}
				}
			}
		} catch (IOException e) {
			System.out.println(e);
		} catch (Exception e) {
			e.printStackTrace();
		}   
	}

	private void broadcastAllPublicKeys() throws IOException {
		String hashtableData = SerializeObject.serializeObject((Serializable) publicKeyMap);
		//String message = "HASHTABLE," + hashtableData;

		PBFTMsg message = new PBFTMsg(MsgType.HASHTABLE, -1);

		message.setBody(hashtableData);

		broadCastMessage(JSON.toJSONString(message));
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
