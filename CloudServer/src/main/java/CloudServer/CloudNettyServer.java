package CloudServer;


import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;


public class CloudNettyServer extends BaseNettyServer {


    public CloudNettyServer() {
//        super(
//                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
//                new ObjectEncoder(),
//                new CloudServerNettyHandler()
//        );
    }

//    public static void main(String[] args) {
//        new CloudNettyServer();
//    }
}
