package qjm.rpc.zk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务器端zookeeper
 * @author QJM
 *
 */
public class ZkServer {
	//日志
	private static final Logger LOGGER = LoggerFactory.getLogger(ZkServer.class);
	
	//链接超时时间
	private int timeout;
	//zookeeper服务器(如：weekend01:2181，多个间用逗号隔开)
	private String zkServeres;
	//服务器节点路径
	private String serverPath;
	//服务器host:port
	private String monitorServer;
	//zookeeper实例
	private ZooKeeper zk;
	//zookeeper监听器
	private Watcher watcher;
	
	public ZkServer(String server, String zkServeres,String serverPath){
		this(server, zkServeres, serverPath, 20000);
	}
	
	public ZkServer(String monitorServer, String zkServeres, String serverPath, int timeout){
		this.monitorServer = monitorServer;
		this.zkServeres = zkServeres;
		this.serverPath = serverPath;
		this.timeout = timeout;
		
		init();
	}

	//加载配置
	private void init(){
		watcher = new Watcher(){
			public void process(WatchedEvent event) {
				LOGGER.debug(event.getPath()+" :"+event.getType().name());
			}
		};
		
		try {
			zk = new ZooKeeper(zkServeres, timeout, watcher);
			//查看服务器根节点是否存在
			Stat stat = zk.exists(serverPath, watcher);
			if(stat == null){
				zk.create(serverPath, "rpcServeres".getBytes("UTF-8"), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				LOGGER.debug("create znode:"+serverPath);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * zookeeper中注册服务器
	 * @return
	 */
	public void register(){
		try {
			//注册服务器
			zk.create(serverPath+"/rpcServer", monitorServer.getBytes("UTF-8"), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取监听地址
	 * @return
	 */
	public String getMonitorServer() {
		return monitorServer;
	}
}
