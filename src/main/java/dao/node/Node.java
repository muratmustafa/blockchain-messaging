package dao.node;

import cn.hutool.crypto.asymmetric.RSA;
import lombok.Data;

@Data
public class Node extends NodeBasicInfo {

    private static Node node = new Node();

    public static Node getInstance() {
        return node;
    }

    private Node() {
        RSA rsa = new RSA();
        this.setPrivateKey(rsa.getPrivateKeyBase64());
        this.setPublicKey(rsa.getPublicKeyBase64());
    }

    private boolean isRun = false;

    private volatile boolean viewOK;

    private String publicKey;
    private String privateKey;
}
