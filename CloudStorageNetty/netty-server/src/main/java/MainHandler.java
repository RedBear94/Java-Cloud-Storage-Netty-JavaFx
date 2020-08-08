import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

// Обработчик (Inbound - на вход)
public class MainHandler extends ChannelInboundHandlerAdapter {

    private String clientName;
    private static int cnt = 0;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Вызывется один раз при подключении клиента
        cnt++;
        clientName = "user#" + cnt;
        System.out.println("Client " + clientName + " connected");
        ctx.writeAndFlush("Вы подключились");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Полученение сообщений от клиента
        // получение и отправка данных происходит ввиде ByteBuf для первого и последнего handler-ра при отсутвии
        // кодирования и декодирования (в данном случае обрабатываются объекты получаемые от клиента)
        if (msg instanceof String) {
            System.out.println("message from client " + clientName + ": " + msg);
            ctx.writeAndFlush(msg);

            // Выполнение комманд полученных от клиента
            String command = (String) msg;
            if(command.startsWith("/")) {
                if(command.startsWith("/download ")) {
                    String [] op = command.split(" ");
                    File file = new File("./netty-server/src/main/resources/" + op[1]);
                    if(file.exists()) {
                        ctx.writeAndFlush(file);
                    }
                }
                return;
            }
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
        System.out.println("На клиенте " + clientName + " произошло исключение");
        ctx.close(); // отключение клиента при ошибке
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Вызов метода при отключении клиента от канала
        System.out.println("Client disconnected");
    }
}
