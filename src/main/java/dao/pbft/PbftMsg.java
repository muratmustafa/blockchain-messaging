package dao.pbft;

import cn.hutool.core.util.IdUtil;
import config.AllNodeCommonMsg;
import lombok.Data;

import java.util.Objects;

@Data
public class PbftMsg {

    private int msgType;

    private int node;

    private int toNode;

    private long time;

    private String body;

    private boolean isOk;

    private int viewNum;

    private String id;

    private String sign;

    private PbftMsg() {
    }

    public PbftMsg(int msgType, int node) {
        this.msgType = msgType;
        this.node = node;
        this.time = System.currentTimeMillis();
        this.id = IdUtil.randomUUID();
        this.viewNum = AllNodeCommonMsg.view;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PbftMsg)) {
            return false;
        }
        PbftMsg msg = (PbftMsg) o;
        return  getMsgType() == msg.getMsgType() &&
                getNode() == msg.getNode() &&
                getToNode() == msg.getToNode() &&
                getTime() == msg.getTime() &&
                isOk() == msg.isOk() &&
                getViewNum() == msg.getViewNum() &&
                Objects.equals(getBody(), msg.getBody()) &&
                Objects.equals(getId(), msg.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMsgType(), getBody(), getNode(), getToNode(), getTime(),  getViewNum(), getId());
    }
}
