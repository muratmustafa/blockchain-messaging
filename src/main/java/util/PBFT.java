package util;

import config.AllNodeCommonMsg;
import dao.node.Node;
import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class PBFT {

    private Node node = Node.getInstance();

    public boolean pubView() throws IOException {
        if (AllNodeCommonMsg.allNodeAddressMap.size() < 3) {
            log.warn("The nodes in the blockchain are less than or equal to 3");
            node.setViewOK(true);

            node.broadcastPublicKey();


            //ClientUtil.publishIpPort(node.getIndex(), node.getAddress().getIp(), node.getAddress().getPort());
            return true;
        }

        PBFTMsg view = new PBFTMsg(MsgType.GET_VIEW, node.getIndex());
        ClientUtil.clientPublish(view);
        return true;
    }

    public boolean changeView() {
        return true;
    }
}
