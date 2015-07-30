package hagerty.simulator.io;

import hagerty.gui.MainApp;
import hagerty.simulator.BrickListGenerator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client {
    public static void main(String[] args) throws Exception {
        String host = "192.168.2.6";
        int port = 7002;
        io.netty.channel.EventLoopGroup workerGroup = new io.netty.channel.nio.NioEventLoopGroup();

        try {
            MainApp mainApp = new MainApp();
            BrickListGenerator gen = new BrickListGenerator(mainApp);
            (new Thread(gen)).start();

            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true); // (4)
            b.handler(new io.netty.channel.ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new Decoder(), new ClientHandler());
                }
            });

            // Start the client.
            io.netty.channel.ChannelFuture f = b.connect(host, port).sync(); // (5)

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}