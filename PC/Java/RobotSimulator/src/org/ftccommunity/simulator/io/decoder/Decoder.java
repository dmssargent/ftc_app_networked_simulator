package org.ftccommunity.simulator.io.decoder;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.ftccommunity.simulator.net.protocol.SimulatorData;

import java.util.List;


public class Decoder extends ByteToMessageDecoder { // (1)
    @Override
    protected void decode(io.netty.channel.ChannelHandlerContext ctx, ByteBuf in, List<Object> out) { // (2)
        long size = 0;
        if (in.readableBytes() >= 4) {
            size = in.readUnsignedInt();
            // System.out.print(" Size of data: " + size + " Received: " + in.readableBytes());
            if (in.readableBytes() < size) {
                return;
            }
        } else {
            return;
        }
        SimulatorData.Data test;
        try {
            test = SimulatorData.Data.parseFrom(in.readBytes((int) size).array());
        } catch (InvalidProtocolBufferException e) {
            return;
        }
        if (test != null) {
            out.add(test);
        }
    }
}

