import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

// Обработчик (Inbound - на вход)
public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Вызывется один раз при подключении клиента
        System.out.println("Client connected");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Полученение сообщений от клиента
        // получение и отправка данных происходит ввиде ByteBuf для первого и последнего handler-ра при отсутвии
        // кодирования и декодирования (в данном случае обрабатываются объекты получаемые от клиента)
        if (msg instanceof String) {
            System.out.println("string: " + msg);
        } else if (msg instanceof List) {
            System.out.println("list: " + msg);
        } else if (msg instanceof File){
            File file = (File) msg;
            Files.copy(new FileInputStream(file),
                    Paths.get("./cloud_server", file.getName()),
                    StandardCopyOption.REPLACE_EXISTING);
            ctx.writeAndFlush("FEEDBACK");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close(); // отключение клиента при ошибке
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Вызов метода при отключении клиента от канала
        System.out.println("Client disconnected");
    }
}
