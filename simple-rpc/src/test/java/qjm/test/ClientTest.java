package qjm.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import qjm.rpc.client.ServiceProxyFactory;
import qjm.test.impl.Person;
import qjm.test.impl.PersonService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-client.xml")
public class ClientTest {
	
	@Autowired
	ServiceProxyFactory serviceProxyFactory;
	
	@Test
	public void clientTest() {
		PersonService service = serviceProxyFactory.getProxy(PersonService.class);
		
		System.out.println(service.getInfo());
		
		Person person = new Person();
		person.setAge(23);
		person.setName("Qjm");
		person.setSex("男");
		System.out.println(service.printInfo(person));
	}

}
