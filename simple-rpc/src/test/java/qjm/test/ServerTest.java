package qjm.test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerTest {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws ClassNotFoundException {
		 new ClassPathXmlApplicationContext("classpath:spring-server.xml");
	}

}
