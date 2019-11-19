package groenbaek.examples.jpa.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {

    List<TestEntity> findAllByOrderByLastModifiedDesc();

}
