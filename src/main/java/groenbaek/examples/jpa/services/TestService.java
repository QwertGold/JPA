package groenbaek.examples.jpa.services;

import groenbaek.examples.jpa.persistence.TestEntity;
import groenbaek.examples.jpa.persistence.TestEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestEntityRepository testEntityRepository;

    @Transactional
    public void addFirstEntity() {
        testEntityRepository.save(TestEntity.create(UUID.randomUUID().toString()));
    }

    @Transactional
    public void updateNewest() {
        List<TestEntity> list = testEntityRepository.findAllByOrderByLastModifiedDesc();
        if (!list.isEmpty()) {
            TestEntity testEntity = list.get(0);
            testEntity.setValue(UUID.randomUUID().toString());
            testEntityRepository.save(testEntity);    // not actually needed
        }
        testEntityRepository.findAll();
    }
}
