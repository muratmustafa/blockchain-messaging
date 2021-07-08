package p2p.server;

import com.alibaba.fastjson.JSON;
import config.AllNodeCommonMsg;
import dao.bean.ReplayJson;
import dao.node.Node;
import dao.node.NodeAddress;
import dao.node.NodeBasicInfo;
import dao.pbft.MsgCollection;
import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.extern.slf4j.Slf4j;

import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import p2p.client.ClientAction;
import p2p.common.MsgPacket;
import util.ClientUtil;
import util.MsgUtil;

import util.PBFTUtil;

import java.io.UnsupportedEncodingException;

@Slf4j
public class ServerAction {
    private final Node node = Node.getInstance();
    private final MsgCollection msgCollection = MsgCollection.getInstance();

    private static final ServerAction action = new ServerAction();

    private final MsgCollection collection = MsgCollection.getInstance();

    public static ServerAction getInstance() {
        return action;
    }

    private ServerAction() {

    }

    public void doAction(ChannelContext channelContext, PBFTMsg msg) {
        switch (msg.getMsgType()) {
            case MsgType.GET_VIEW:
                onGetView(channelContext, msg);
                break;
            case MsgType.CHANGE_VIEW:
                changeView(channelContext, msg);
                break;
            case MsgType.PRE_PREPARE:
                prePrepare(msg);
                break;
            case MsgType.PREPARE:
                prepare(msg);
                break;
            case MsgType.COMMIT:
                commit(msg);
            case MsgType.CLIENT_REPLAY:
                addClient(msg);
                break;
            default:
                break;
        }
    }

    private void commit(PBFTMsg msg) {

        long count = collection.getAgreeCommit().incrementAndGet(msg.getId());

        log.info(String.format("server commit：%s", msg));
        if (count >= AllNodeCommonMsg.getAgreeNum()) {
            log.info("commit");
            collection.getAgreeCommit().remove(msg.getId());
            PBFTUtil.save(msg);
        }
    }

    private void prepare(PBFTMsg msg) {
        log.info(msgCollection.getVotePrePrepare().contains(msg) + ">>>>");
        if (!msgCollection.getVotePrePrepare().contains(msg.getId()) || !PBFTUtil.checkMsg(msg)) {
            return;
        }

        long count = collection.getAgreePrepare().incrementAndGet(msg.getId());
        log.info(String.format("server prepare：%s", msg));
        if (count >= AllNodeCommonMsg.getAgreeNum()) {
            log.info("commit");
            collection.getVotePrePrepare().remove(msg.getId());
            collection.getAgreePrepare().remove(msg.getId());

            msg.setMsgType(MsgType.COMMIT);
            try {
                collection.getMsgQueue().put(msg);
                ClientAction.getInstance().doAction(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void prePrepare(PBFTMsg msg) {

        log.info(String.format("server pre-prepare：%s", msg));

        msgCollection.getVotePrePrepare().add(msg.getId());
        if (!PBFTUtil.checkMsg(msg)) {
            return;
        }

        msg.setMsgType(MsgType.PREPARE);
        try {
            msgCollection.getMsgQueue().put(msg);
            ClientAction.getInstance().doAction(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

    private void changeView(ChannelContext channelContext, PBFTMsg msg) {
        if (node.isViewOK()) {
            return;
        }
        long count = collection.getViewNumCount().incrementAndGet(msg.getViewNum());

        if (count >= AllNodeCommonMsg.getAgreeNum() && !node.isViewOK()) {
            collection.getViewNumCount().clear();
            node.setViewOK(true);
            AllNodeCommonMsg.view = msg.getViewNum();
            log.info("OK");
        }
    }

    private void addClient(PBFTMsg msg) {
        if (!ClientUtil.haveClient(msg.getNode())) {
            String ipStr = msg.getBody();
            ReplayJson replayJson = JSON.parseObject(ipStr, ReplayJson.class);
            //ClientChannelContext context = ClientUtil.clientConnect(replayJson.getIp(), replayJson.getPort());

            NodeAddress address = new NodeAddress();
            //address.setIp(replayJson.getIp());
            //address.setPort(replayJson.getPort());
            NodeBasicInfo info = new NodeBasicInfo();
            info.setIndex(msg.getNode());
            info.setAddress(address);

            AllNodeCommonMsg.allNodeAddressMap.put(msg.getNode(), info);
            AllNodeCommonMsg.publicKeyMap.put(msg.getNode(), replayJson.getPublicKey());

            log.info(String.format("%s：%s", node, info));
            /*if (context != null) {
                ClientUtil.addClient(msg.getNode(), context);
            }*/
        }
    }

    private void onGetView(ChannelContext channelContext, PBFTMsg msg) {
        int fromNode = msg.getNode();
        msg.setNode(node.getIndex());
        msg.setToNode(fromNode);
        log.info(String.format("%s", msg));
        msg.setOk(true);
        msg.setViewNum(AllNodeCommonMsg.view);
        MsgUtil.signMsg(msg);
        String jsonView = JSON.toJSONString(msg);
        MsgPacket msgPacket = new MsgPacket();
        try {
            msgPacket.setBody(jsonView.getBytes(MsgPacket.CHARSET));
            Tio.send(channelContext, msgPacket);
        } catch (UnsupportedEncodingException e) {
            log.error(String.format("%s", e.getMessage()));
        }
    }
}
