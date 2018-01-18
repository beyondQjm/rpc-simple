package qjm.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import qjm.rpc.common.RpcDecoder;
import qjm.rpc.common.RpcEncoder;
import qjm.rpc.common.RpcRequest;
import qjm.rpc.common.RpcResponse;

/**
 * 客户端请求服务器类
 * @author QJM
 *
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse>{
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientHandler.class);

	//服务器地址
	private String host;
	//服务器监听端口
	private int port;

	private RpcResponse response;

	private final Object obj = new Object();

	public RpcClientHandler(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * 链接服务端，发送消息
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public RpcResponse send(RpcRequest request) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try{
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class);
			bootstrap.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					// 添加编码、解码、业务处理的handler
					ch.pipeline().addLast(new RpcEncoder(RpcRequest.class))
					.addLast(new RpcDecoder(RpcResponse.class))
					.addLast(RpcClientHandler.this);
				}
				
			}).option(ChannelOption.SO_KEEPALIVE, true);
			
			// 链接服务器
			ChannelFuture future = bootstrap.connect(host, port).sync();
			//将request对象写入outbundle处理后发出（即RpcEncoder编码器）
			future.channel().writeAndFlush(request).sync();

			/*
			 * 用线程等待的方式决定是否关闭连接
			 * 其意义是：先在此阻塞，等待获取到服务端的返回后，被唤醒，从而关闭网络连接
			 */
			synchronized (obj) {
				obj.wait();
			}
			if (response != null) {
				future.channel().closeFuture().sync();
			}
			return response;
		}finally{
			group.shutdownGracefully();
		}
	}

	/**
	 * 读取服务端的返回结果
	 */
	@Override
	public void channelRead0(ChannelHandlerContext ctx, RpcResponse response)
			throws Exception {
		this.response = response;

		synchronized (obj) {
			obj.notifyAll();
		}
	}

	/**
	 * 异常处理
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		LOGGER.error("client caught exception", cause);
		ctx.close();
	}

}
