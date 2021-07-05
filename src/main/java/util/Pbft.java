package util;

import config.AllNodeCommonMsg;
import dao.node.Node;
import dao.pbft.MsgType;
import dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Pbft {

    private Node node = Node.getInstance();

    public boolean pubView() {
        if (AllNodeCommonMsg.allNodeAddressMap.size() < 3) {
            log.warn("区块链中的节点小于等于3");
            node.setViewOK(true);
            ClientUtil.publishIpPort(node.getIndex(), node.getAddress().getIp(), node.getAddress().getPort());
            return true;
        }

        PbftMsg view = new PbftMsg(MsgType.GET_VIEW, node.getIndex());
        ClientUtil.clientPublish(view);
        return true;
    }

    public boolean changeView() {
        return true;
    }
}
