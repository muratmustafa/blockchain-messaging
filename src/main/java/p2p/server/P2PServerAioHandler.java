package p2p.server;


import com.alibaba.fastjson.JSON;
import dao.pbft.MsgType;
import dao.pbft.PBFTMsg;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.ServerAioHandler;
import p2p.common.MsgPacket;
import util.MsgUtil;

import java.nio.ByteBuffer;

@Slf4j
public class P2PServerAioHandler implements ServerAioHandler {

    private ServerAction action = ServerAction.getInstance();

    @Override
    public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws AioDecodeException {

        if (readableLength < MsgPacket.HEADER_LENGHT) {
            return null;
        }

        int bodyLength = buffer.getInt();

        if (bodyLength < 0) {
            throw new AioDecodeException("bodyLength [" + bodyLength + "] is not right, remote:" + channelContext.getClientNode());
        }

        int neededLength = MsgPacket.HEADER_LENGHT + bodyLength;

        if (readableLength < neededLength) {
            return null;
        } else {
            MsgPacket imPacket = new MsgPacket();
            if (bodyLength > 0) {
                byte[] dst = new byte[bodyLength];
                buffer.get(dst);
                imPacket.setBody(dst);
            }
            return imPacket;
        }
    }

    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        MsgPacket msgPacket = (MsgPacket) packet;
        byte[] body = msgPacket.getBody();
        int bodyLen = 0;

        if (body != null) {
            bodyLen = body.length;
        }

        int allLen = MsgPacket.HEADER_LENGHT + bodyLen;

        ByteBuffer buffer = ByteBuffer.allocate(allLen);

        buffer.order(tioConfig.getByteOrder());


        buffer.putInt(bodyLen);


        if (body != null) {
            buffer.put(body);
        }
        return buffer;
    }

    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {

        MsgPacket msgPacket = (MsgPacket) packet;
        byte[] body = msgPacket.getBody();

        if (body == null) {
            return;
        }

        String msg = new String(body, MsgPacket.CHARSET);

        if (!JSON.isValid(msg)) {
            return;
        }

        log.info("Info: " + msg);
        PBFTMsg pbftMsg = JSON.parseObject(msg, PBFTMsg.class);
        if (pbftMsg == null) {
            log.error("Error");
            return;
        }

        if ((pbftMsg.getMsgType() != MsgType.CLIENT_REPLAY && pbftMsg.getMsgType() != MsgType.GET_VIEW) && !MsgUtil.afterMsg(pbftMsg)) {
            log.warn("Warning");
            return;
        }

        action.doAction(channelContext, pbftMsg);
    }
}
