package dao.bean;

import lombok.Data;

@Data
public class ReplayJson {
    private int index;
    private String ip;
    private int port;
    private String publicKey;
}
