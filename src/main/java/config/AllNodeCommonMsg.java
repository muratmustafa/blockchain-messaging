package config;

import dao.node.NodeBasicInfo;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AllNodeCommonMsg {

    public static int getMaxf() {
        return (getSize() - 1) / 3;
    }

    public static int getAgreeNum(){
        return 2 * AllNodeCommonMsg.getMaxf() + 1;
    }

    public static int getPriIndex() {
        return view % getSize();
    }

    public static ConcurrentHashMap<Integer, NodeBasicInfo> allNodeAddressMap = new ConcurrentHashMap<>(100);

    public static Map<String, PublicKey> publicKeyMap = new ConcurrentHashMap<>(2<<10);

    public volatile static int view = 0;

    public static int getSize() {
        return allNodeAddressMap.size() + 1;
    }
}
