package cn.devmgr.tutorial.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranLearning {

	@Autowired
	private PersonDao personDao;

	@Transactional(readOnly = true)
	public void verifyReadOnly() {

		PersonDto person = new PersonDto();
		person.setId(1);
		person.setName("jason Wang");
		

		int a = personDao.insert(person);
		
		int x = 1/0 ;
		
		System.out.println(a);

	}

}
