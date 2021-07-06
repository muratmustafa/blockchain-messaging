package util;

import com.alibaba.fastjson.JSON;
import config.AllNodeCommonMsg;
import dao.bean.ReplayJson;
import dao.node.Node;
import dao.pbft.MsgCollection;
import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientTioConfig;
import org.tio.client.ReconnConf;
import org.tio.core.Tio;
import p2p.P2PConnectionMsg;
import p2p.client.ClientAction;
import p2p.client.P2PClientLinstener;
import p2p.client.P2pClientAioHandler;
import p2p.common.Const;
import p2p.common.MsgPacket;
import sample.Broadcast;
import sample.Network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Slf4j
public class ClientUtil {

    private static ClientTioConfig clientTioConfig = new ClientTioConfig(
            new P2pClientAioHandler(),
            new P2PClientLinstener(),
            new ReconnConf(Const.TIMEOUT)
    );

    /*public static ClientChannelContext clientConnect(String ip, int port) {

        clientTioConfig.setHeartbeatTimeout(Const.TIMEOUT);
        ClientChannelContext context;
        try {
            TioClient client = new TioClient(clientTioConfig);
            context = client.connect(new org.tio.core.Node(ip, port), Const.TIMEOUT);
            return context;
        } catch (Exception e) {
            log.error("%s：%d" + e.getMessage());
            return null;
        }
    }*/

    public static void addClient(int index, ClientChannelContext client) {
        P2PConnectionMsg.CLIENTS.put(index, client);
    }

    public static boolean haveClient(int index) {
        if (P2PConnectionMsg.CLIENTS.containsKey(index)) {
            return true;
        } else {
            return false;
        }
    }

    public static void clientPublish(PBFTMsg msg) throws IOException {
        msg.setNode(Node.getInstance().getIndex());
        msg.setToNode(-1);

        if (msg.getMsgType() != MsgType.CLIENT_REPLAY && msg.getMsgType() != MsgType.GET_VIEW) {
            if (!MsgUtil.preMsg(0, msg)) {
                log.error("Error");
                return;
            }
        }

        String json = JSON.toJSONString(msg);

        Broadcast.broadcast(json, Network.availableInterfaces().get(0), Const.PORT);

        for (int index : P2PConnectionMsg.CLIENTS.keySet()) {
            ClientChannelContext client = P2PConnectionMsg.CLIENTS.get(index);

            if (msg.getMsgType() != MsgType.CLIENT_REPLAY && msg.getMsgType() != MsgType.GET_VIEW) {
                if (!MsgUtil.preMsg(index, msg)) {
                    log.error("Error");
                    return;
                }
            }
            //String json = JSON.toJSONString(msg);

            Broadcast.broadcast(json, Network.availableInterfaces().get(0), Const.PORT);


            MsgPacket msgPacket = new MsgPacket();
            try {
                msgPacket.setBody(json.getBytes(MsgPacket.CHARSET));
                Tio.send(client, msgPacket);
            } catch (UnsupportedEncodingException e) {
                log.error("Error: " + e.getMessage());
            }
        }
    }

    public static void publishIpPort(int index, String ip, int port) throws IOException {
        PBFTMsg replayMsg = new PBFTMsg(MsgType.CLIENT_REPLAY, index);
        replayMsg.setViewNum(AllNodeCommonMsg.view);

        ReplayJson replayJson = new ReplayJson();
        replayJson.setIp(ip);
        replayJson.setPort(port);

        //replayJson.setPublicKey(Node.getInstance().getPublicKey());

        replayMsg.setBody(JSON.toJSONString(replayJson));
        ClientUtil.clientPublish(replayMsg);
        log.info(String.format("%s", replayMsg));
    }

    public static void prePrepare(PBFTMsg msg) throws IOException {

        Node node = Node.getInstance();

        msg.setNode(node.getIndex());
        msg.setToNode(-1);
        msg.setViewNum(AllNodeCommonMsg.view);

        if (node.getIndex() != AllNodeCommonMsg.getPriIndex()) {
            return;
        }
        msg.setMsgType(MsgType.PRE_PREPARE);
        ClientUtil.clientPublish(msg);

        MsgCollection msgCollection = MsgCollection.getInstance();
        msg.setMsgType(MsgType.PREPARE);
        msgCollection.getVotePrePrepare().add(msg.getId());
        if (!PBFTUtil.checkMsg(msg)) {
            return;
        }
        try {
            msgCollection.getMsgQueue().put(msg);
            ClientAction.getInstance().doAction(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }
}
