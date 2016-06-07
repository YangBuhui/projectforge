package org.projectforge.framework.configuration;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.projectforge.framework.persistence.attr.impl.AttrSchemaServiceSpringBeanImpl;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.jpa.init.LocalSettingsPropertySourcesPlaceholderConfigurer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.db.jpa.history.entities.HistoryMasterBaseDO;
import de.micromata.genome.db.jpa.history.impl.HistoryServiceImpl;
import de.micromata.genome.db.jpa.tabattr.api.AttrSchemaService;
import de.micromata.genome.util.runtime.LocalSettingsEnv;
import de.micromata.mgc.jpa.spring.SpringEmgrFilterBean;
import de.micromata.mgc.jpa.spring.factories.JpaToSessionFactorySpringBeanFactory;
import de.micromata.mgc.jpa.spring.factories.JpaToSessionSpringBeanFactory;

/**
 * Intial configuration for ORM.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DatabaseOrmConfiguration
{
  @Autowired
  private SpringEmgrFilterBean springEmgrFilterBean;

  private static final String DS_JDNI = "java:comp/env/projectForge/jdbc/dsWeb";

  public DatabaseOrmConfiguration()
  {
    //Need to call first to init local-settings.properties
    LocalSettingsEnv.get();
  }

  @Bean
  public static PropertyPlaceholderConfigurer properties()
  {
    return new LocalSettingsPropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public static DataSource dataSource()
  {
    Object ret;
    try {
      Context initialContext = new InitialContext();
      ret = initialContext.lookup(DS_JDNI);
      if (ret instanceof DataSource) {
        return (DataSource) ret;
      }
      throw new IllegalArgumentException("Cannot resolve JDNI: " + DS_JDNI);
    } catch (NamingException ex) {
      throw new IllegalArgumentException("Cannot resolve JDNI: " + DS_JDNI + ": " + ex.getMessage(), ex);
    }
  }

  @Bean
  public static FactoryBean<Session> hibernateSession()
  {
    return new JpaToSessionSpringBeanFactory();
  }

  @Bean
  public static FactoryBean<SessionFactory> sessionFactory()
  {
    return new JpaToSessionFactorySpringBeanFactory()
    {

      @Override
      protected EntityManagerFactory getEntityManagerFactory()
      {
        return PfEmgrFactory.get().getEntityManagerFactory();
      }
    };

  }

  /**
   * has to be defined, otherwise spring creates a LocalContainerEntityManagerFactoryBean, which has no correct
   * sessionFactory.getCurrentSession();.
   * 
   * @return
   */
  @Bean
  public static EntityManagerFactory entityManagerFactory()
  {
    return PfEmgrFactory.get().getEntityManagerFactory();
  }

  @Bean
  public static PfEmgrFactory emgrFactory()
  {
    return PfEmgrFactory.get();
  }

  @Bean
  public static HibernateTransactionManager transactionManager() throws Exception
  {
    HibernateTransactionManager ret = new HibernateTransactionManager(sessionFactory().getObject());
    ret.setAutodetectDataSource(false);
    ret.setDataSource(dataSource());
    return ret;
  }

  @Bean
  public static TransactionTemplate txTemplate() throws Exception
  {
    TransactionTemplate ret = new TransactionTemplate();
    ret.setTransactionManager(transactionManager());
    return ret;
  }

  @Bean
  public static HibernateTemplate hibernateTemplate() throws Exception
  {
    return new HibernateTemplate(sessionFactory().getObject());
  }

  @Bean
  public static AttrSchemaService attrSchemaService()
  {
    return new AttrSchemaServiceSpringBeanImpl();
  }

  @PostConstruct
  public void initEmgrFactory()
  {
    springEmgrFilterBean.registerEmgrFilter(emgrFactory());
    HistoryServiceManager.get().setHistoryService(new HistoryServiceImpl()
    {

      @Override
      public Class<? extends HistoryMasterBaseDO<?, ?>> getHistoryMasterClass()
      {
        return PfHistoryMasterDO.class;
      }

    });
  }

}
