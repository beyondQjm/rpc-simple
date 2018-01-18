package qjm.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RPC编码器
 * @author QJM
 *
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {

	private Class<?> clazz;

	/**
	 * @param clazz 序列化类型
	 */
	public RpcEncoder(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public void encode(ChannelHandlerContext ctx, Object inob, ByteBuf out)
			throws Exception {
		//序列化
		if (clazz.isInstance(inob)) {
			byte[] data = SerializationUtils.serialize(inob);
			out.writeInt(data.length);
			out.writeBytes(data);
		}
	}
}