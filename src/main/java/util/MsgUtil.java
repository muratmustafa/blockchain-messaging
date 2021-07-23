package util;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import config.AllNodeCommonMsg;
import dao.node.Node;
import dao.pbft.PBFTMsg;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.Map;

@Slf4j
public class MsgUtil {

    private static RSA selfRsa = new RSA(Node.getInstance().getPrivateKey(), Node.getInstance().getPublicKey());

    private static final Map<String, PublicKey> publicKeyMap = AllNodeCommonMsg.publicKeyMap;

    public static void signMsg(PBFTMsg msg) {
        String hash = String.valueOf(msg.hashCode());
        String sign = selfRsa.encryptBase64(hash, KeyType.PrivateKey);
        msg.setSign(sign);
    }

    private static boolean encryptMsg(PBFTMsg msg) {
        PublicKey publicKey;
        if ((publicKey = publicKeyMap.get(msg.getReceiverName())) == null) {
            log.error("Error");
            return false;
        }

        if (msg.getBody() == null) {
            log.warn("Warning");
            return true;
        }

        RSA encryptRsa;
        try {
            encryptRsa = new RSA(null, publicKey);
        } catch (Exception e) {
            log.error("RSA Error");
            return false;
        }

        msg.setBody(encryptRsa.encryptBase64(msg.getBody(), KeyType.PublicKey));
        return true;
    }

    private static boolean decryptMsg(PBFTMsg msg) {
        if (msg.getBody() == null) {
            log.warn("Warning");
            return true;
        }
        String body;
        try {
            body = selfRsa.decryptStr(msg.getBody(), KeyType.PrivateKey);
        } catch (Exception e) {
            log.error(String.format("%s", e.getMessage()));
            return false;
        }
        msg.setBody(body);
        return true;
    }

    public static boolean preMsg(PBFTMsg msg) {
        if (!encryptMsg(msg)) {
            return false;
        }
        signMsg(msg);
        return true;
    }

    public static boolean afterMsg(PBFTMsg msg) {
        if (!isRealMsg(msg) || !decryptMsg(msg)) {
            return false;
        }
        return true;
    }

    public static boolean isRealMsg(PBFTMsg msg) {
        PublicKey publicKey = publicKeyMap.get(msg.getUserName());
        RSA rsa;
        try {
            rsa = new RSA(null, publicKey);
        } catch (Exception e) {
            log.error(String.format("Error: %s", e.getMessage()));
            return false;
        }

        String nowHash = String.valueOf(msg.hashCode());

        String sign = msg.getSign();
        try {
            String hash = rsa.decryptStr(sign, KeyType.PublicKey);
            if (nowHash.equals(hash)) {
                return true;
            }
        } catch (Exception e) {
            log.warn(String.format("Warning: %s", e.getMessage()));
        }
        return false;
    }

}
