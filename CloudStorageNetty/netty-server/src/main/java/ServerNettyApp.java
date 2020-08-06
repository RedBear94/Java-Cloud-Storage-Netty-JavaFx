import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class ServerNettyApp {
    private static final int PORT = 8189;

    public static void main(String[] args) {
        new ServerNettyApp();
    }

    public ServerNettyApp() {
        // Объявление 2-ух пулов потоков
        // поток для работы подключающихся клиентов (1- поток)
        EventLoopGroup authGroup = new NioEventLoopGroup(1);
        // сетевое взаимодействие - обработка данных (20-30 потоков по умолчанию)
        EventLoopGroup workerGroup =  new NioEventLoopGroup();

        try {
            // ServerBootstrap - используется для преднастройки сервера
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(authGroup, workerGroup) // указали серверу использовать 2 пула потоков
                    // для подключения клентов исп-ся канал NioServerSocketChannel - стандартный сервер на NIO
                    .channel(NioServerSocketChannel.class)
                    // Настройка процесса общения с подключаемыми клиентами.
                    // В SocketChannel - лежит информация о клиентах. Инициализируется в методе initChannel
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // в конец pipeline добавляются новые handler-ры для каждого подключения они свои
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new MainHandler());
                        }
                    });
            // sync - запуск сервера на указанном порту в bind
            // ChannelFuture - объекты типа Future это информациия выполняемая из какой-то задачи.
            // через future - можно определить состояние сервера
            ChannelFuture future = bootstrap.bind(PORT).sync();
            System.out.println("Server started!");
            // closeFuture() - блокирующая операция
            future.channel().closeFuture().sync(); // Ждем когда сервер будет остановлен
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Закрытие всех пулов потоков
            authGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
