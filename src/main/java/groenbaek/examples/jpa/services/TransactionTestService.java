package groenbaek.examples.jpa.services;

import groenbaek.examples.jpa.persistence.TestEntity;
import groenbaek.examples.jpa.persistence.TestEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * For testing transaction and isolation level. Insert/update methods require a semaphore (0 permits) which will be acquired, after the UnitOfWork has been
 * sent to the database but before the transaction commits. This allows the test to control when the transaction is committed
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionTestService {

    private final TestEntityRepository testEntityRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)  // isolation doesn't matter when creating
    @SneakyThrows(InterruptedException.class)
    public void create(String value, Semaphore transactionStarted, Semaphore flushed) {
        printIsolationLevel();
        transactionStarted.acquire();
        testEntityRepository.saveAndFlush(TestEntity.create(value));
        flushed.acquire();
        log.info("Transaction released");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED) // isolation doesn't matter when updating
    @SneakyThrows(InterruptedException.class)
    public void update(long id, String newValue, Semaphore transactionStarted, Semaphore flushed) {
        printIsolationLevel();
        transactionStarted.acquire();
        TestEntity entity = testEntityRepository.findById(id).orElseThrow(() -> new IllegalStateException("Not found"));
        entity.setValue(newValue);
        testEntityRepository.flush();
        flushed.acquire();
        log.info("Transaction released");
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TestEntity findUncommitted(long id) {
        printIsolationLevel();
        return testEntityRepository.findById(id).orElseThrow(() -> new IllegalStateException("Not found"));
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<TestEntity> findAllUncommitted() {
        printIsolationLevel();
        return testEntityRepository.findAll();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<TestEntity> findAllCommitted() {
        printIsolationLevel();
        return testEntityRepository.findAll();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TestEntity findCommitted(long id) {
        printIsolationLevel();
        return testEntityRepository.findById(id).orElseThrow(() -> new IllegalStateException("Not found"));
    }


    /**
     * @see org.springframework.transaction.TransactionDefinition
     */
    private void printIsolationLevel() {
        Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        log.info("Isolation level is {}", isolationLevel);
    }


}
