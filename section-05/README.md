README
===========================
Spring Transactional @Transactional中propagation参数的例子，用实际例子来了解各种情况，比看文档更深刻些，需要PostgreSQL Server。 

### 代码简介

运行AppTests中的doSomthing的测试用例，doSomthing中有7个函数，建议每次运行1个，以便看得更清楚。

### 准备数据库

如果没有PostgreSQL Server，请先安装一份。可从http://www.postgres.org下载。
创建一个数据库，名为tvseries，并在此数据库内创建1个表，建表语句可参考sq.sql(./sql.sql)文件。

````SQL
create table person(
   id int primary key,
   name varchar(50) not null,
   birthday timestamp default now()
);
````

### 运行
```bash
mvn test
```

### 事务隔离级别的SQL测试
#### 测试REPEATABLE READ

打开一个psql命令行(No.1)：
```SQL
insert into person (id, name) values(1, 'Tom');

BEGIN TRANSACTION;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT * FROM person WHERE id=1;
```
去另一终端，打开一个psql连接(No.2)，执行
```SQL
UPDATE person SET name='Mike' WHERE id=1;
```
返回到前面开始事务的终端(No.1)
```SQL
UPDATE person SET name='Jack' WHERE id=1;
```
会得到错误信息：

    ERROR:  could not serialize access due to concurrent update

这是遇到了不可重复读，事务运行失败，只能被回滚，即便尝试commit，也不会成功，事务只能被rollback，因为不满足REPEATABLE READ的隔离级别了

#### 测试SERIALIZABLE

首先准备点数据
```SQL
insert into person (id, name) values(1, 'Tom');
insert into person (id, name) values(2, 'Tom-2');
insert into person (id, name) values(3, 'Tom-3');
```
在(No.1)号psql命令后：
```SQL
BEGIN TRANSACTION;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SELECT * FROM person WHERE id<10;
update person set name='Jack' where id=1;
```
 暂停，去另一终端，打开一个psql连接(No.2)，执行
```SQL
BEGIN TRANSACTION;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
delete from person where id=3;
SELECT * FROM person WHERE id<5;
COMMIT;
```
返回到前面开始事务的psql命令行(No.1)，commit或者执行其他sql，会得到错误信息：

    ERROR:  could not serialize access due to read/write dependencies among transactions
    DETAIL:  Reason code: Canceled on identification as a pivot, during commit attempt.
    HINT:  The transaction might succeed if retried.

这是遇到了幻读，前面统计到是3条，后面统计到2条，事务运行失败，只能被回滚。如果其中没有update语句，不涉及更新数据会执行成功的。

如果另外一个事务不是设置成SERIALIZABLE，是不会出现这个异常的。postgresql文档中关于serializable有解释说明了这一点

    In fact, this isolation level works exactly the same as Repeatable Read except that it monitors for 
    conditions which could make execution of a concurrent set of serializable transactions behave in a 
    manner inconsistent with all possible serial (one at a time) executions of those transactions. 

只有两个都是SERIABLIZABLE隔离级别的事务才会互相影响。

-------------------
-------------------
readonly是表明当前是个readOnly的transaction还是write的 read/write的

@Transactional默认会处理好，begin transaction, do transaction, rollback at excpetion, commit等
Propagation.MANDATORY必须在一个已有的事务中执行,否则抛出异常  Mandatory自身不产生transaction
Propagation.REQUIRED果有事务, 那么加入事务, 没有的话新建一个(默认)
Propagation.NOT_SUPPORTED 容器不为这个方法开启事务
Propagation.REQUIRE_NEW必须为当前方法开启个新的事务，否则异常
PROPAGATION_NESTED 如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则进行与PROPAGATION_REQUIRED类似的操作。
Propagation.NEVER 必须在一个没有的事务中执行,否则抛出异常(与Propagation.MANDATORY相反)
Propagation.SUPPORTS 如果其他bean调用这个方法,在其他bean中声明事务,那就用事务.如果其他bean没有声明事务那就不用事务

throws 中涵盖的exception及其子类，都不会导致rollback，所以再@transactional对应的throws定义时需要特别注意
norollbackfor exception指定不为十么exception rollback
rollbackfor exception指的是为特定class rollback.


@Select("lock table person in  ACCESS EXCLUSIVE mode")可以锁定表
Timeout从方法开始到方法中最后一个sql语句结束  

