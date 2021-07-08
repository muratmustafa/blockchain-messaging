package config;

import cn.hutool.core.io.file.FileReader;
import com.alibaba.fastjson.JSON;
import dao.bean.ReplayJson;
import dao.node.Node;
import dao.node.NodeAddress;
import dao.node.NodeBasicInfo;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.ClientChannelContext;
import org.tio.server.ServerTioConfig;
import org.tio.server.TioServer;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;
import p2p.P2PConnectionMsg;
import p2p.common.Const;
import p2p.server.P2PServerAioHandler;
import p2p.server.ServerListener;
import util.ClientUtil;
import util.PBFTUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class StartConfig {
    private Node node = Node.getInstance();
    public static String basePath;

    public boolean startConfig() {
        return true;
    }

    /*private boolean initClient() {
        P2PConnectionMsg.CLIENTS = new ConcurrentHashMap<>(AllNodeCommonMsg.allNodeAddressMap.size());

        for (NodeBasicInfo basicInfo : AllNodeCommonMsg.allNodeAddressMap.values()) {
            NodeAddress address = basicInfo.getAddress();
            log.info(String.format("%s", node.getIndex(), basicInfo));
            ClientChannelContext context = ClientUtil.clientConnect(address.getIp(), address.getPort());
            if (context != null) {
                ClientUtil.addClient(basicInfo.getIndex(), context);
            } else {
                log.warn(String.format("%d-->%s", basicInfo.getIndex(), address.getIp(), address.getPort()));
            }
        }
        log.info("Client" + P2PConnectionMsg.CLIENTS.size());
        return true;
    }*/


    private boolean initServer() {

        ServerAioHandler handler = new P2PServerAioHandler();

        ServerAioListener listener = new ServerListener();

        ServerTioConfig config = new ServerTioConfig("Server", handler, listener);

        config.setHeartbeatTimeout(Const.TIMEOUT);
        TioServer tioServer = new TioServer(config);
        //tioServer.start(node.getAddress().getIp(), node.getAddress().getPort());
        return true;
    }


    private boolean initAddress() {
        FileReader fileReader = new FileReader(PBFTUtil.ipJsonPath);
        List<String> ipJsonStr = fileReader.readLines();
        for (String s : ipJsonStr) {
            ReplayJson replayJson = JSON.parseObject(s, ReplayJson.class);
            NodeAddress nodeAddress = new NodeAddress();
            //nodeAddress.setIp(replayJson.getIp());
            //nodeAddress.setPort(replayJson.getPort());
            NodeBasicInfo nodeBasicInfo = new NodeBasicInfo();
            nodeBasicInfo.setAddress(nodeAddress);
            nodeBasicInfo.setIndex(replayJson.getIndex());
            AllNodeCommonMsg.allNodeAddressMap.put(replayJson.getIndex(), nodeBasicInfo);
            AllNodeCommonMsg.publicKeyMap.put(replayJson.getIndex(), replayJson.getPublicKey());
        }

        if (AllNodeCommonMsg.allNodeAddressMap.values().size() < 3 && !AllNodeCommonMsg.allNodeAddressMap.containsKey(node.getIndex())) {
            PBFTUtil.writeIpToFile(node);
            return true;
        }
        if (AllNodeCommonMsg.allNodeAddressMap.containsKey(node.getIndex())) {
            log.error("Error");
            return false;
        }
        log.info(String.format("ip.json %s", ipJsonStr.size()));
        log.info(String.format("%s", AllNodeCommonMsg.allNodeAddressMap.values().size()));
        return AllNodeCommonMsg.allNodeAddressMap.values().size() == ipJsonStr.size();
    }
}
