package org.ftccommunity.simulator.networking;/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import com.qualcomm.ftccommon.configuration.FtcConfigurationActivity;
import com.qualcomm.robotcore.util.RobotLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Simplistic telnet client. Serves a Protobuf base
 * <p>
 * from https://github.com/netty/netty/blob/netty-4.1.6.Final/example/src/main/java/io/netty/example/telnet/TelnetClient.java
 * on 12/20/2016
 */
public final class TelnetClient implements Runnable {
    private static final String HOST = System.getProperty("host", "127.0.0.1");
    private static final int PORT = 8023;
    private static TelnetClient instance;
    private boolean isRunning = false;

    private FtcConfigurationActivity activity;

    public void run() {
        isRunning = true;
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new TelnetServerInitializer());

            // Start the connection attempt.
            RobotLog.i("[SIM] Starting up on port " + PORT);
            b.bind(PORT).sync().channel().closeFuture().sync();
        } catch (InterruptedException ignored) {

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            isRunning = false;
        }
    }

    public static class SimulatorWriteService implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                while (TelnetServerInitializer.SERVER_HANDLER.context == null) { // wait for a context to show up
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                try {
                    // Server is now active
                    RobotLog.i("[SIM-net] Found client");
                    while (!Thread.currentThread().isInterrupted()) {
                        final ChannelHandlerContext context = TelnetServerInitializer.SERVER_HANDLER.context;
                        final ArrayList<SimulatorConnection.SimulatorDeviceHandle> handles = Collections.list(SimulatorConnection.handles());
                        for (SimulatorConnection.SimulatorDeviceHandle handle : handles) {
                            if (handle.isLocked) continue;

                            if (handle.writeUpdate) {
                                synchronized (handle.lock) {
                                    final String writeMsg = handle.id + ":" + Arrays.toString(handle.writeQueue.backing());
                                    RobotLog.d("[SIM] writing " + writeMsg);
                                    context.write(writeMsg + "\r\n");
                                    handle.writeQueue.empty(); // remove all elements
                                    handle.writeUpdate = false;
                                }
                            }
                        } // for handles

                        // temp test code
                        Thread.sleep(500);
                        context.write("BEAT\r\n");
                        context.flush();
                    }
                } catch (Exception ex) {
                    RobotLog.e("[SIM-net] " + ex.getMessage(), ex);
                }
            }
        }
    }

    public static TelnetClient instance() {
        if (instance == null) {
            instance = new TelnetClient();
        }

        return instance;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Handles a server-side channel.
     */
    @ChannelHandler.Sharable
    public static class TelnetServerHandler extends SimpleChannelInboundHandler<String> {
        private ChannelHandlerContext context;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // Send greeting for a new connection.
            if (context == null) {
                ctx.write("BEGIN FTCSIMP-" + SimulatorConnection.VERSION + "\r\n" +
                        "REQUIRE DEVICEL\r\n");
                ctx.flush();
                context = ctx;
            } else {
                ctx.write("BEGIN FTCSIMP-" + SimulatorConnection.VERSION + "\r\n" +
                        "ERR TOO MANY CLIENTS\r\n" +
                        "DISCON\r\n")
                        .addListener(ChannelFutureListener.CLOSE);
                ctx.flush();
            }
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
            // Generate and write a response.
            String response;
            boolean close = false;
            if (request.isEmpty()) {
                response = "Please type something.\r\n";
            } else if ("DISCON".equals(request)) {
                response = "DISCON\r\n";
                close = true;
            } else if ("RESTART".equals(request)) {
                response = "DISCON\r\n";
            } else {
                response = "RECV '" + request + "'\r\n";
            }

            // We do not need to write a ChannelBuffer here.
            // We know the encoder inserted at TelnetPipelineFactory will do the conversion.
            ChannelFuture future = ctx.write(response);

            // Close the connection after sending 'Have a good day!'
            // if the client has sent 'bye'.
            if (close) {
                future.addListener(ChannelFutureListener.CLOSE);
                context = null;
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
            context = null;
        }

        public ChannelHandlerContext context() {
            return context;
        }
    }

//    @ChannelHandler.Sharable
//    public static class TelnetClientHandler extends SimpleChannelInboundHandler<String> {
//        @Override
//        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
//            RobotLog.d("[SIM] rev'd: " + msg);
//            int idPos = msg.indexOf(":");
//            String id = msg.substring(0, idPos - 1);
//            String[] data = id.substring(idPos + 1).split(",");
//
//            RobotLog.d("[SIM] proc'd: " + id + ":" + Arrays.toString(data));
//            SimulatorConnection.readInData(id, data);
//        }
//
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//            cause.printStackTrace();
//            ctx.close();
//        }
//    }

    /**
     * Creates a newly configured {@link ChannelPipeline} for a new channel.
     */
    public static class TelnetServerInitializer extends ChannelInitializer<SocketChannel> {
        private TelnetClient client;
        private static final StringDecoder DECODER = new StringDecoder();
        private static final StringEncoder ENCODER = new StringEncoder();

        static final TelnetServerHandler SERVER_HANDLER = new TelnetServerHandler();


        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();


            // Add the text line codec combination first,
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            pipeline.addLast(DECODER);
            pipeline.addLast(ENCODER);

            // and then business logic.
            pipeline.addLast(SERVER_HANDLER);
        }
    }


//    /**
//     * Creates a newly configured {@link ChannelPipeline} for a new channel.
//     */
//    private static class TelnetClientInitializer extends ChannelInitializer<SocketChannel> {
//        private static final StringDecoder DECODER = new StringDecoder();
//        private static final StringEncoder ENCODER = new StringEncoder();
//
//        private static final TelnetClientHandler CLIENT_HANDLER = new TelnetClientHandler();
//
//
//        @Override
//        public void initChannel(SocketChannel ch) {
//            ChannelPipeline pipeline = ch.pipeline();
//
//            // Add the text line codec combination first,
//            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
//            pipeline.addLast(DECODER);
//            pipeline.addLast(ENCODER);
//
//            // and then business logic.
//            pipeline.addLast(CLIENT_HANDLER);
//        }
//    }
}