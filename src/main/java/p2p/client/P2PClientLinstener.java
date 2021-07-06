package p2p.client;

import config.AllNodeCommonMsg;
import dao.node.Node;
import dao.pbft.MsgCollection;
import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import p2p.P2PConnectionMsg;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class P2PClientLinstener implements ClientAioListener {

    private ClientAction action = ClientAction.getInstance();

    private Node node = Node.getInstance();

    private BlockingQueue<PBFTMsg> msgQueue = MsgCollection.getInstance().getMsgQueue();

    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
        if (isReconnect) {
            log.warn(String.format("%s", channelContext));
        }
        if (isConnected) {
            log.info(String.format("%s", channelContext));
        }else{
            log.warn(String.format("%s %s", node.getIndex(),channelContext.getServerNode()));
        }
    }


    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {

    }

    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {

    }

    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {

    }

    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {
        action.doAction(channelContext);
    }

    @Override
    public void onBeforeClose(final ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
       log.warn(String.format("%s", channelContext));
       P2PConnectionMsg.CLIENTS.values().removeIf(v -> v.equals(channelContext));

       if (channelContext.equals(P2PConnectionMsg.CLIENTS.get(AllNodeCommonMsg.getPriIndex()))){
            node.setViewOK(false);
            PBFTMsg msg = new PBFTMsg(MsgType.CHANGE_VIEW,node.getIndex());
            msgQueue.put(msg);
            action.doAction(channelContext);
       }
    }
}
