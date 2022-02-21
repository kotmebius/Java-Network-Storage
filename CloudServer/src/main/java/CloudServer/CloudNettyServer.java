package CloudServer;


import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


public class CloudNettyServer extends BaseNettyServer {


    public CloudNettyServer() {
        super(
                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                new ObjectEncoder(),
                new CloudServerNettyHandler()
        );
    }

    public static void main(String[] args) {
        new CloudNettyServer();
    }
}
