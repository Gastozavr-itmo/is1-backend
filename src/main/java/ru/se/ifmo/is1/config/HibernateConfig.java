package ru.se.ifmo.is1.config;

import org.hibernate.SessionFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    @Bean
    public DataSource dataSource() {
        try {
            return (DataSource) new InitialContext().lookup("java:/Helios");
        } catch (NamingException e) {
            throw new IllegalStateException("JNDI DataSource java:/jdbc/studs not found in WildFly", e);
        }
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource ds) {
        LocalSessionFactoryBean sf = new LocalSessionFactoryBean();
        sf.setDataSource(ds);
        sf.setMappingLocations(
                new ClassPathResource("mapping/Product.hbm.xml"),
                new ClassPathResource("mapping/Organization.hbm.xml"),
                new ClassPathResource("mapping/Person.hbm.xml")
        );

        Properties p = new Properties();
        p.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        p.put("hibernate.show_sql", "true");
        p.put("hibernate.format_sql", "true");
        p.put("hibernate.jdbc.time_zone", "UTC");
        p.put("hibernate.current_session_context_class", "org.springframework.orm.hibernate5.SpringSessionContext");
        sf.setHibernateProperties(p);
        return sf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(SessionFactory sf) {
        return new HibernateTransactionManager(sf);
    }
}
