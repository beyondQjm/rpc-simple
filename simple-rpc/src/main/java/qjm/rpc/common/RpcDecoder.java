package qjm.rpc.common;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * RPC 解码器
 * @author QJM
 *
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> clazz;

	/**
	 * @param clazz 反序列化的class
	 */
    public RpcDecoder(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();
        if (dataLength < 0) {
            ctx.close();
        }
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
        }
        //将ByteBuf转换为byte[]
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        //将data转换成object
        Object obj = SerializationUtils.deserialize(data, clazz);
        out.add(obj);
    }
}
