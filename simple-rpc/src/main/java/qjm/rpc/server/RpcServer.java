package qjm.rpc.server;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import qjm.rpc.anno.RpcService;
import qjm.rpc.common.RpcDecoder;
import qjm.rpc.common.RpcEncoder;
import qjm.rpc.common.RpcRequest;
import qjm.rpc.common.RpcResponse;
import qjm.rpc.zk.ZkServer;

public class RpcServer implements ApplicationContextAware, InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);
	
	//rpc服务
	private Map<String, Object> handlers = new HashMap<String, Object>();
	//rpc服务地址
	private String monitorServer;
	//zookeeper服务
	private ZkServer zkServer;
	
	public RpcServer(ZkServer zkServer) {
		this.zkServer = zkServer;
	}

	public RpcServer(String monitorServer) {
		this.monitorServer = monitorServer;
	}

	/**
	 * 加载所有服务
	 * Spring容器会在加载完后调用
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<String, Object> services = applicationContext.getBeansWithAnnotation(RpcService.class);
		if(services != null && services.size() != 0){
			for (Object serviceBean : services.values()) {
				//从业务实现类上的自定义注解中获取到value，从来获取到业务接口的全名
				String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
				handlers.put(interfaceName, serviceBean);
			}
		}
	}

	/**
	 * 注册监听器
	 * *在setApplicationContext之后被Spring执行
	 */
	public void afterPropertiesSet() throws Exception {
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(workerGroup,bossGroup)
					 .channel(NioServerSocketChannel.class)
					 .childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel channel)
								throws Exception {
							// 添加编码、解码、业务处理的handler
							channel.pipeline()
									.addLast(new RpcDecoder(RpcRequest.class))
									.addLast(new RpcEncoder(RpcResponse.class))
									.addLast(new RpcServerHandler(handlers));
						}
					}).option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);

			//注册服务
			if(monitorServer == null){
				zkServer.register();
				monitorServer = zkServer.getMonitorServer();
			}
			String[] array = monitorServer.split(":");
			String host = array[0];
			int port = Integer.parseInt(array[1]);

			ChannelFuture future = bootstrap.bind(host, port).sync();
			LOGGER.debug("server started on port {}", port);

			future.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
		
	}
}
