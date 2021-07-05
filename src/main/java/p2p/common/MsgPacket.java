package p2p.common;

import org.tio.core.intf.Packet;

public class MsgPacket extends Packet {

    private static final long serialVersionUID = -172060606924066412L;

    public static final int HEADER_LENGHT = 4;
    public static final String CHARSET = "utf-8";
    private byte[] body;

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
