package p2p.client;

import config.AllNodeCommonMsg;
import dao.node.Node;
import dao.pbft.MsgCollection;
import dao.pbft.MsgType;
import dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import util.ClientUtil;
import util.MsgUtil;
import util.PbftUtil;

@Slf4j
public class ClientAction {
    private MsgCollection collection = MsgCollection.getInstance();
    private Node node = Node.getInstance();

    private static ClientAction action = new ClientAction();

    public static ClientAction getInstance() {
        return action;
    }

    private ClientAction() {
    }

    public void doAction(ChannelContext channelContext) {
        try {
            PbftMsg msg = collection.getMsgQueue().take();

            if (!node.isViewOK() && msg.getMsgType() != MsgType.GET_VIEW && msg.getMsgType() != MsgType.CHANGE_VIEW) {
                collection.getMsgQueue().put(msg);
                return;
            }
            switch (msg.getMsgType()) {
                case MsgType.GET_VIEW:
                    getView(msg);
                    break;
                case MsgType.CHANGE_VIEW:
                    onChangeView(msg);
                    break;
                case MsgType.PREPARE:
                    prepare(msg);
                    break;
                case MsgType.COMMIT:
                    commit(msg);
                default:
                    break;
            }
        } catch (InterruptedException e) {
            log.debug(String.format("%s", e.getMessage()));
        }
    }

    private void commit(PbftMsg msg) {
        ClientUtil.clientPublish(msg);
    }

    private void prepare(PbftMsg msg) {
        ClientUtil.clientPublish(msg);
    }

    private void onChangeView(PbftMsg msg) {
        int viewNum = AllNodeCommonMsg.view + 1;
        msg.setViewNum(viewNum);
        ClientUtil.clientPublish(msg);
    }

    synchronized private void getView(PbftMsg msg) {
        int fromNode = msg.getNode();
        if (node.isViewOK()) {
            return;
        }

        if (!MsgUtil.isRealMsg(msg) || !msg.isOk()) {
            long count = collection.getDisagreeViewNum().incrementAndGet();
            if (count >= AllNodeCommonMsg.getMaxf()) {
                System.exit(0);
            }
            return;
        }

        long count = collection.getViewNumCount().incrementAndGet(msg.getViewNum());
        if (count >= AllNodeCommonMsg.getAgreeNum() && !node.isViewOK()) {
            collection.getViewNumCount().clear();

            node.setViewOK(true);
            AllNodeCommonMsg.view = msg.getViewNum();

            PbftUtil.writeIpToFile(node);
            ClientUtil.publishIpPort(node.getIndex(), node.getAddress().getIp(), node.getAddress().getPort());
        }
    }

}
