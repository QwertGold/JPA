package groenbaek.examples.jpa.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.CACHE_SHARED_DEFAULT;
import static org.eclipse.persistence.config.PersistenceUnitProperties.WEAVING;

/**
 *
 * The JPA configuration, requires a DataSource
 */
@Profile("eclipse")
@Configuration
@EnableTransactionManagement
@Slf4j
public class EclipseLinkConfiguration extends JpaBaseConfiguration {


    public EclipseLinkConfiguration(DataSource dataSource, JpaProperties properties, ObjectProvider<JtaTransactionManager> jtaTransactionManager, ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        super(dataSource, properties, jtaTransactionManager, transactionManagerCustomizers);
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
       return new EclipseLinkJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(PersistenceUnitProperties.WEAVING, "false");
        map.put(PersistenceUnitProperties.DDL_GENERATION, "drop-and-create-tables");

        map.put(PersistenceUnitProperties.BATCH_WRITING, "jdbc");
        map.put(PersistenceUnitProperties.CACHE_SHARED_DEFAULT, "false");

        map.put(PersistenceUnitProperties.LOGGING_PARAMETERS, "true");

        //map.put(LOGGING_FILE, "output.log");
        //map.put(LOGGING_LEVEL, "FINE");
        return map;
    }

}
