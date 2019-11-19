package groenbaek.examples.jpa.persistence;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.Instant;

@Getter
@ToString
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestEntity {

    public static TestEntity create(String value) {
        return  new TestEntity().setValue(value);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Basic
    @Column(nullable = false)
    private String value;

    @Basic
    @Column(nullable = false)
    private Instant lastModified;

    @PrePersist
    @PreUpdate
    public void updateLastModified() {
        lastModified = Instant.now();
    }

}
