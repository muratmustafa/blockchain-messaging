package util;

import cn.hutool.core.io.file.FileWriter;
import cn.hutool.json.JSONUtil;
import dao.bean.ReplayJson;
import dao.node.Node;
import dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PbftUtil {

    private static boolean flag = true;

    public static String ipJsonPath = "" + "ip.json";

    public static boolean checkMsg(PbftMsg msg) {

        return true;
    }

    public static void save(PbftMsg msg) {

    }

    synchronized public static void writeIpToFile(Node node) {
        if (!flag) {
            return;
        }
        log.info(String.format("%s", node.getIndex()));
        FileWriter writer = new FileWriter(ipJsonPath);
        ReplayJson replayJson = new ReplayJson();
        replayJson.setIndex(node.getIndex());
        replayJson.setIp(node.getAddress().getIp());
        replayJson.setPort(node.getAddress().getPort());
        replayJson.setPublicKey(node.getPublicKey());
        String json = JSONUtil.toJsonStr(replayJson);
        writer.append(json + "\n");
        flag = false;
    }

}
