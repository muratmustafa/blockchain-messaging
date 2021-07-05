package p2p.client;

import com.alibaba.fastjson.JSON;
import dao.pbft.MsgCollection;
import dao.pbft.MsgType;
import dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.intf.ClientAioHandler;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;
import p2p.common.MsgPacket;
import util.MsgUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class P2pClientAioHandler implements ClientAioHandler {

    private static MsgPacket heartPacket = new MsgPacket();

    private BlockingQueue<PbftMsg> msgQueue = MsgCollection.getInstance().getMsgQueue();

    @Override
    public Packet heartbeatPacket(ChannelContext channelContext) {
        return heartPacket;
    }

    @Override
    public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws AioDecodeException {


        if (readableLength < MsgPacket.HEADER_LENGHT) {
            return null;
        }

        int bodyLength = buffer.getInt();
        if (bodyLength < 0) {
            throw new AioDecodeException("body length is invalid.romote: " + channelContext.getServerNode());
        }

        int usefulLength = MsgPacket.HEADER_LENGHT + bodyLength;

        if (usefulLength > readableLength) {
            return null;
        } else {
            MsgPacket packet = new MsgPacket();
            byte[] body = new byte[bodyLength];
            buffer.get(body);
            packet.setBody(body);
            return packet;
        }

    }

    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        MsgPacket msgPacket = (MsgPacket) packet;
        byte[] body = msgPacket.getBody();

        int bodyLength = 0;

        if (body != null) {
            bodyLength = body.length;
        }

        int len = MsgPacket.HEADER_LENGHT + bodyLength;

        ByteBuffer byteBuffer = ByteBuffer.allocate(len);
        byteBuffer.order(tioConfig.getByteOrder());
        byteBuffer.putInt(bodyLength);

        if (body != null) {
            byteBuffer.put(body);
        }
        return byteBuffer;
    }

    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        MsgPacket msgPacket = (MsgPacket) packet;
        byte[] body = msgPacket.getBody();
        // 空的很可能为心跳包
        if (body == null) {
            return;
        }

        String msg = new String(body, MsgPacket.CHARSET);
        // 如果数据不是json数据，代表数据有问题
        if (!JSON.isValid(msg)) {
            return;
        }
        PbftMsg pbftMsg = JSON.parseObject(msg, PbftMsg.class);
        if (pbftMsg == null) {
            log.error("客户端将Json数据解析成pbft数据失败");
            return;
        }

        if (pbftMsg.getMsgType() != MsgType.GET_VIEW && !MsgUtil.afterMsg(pbftMsg)) {
            log.warn("数据检查签名或者解密失败");
            return;
        }
        this.msgQueue.put(pbftMsg);
    }
}
