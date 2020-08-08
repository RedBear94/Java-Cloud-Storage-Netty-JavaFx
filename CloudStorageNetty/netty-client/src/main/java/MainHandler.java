import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private Callback onMessageReceivedCallBack;

    public MainHandler(Callback onMessageReceivedCallBack) {
        this.onMessageReceivedCallBack = onMessageReceivedCallBack;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(onMessageReceivedCallBack != null){
            Platform.runLater(() -> onMessageReceivedCallBack.callback(msg));
        }
    }
}
