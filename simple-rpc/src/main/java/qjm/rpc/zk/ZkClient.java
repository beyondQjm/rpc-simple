package qjm.rpc.zk;

import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 获取zookeeper中注册服务器
 * @author QJM
 *
 */
public class ZkClient {
	//日志
	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClient.class);
	
	//链接超时时间
	private int timeout;
	//zookeeper服务器(如：weekend01:2181，多个间用逗号隔开)
	private String zkServeres;
	//服务器节点路径
	private String serverPath;
	//zookeeper实例
	private ZooKeeper zk;
	//zookeeper监听器
	private Watcher watcher;
	//所有服务器地址
	private volatile List<String> serverlist;
	
	public ZkClient() {
		super();
	}

	public ZkClient(String zkServeres,String serverPath){
		this(zkServeres, serverPath, 20000);
	}
	
	public ZkClient(String zkServeres, String serverPath, int timeout){
		super();
		this.zkServeres = zkServeres;
		this.serverPath = serverPath;
		this.timeout = timeout;
		
		init();
	}
	
	//加载配置
	private void init(){
		watcher = new Watcher(){
			public void process(WatchedEvent event) {
				//如果发生变化的在服务器节点下,更新节点信息
				if(event.getType() == EventType.NodeChildrenChanged
						&& serverPath.equals(event.getPath())){
					updateServerList();
				}
			}
		};
		
		getZk();
	}
	
	/**
	 * 获取zookeeper
	 * @return
	 */
	private void getZk(){
		try {
			zk = new ZooKeeper(zkServeres, timeout, watcher);
			//查看服务器根节点是否存在
			Stat stat = zk.exists(serverPath, watcher);
			if(stat == null){
				throw new RuntimeException("服务器未启动！");
			}else{
				//获取所有服务器
				updateServerList();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *更新节点
	 */
	protected void updateServerList() {
		try {
			List<String> servers = zk.getChildren(serverPath, watcher);
			List<String> serverl  = new ArrayList<String>();
			//遍历节点
			for(String server:servers){
				byte[] data = zk.getData(serverPath+"/"+server, false, null);
				serverl.add(new String(data,"UTF-8"));
			}
			serverlist = serverl;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

	/**
	 * 关闭链接
	 * @return
	 */
	public void closeZk(){
		try {
			if(zk != null)
				zk.close();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取服务器地址
	 * @return
	 */
	public synchronized String getServer(){
		String server = null;
		//获取到一个服务器地址，并把它放回到最后
		if(serverlist.size()>0){
			server = serverlist.remove(0);
			serverlist.add(server);
			
			LOGGER.debug("get server:"+server);
		}
		return server;
	}
	
	/**
	 * 获取所有服务器地址
	 * @return
	 */
	public List<String> getServerAll(){
		return serverlist;
	}
}
