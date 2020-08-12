import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class NettyNetwork {
    private SocketChannel channel;
    private Callback onMessageReceivedCallBack;

    private static final String HOST = "localhost";
    private static final int PORT = 8189;

    public NettyNetwork(Callback onMessageReceivedCallBack) {
        this.onMessageReceivedCallBack = onMessageReceivedCallBack;

        Thread thread = new Thread(() -> {
            // 1 пул потоков для работы с сетью (обработки сетевых событий) т.к. никто не будет подключаться к клиенту
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                // Bootstrap - для преднастройки клиента
                Bootstrap bootstrap = new Bootstrap();
                // в качестве пула потоков исп. workerGroup и исп. стандартный сетевой канал NioSocketChannel
                bootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        // Настройка конвеера, по аналогии с сервером
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                channel = socketChannel;
                                // Работаем с объестами => добален добавлены хендлеры кодирования/декодирования
                                socketChannel.pipeline().addLast(
                                        new ObjectDecoder(1024 * 1024,
                                                ClassResolvers.cacheDisabled(null)),
                                        new ObjectEncoder(),
                                        // хендлер для получения данных от сервера
                                        new MainHandler(onMessageReceivedCallBack)
                                );
                            }
                        });
                // Подключение к серверу
                ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
                // Ожидание закрытия
                future.channel().closeFuture().sync(); // Если запустить не в Thread - заблокирует весь интерфейс
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Закрытие потоков
                workerGroup.shutdownGracefully();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    public void sendMessage(Object message) {
        channel.writeAndFlush(message);
    }

    public void close() {
        channel.close();
    }
}
