package cn.devmgr.tutorial.tx.test;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionUsageException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cn.devmgr.tutorial.tx.AnotherService;
import cn.devmgr.tutorial.tx.PersonDao;
import cn.devmgr.tutorial.tx.PersonDto;
import cn.devmgr.tutorial.tx.PersonService;
import cn.devmgr.tutorial.tx.TranLearning;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AppTests {
	private final Log log = LogFactory.getLog(AppTests.class);

	@Autowired
	PersonService serv;
	@Autowired
	AnotherService anotherService;
	@Autowired
	PersonDao dao;

	@Autowired
	TranLearning tl;

	@Before
	public void setUp() {
		dao.deleteAll();
	}

	@Test
	public void testSomething() {
		// 为了看的清晰，每次只执行一个测试
		// testTransactionalRO();
//		testRequiredNewAndNewFailed();
//		 testInsertNoTransInsideTrans();
//		 testInsertNoAnnotationInsideTrans();
//		 testNested();
//		 testNested2();
//		 testMandatory();
//		testMandatory2();
//		testMandatory3();
//		testMandatory4();
//		 testThrowException();
//		 testThrowExceptionAndCommit();
		 testTranscationalTimeoutParamter();
	}

	/**
	 * learn @transactional readOnly
	 */

	public void testTransactionalRO() {

		tl.verifyReadOnly();

	}

	/**
	 * MANDATORY要求必须在一个已有的事务中执行,否则抛出异常；
	 * 此测试中，调用insertMandatory方法（方法声明Propagation.MANDATORY)之前没有开启事务 所以必定会抛出异常
	 */
	
	public void testMandatory() {
		boolean b = false;
		try {
			serv.insertMandatory(new PersonDto(20002, "乙"), false);
		} catch (TransactionUsageException e) {
			log.trace(e.getClass().getName());
			b = true;
		}
		Assert.assertFalse(b);
	}
	
	/**
	 * mandatory combined with exist tx, then will be commited together with exist tx
	 */
	public void testMandatory2() {
		boolean b = false;
		try {
			serv.insertMandatory2(new PersonDto(20002, "乙"), false,false);
		} catch (TransactionUsageException e) {
			log.trace(e.getClass().getName());
			b = true;
		}
		Assert.assertFalse(b);
	}
	
	/*
	 * mandatory do not have own transaction, therefore its own exception will not rollback its own 
	 * if its exception raise not catched it will cause its out ex rollback
	 */
	public void testMandatory3() {
		boolean b = false;
		try {
			serv.insertMandatory2(new PersonDto(20002, "乙"), true,false);
		} catch (TransactionUsageException e) {
			log.trace(e.getClass().getName());
			b = true;
		}
		Assert.assertFalse(b);
	}
	
	/*
	 * mandatory do not have own transaction, therefore its own exception will not rollback its own 
	 * if its exception raise and caught, it will be committed together with its outside tx
	 */
	public void testMandatory4() {
		boolean b = false;
		try {
			serv.insertMandatory3(new PersonDto(20002, "乙"), false,true);
		} catch (TransactionUsageException e) {
			log.trace(e.getClass().getName());
			b = true;
		}
		Assert.assertFalse(b);
	}

	/**
	 * 一个事务中嵌套另外一个子事务；子事务回滚不影响父级事务
	 */
	public void testNested() {
		dao.deleteAll();
		try {
			anotherService.insertNested(new PersonDto(1, "甲"), new PersonDto(2, "乙"), false);
		} catch (RuntimeException e) {

		}
		List<PersonDto> list = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("query all list.size()=" + list.size());
		}
		// 外部事务的p1应该被保存了
		Assert.assertTrue(list.size() == 1);
	}
	
	/**
	 * 一个事务中嵌套另外一个子事务；子事务跟随父事务一起commit
	 */
	public void testNested2() {
		dao.deleteAll();
		try {
			anotherService.insertNested2(new PersonDto(1, "甲"), new PersonDto(2, "乙"), false);
		} catch (RuntimeException e) {

		}
		List<PersonDto> list = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("query all list.size()=" + list.size());
		}
		// 子事务和外部事务应该一起被保存了
		Assert.assertTrue(list.size() == 2);
		
		System.out.println(list.get(0).getName() + " " + list.get(1).getName());
	}

	/**
	 * 一个事务中执行了另外一个没有事务注解的方法，（这个和一个函数是一样的）
	 */
	public void testInsertNoAnnotationInsideTrans() {
		dao.deleteAll();
		try {
			anotherService.insertNoAnnotationInsideTrans(new PersonDto(1, "甲"), new PersonDto(2, "乙"), true);
		} catch (RuntimeException e) {

		}
		List<PersonDto> list = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("query all list.size()=" + list.size());
		}
		// 不开启事务的Person2 和person 1 被合并到一个事务了，因为exception而rollback了所有的
		Assert.assertTrue(list.size() == 0);
	}

	/**
	 * 一个事务中执行了NOT_SUPPORT的方法，NOT_SUPPORT注解的方法不会加入到当前事务中。
	 * 即时调用NOT_SUPPORT注解方法的方法中事务回滚了，也不会影响不开事务的。
	 */
	public void testInsertNoTransInsideTrans() {
		dao.deleteAll();
		try {
			anotherService.insertNoTransInsideTrans(new PersonDto(1, "甲"), new PersonDto(2, "乙"), true);
		} catch (RuntimeException e) {

		}
		List<PersonDto> list = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("query all list.size()=" + list.size());
		}
		// 不开启事务的Person 2应该被保存了, 而开启了事务的person1 因为exception回滚而没有更新到数据库
		Assert.assertTrue(list.size() == 1);
		Assert.assertEquals(2, list.get(0).getId());
	}

	/**
	 * 抛出异常的测试
	 */
	public void testThrowException() {
		dao.deleteAll();

		// exceptions raised during method execution belongs to exceptions defined by throws，事务是还是会被提交的；如果声明了throws Exception，那就几乎所有情况都被提交事务了
		PersonDto p = new PersonDto(1, "甲");
		try {
			anotherService.insertAndThrowException(p, new Exception("ERROR"));
		} catch (Exception e) {

		}
		List<PersonDto> list = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("query all list.size()=" + list.size());
		}
		Assert.assertTrue(list.size() == 1);

//		dao.deleteAll();
	}

	public void testThrowExceptionAndCommit() {
		dao.deleteAll();
		PersonDto p = new PersonDto(1, "甲");
		PersonDto yi = new PersonDto(2,"yi");
		try {
			anotherService.insertRollbackForSQLException(p, new SQLException("sql exception"));
		} catch (Exception e) {

		}
		List<PersonDto> list = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("query all list.size()=" + list.size());
		}
		//due to exception SQLExcetpion is defined for rollbackfor,hence there should be record saved.
		Assert.assertTrue(list.size() == 0);

		// 设置了rollbackFor之后，仅仅rollbackFor的会回滚，不是rollbackFor或其子类的就不回滚了
		try {
			anotherService.insertRollbackForSQLException(p, new Exception("common exception"));
		} catch (Exception e) {

		}
		List<PersonDto> list2 = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("rollbackfor sqlExcpetion but current exception is Exception hence tx should be comminited,query all list2.size()=" + list2.size());
		}
		//due to Exception is not SQLException or SQLExceptions's subClass, therefore record will be commited.
		Assert.assertTrue(list2.size() == 1);

		// 设置了noRollbackFor NullPointerException，rollBackFor
		// RuntimeException，NullPointerException是RuntimeException的子类
		try {
			anotherService.insertNoRollbackForSQLException(yi, new NullPointerException("null"));
		} catch (Exception e) {

		}
		List<PersonDto> list3 = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("norollback for NullPointerException hence commit should happened.rollback for query all list2.size()=" + list3.size());
		}
		//noRollback for NullPointerException defined for transaction,therefore record is updated
		Assert.assertTrue(list3.size() == 2);
	}

	/**
	 * 一个事务中，调用一个Propagation.REQUIRED_NEW的方法，两个不互相影响，各自负责自己的事务。 内部的失败回滚了，外部一样会提交成功
	 */
	public void testRequiredNewAndNewFailed() {
		dao.deleteAll();
		try {
			anotherService.insert2trans(new PersonDto(1, "甲"), new PersonDto(2, "乙"), false);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		List<PersonDto> list = serv.getAll();
		if (log.isTraceEnabled()) {
			log.trace("query all list.size()=" + list.size());
		}
		// 由于p1的事务和p2的事务是分割开的，p2的失败并没有影响p1的保存，所以最终p1是保存到数据库了的
		Assert.assertTrue(list.size() == 1);
		Assert.assertEquals(1, list.get(0).getId());

	}

	/**
	 * 测试Transactional注解的timeout参数
	 */
	public void testTranscationalTimeoutParamter() {
		dao.deleteAll();
		// 这里开启一个线程把table person锁住10秒，以便后续sql在10秒内无法完成
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					anotherService.lockTable(10);
				} catch (InterruptedException e) {
					// e.printStackTrace();
				}
			}
		};
		t.start();
		// insertWithTimeout方法有设置timeout是5秒，上面锁了15秒，这里等待时间会超过5秒，看看是否抛出异常
		boolean noException = false;
		long t1 = System.currentTimeMillis();
		try {
			anotherService.insertWithTimeout(new PersonDto(1, "甲"), new PersonDto(2, "乙"), 0, 0, 0);
		} catch (Exception e) {
			// 应该是抛出异常的
			noException = true;
			if (log.isTraceEnabled()) {
				log.trace("捕获异常", e);
			}
		}
		long cost = System.currentTimeMillis() - t1;
		Assert.assertTrue(noException);

		if (cost < 5000) {
			// 表一直被锁住，insert无法执行，需要等待；事务5秒超时，这个方法如果执行实际小于5秒，说明哪里出错了
			Assert.fail("cost=" + cost + "这个测试有问题");
		} else {
			if (log.isTraceEnabled()) {
				log.trace("@Transcational(timeout=5)，实际执行花费了" + cost / 1000 + "秒。");
			}
		}

		// 执行下一个测试前，先把锁表线程结束掉
		t.interrupt();
		while (t.isAlive()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {

			}
			if (log.isTraceEnabled()) {
				log.trace("等待锁表线程结束...");
			}
		}

		// 下面来测试另外一种情况，设置了timeout=5,先sleep5秒，然后先执行2条SQL看看是否抛出异常
		noException = false;
		t1 = System.currentTimeMillis();
		try {
			anotherService.insertWithTimeout(new PersonDto(5, "丙"), new PersonDto(6, "丁"), 5, 0, 0);
		} catch (Exception e) {
			noException = true;
			if (log.isTraceEnabled()) {
				log.trace("捕获异常", e);
			}
		}
		if (log.isTraceEnabled()) {
			log.trace(
					"先sleep后执行SQL，耗时" + (System.currentTimeMillis() - t1) + "ms, " + (noException ? "有异常抛出" : "没抛异常"));
		}
		Assert.assertTrue(noException);

		// 下面来测试另外一种情况，设置了timeout=5,然后先执行一条SQL，让线程等待5秒，然后执行另外一条SQL，看看是否抛出异常
		noException = false;
		t1 = System.currentTimeMillis();
		try {
			anotherService.insertWithTimeout(new PersonDto(5, "丙"), new PersonDto(6, "丁"), 0, 5, 0);
		} catch (Exception e) {
			noException = true;
			if (log.isTraceEnabled()) {
				log.trace("捕获异常", e);
			}
		}
		if (log.isTraceEnabled()) {
			log.trace(
					"两个SQL中间sleep，耗时" + (System.currentTimeMillis() - t1) + "ms, " + (noException ? "有异常抛出" : "没抛异常"));
		}
		Assert.assertTrue(noException);

		// 下面来测试另外一种情况，设置了timeout=5,然后先执行一条SQL，先执行2条SQL，再sleep5秒，看看是否抛出异常
		noException = false;
		t1 = System.currentTimeMillis();
		try {
			anotherService.insertWithTimeout(new PersonDto(5, "丙"), new PersonDto(6, "丁"), 0, 0, 6);
		} catch (Exception e) {
			noException = true;
			if (log.isTraceEnabled()) {
				log.trace("捕获异常", e);
			}
		}
		if (log.isTraceEnabled()) {
			log.trace("执行2个SQL，后sleep，耗时" + (System.currentTimeMillis() - t1) + "ms, "
					+ (noException ? "有异常抛出" : "没抛异常"));
		}
		// 这段测试是不抛异常的
		Assert.assertFalse(noException);

		if (log.isTraceEnabled()) {
			log.trace("结论：只要被@Transactional(timeout=xx)注解的方法，时长的计算方式是：从方法执行开始，到方法内最后一条SQL之间的时长是否超过timeout设定的秒数。"
					+ "异常可能是： org.springframework.transaction.TransactionTimedOutException: Transaction timed out，"
					+ "也可能是数据库给出的：org.postgresql.util.PSQLException: ERROR: canceling statement due to user request");
		}
	}
}
