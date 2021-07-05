package dao.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class DbDao implements Serializable {

    private long time;

    private int node;

    private String publicKey;

    private int viewNum;
}
