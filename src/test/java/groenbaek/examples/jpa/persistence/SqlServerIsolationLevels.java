package groenbaek.examples.jpa.persistence;

import groenbaek.examples.jpa.AppConfiguration;
import groenbaek.examples.jpa.services.TransactionTestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Slf4j
@ActiveProfiles({"sqlserver"})  // hibernate is implicit when no JPA profile is specified
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AppConfiguration.class)
public class SqlServerIsolationLevels {

    @Autowired
    private TestEntityRepository repository;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    private TransactionTestService transactionTestService;

    @Before
    public void cleanup() {
        repository.deleteAllInBatch();
    }

    @After
    public void after() {
        if (!executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    /**
     * Test that values inserted in another transaction can be read by a concurrent READ_UNCOMMITTED transaction after the insert statement has
     * been flushed to the database
     */
    @Test
    public void readUncommittedCreateVisible() {
        repository.save(TestEntity.create("first"));
        Semaphore started = new Semaphore(0);
        Semaphore flushed = new Semaphore(0);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                transactionTestService.create("second", started, flushed);
            }
        });

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<TestEntity> all = transactionTestService.findAllUncommitted();
            log.info("{}", all);
            started.release();  // make sure we read once before creation
            Assertions.assertThat(all).hasSize(2);
        });

        // even though the create transaction action is not done until after this semaphore is released it is visible to READ_UNCOMMITTED transactions
        flushed.release();
    }

    /**
     * Tests that a READ_COMMITTED blocks if a concurrent transaction is insert a row
     */
    @Test
    public void readCommittedBlockOnInsert() throws Exception {
        repository.save(TestEntity.create("first"));
        Semaphore started = new Semaphore(0);
        Semaphore flushed = new Semaphore(0);

        executorService.execute(() -> transactionTestService.create("second", started, flushed));

        Thread.sleep(1000); // should be enough for the transaction to be started
        List<TestEntity> all = transactionTestService.findAllCommitted();
        Assertions.assertThat(all).hasSize(1);
        started.release();
        Thread.sleep(1000); // should be enough for flush to have happened

        Future<List<TestEntity>> future = executorService.submit(() -> {
            return transactionTestService.findAllCommitted();
        });
        Thread.sleep(3000);    // 3 seconds should be enough to try to read
        assertFalse("should block as a update lock prevents read", future.isDone());
        future.cancel(true);

        flushed.release();
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue(terminated);
        // we know the transaction is committed, insert should now be visible
        all = transactionTestService.findAllCommitted();
        Assertions.assertThat(all).hasSize(2);



    }

    /**
     * Checks that a READ_UNCOMMITTED can read a value updated by a different transaction, even if the updating transaction has not been committed yet
     */
    @Test
    public void readUncommitted() throws Exception {
        TestEntity firstValue = repository.save(TestEntity.create("originalValue"));

        Semaphore started = new Semaphore(0);
        Semaphore flushed = new Semaphore(0);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                transactionTestService.update(firstValue.getId(), "updatedValue", started, flushed);
            }
        });

        TestEntity firstRead = transactionTestService.findUncommitted(firstValue.getId());
        log.info("read before flush{}", firstRead);
        started.release();  // make sure we read once before creation
        // we should see the change at some point
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            TestEntity testEntity = transactionTestService.findUncommitted(firstValue.getId());
            log.info("{}", testEntity);
            assertEquals("updatedValue", testEntity.getValue());

        });
        flushed.release();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void readCommittedBlocksOnUpdate() throws Exception {
        TestEntity firstValue = repository.save(TestEntity.create("originalValue"));

        Semaphore started = new Semaphore(0);
        Semaphore flushed = new Semaphore(0);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                transactionTestService.update(firstValue.getId(), "updatedValue", started, flushed);
            }
        });

        TestEntity firstRead = transactionTestService.findCommitted(firstValue.getId());
        log.info("read before flush{}", firstRead);
        started.release();
        // once the update has been flushed to the database, any attempt to read the row in a READ_COMMITTED transaction will block until the transaction
        // completes. So we spawn a new thread which will try to load the entity, but not complete
        Thread.sleep(1000); // we have to wait a little to make sure flush has happened
        Future<TestEntity> future = executorService.submit(() -> {
            return transactionTestService.findCommitted(firstValue.getId());    // this will block
        });
        Thread.sleep(3000);    // 3 seconds should be enough to try to read
        assertFalse("should block as a update lock prevents read", future.isDone());
        future.cancel(true);

        flushed.release();
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue(terminated);
        // we know the transaction is committed, update should now be visible
        TestEntity testEntity = transactionTestService.findCommitted(firstValue.getId());
        assertEquals("updatedValue", testEntity.getValue());
    }

    private interface ValueReader {
        List<TestEntity> findAll();
    }
}
