package org.projectforge.framework.persistence.jpa;

import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.cfg.AvailableSettings;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import de.micromata.genome.db.jpa.history.api.HistoryService;
import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.jpa.EmgrTx;
import de.micromata.mgc.jpa.hibernatesearch.api.SearchEmgrFactory;

/**
 * A factory for creating PfEmgr objects.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class PfEmgrFactory extends SearchEmgrFactory<PfEmgr>
{

  /**
   * The instance.
   */
  private static PfEmgrFactory INSTANCE;

  /**
   * Gets the.
   *
   * @return the pf emgr factory
   */
  public static synchronized PfEmgrFactory get()
  {
    if (INSTANCE != null) {
      return INSTANCE;
    }
    INSTANCE = new PfEmgrFactory();
    return INSTANCE;

  }

  @Override
  protected void registerEvents()
  {
    super.registerEvents();
    HistoryService historyService = HistoryServiceManager.get().getHistoryService();
    historyService.registerEmgrListener(this);
    historyService.registerStandardHistoryPropertyConverter(this);
  }

  /**
   * Instantiates a new pf emgr factory.
   */
  protected PfEmgrFactory()
  {
    super("org.projectforge.webapp");
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  protected PfEmgr createEmgr(EntityManager entityManager, EmgrTx<PfEmgr> emgrTx)
  {
    return new PfEmgr(entityManager, this, emgrTx);
  }

  @Override
  public String getCurrentUserId()
  {
    Integer userId = ThreadLocalUserContext.getUserId();
    if (userId != null) {
      return userId.toString();
    }
    return "anon";
  }

  @Override
  protected Map<String, Object> getInitEntityManagerFactoryProperties()
  {
    Map<String, Object> properties = super.getInitEntityManagerFactoryProperties();
    //    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    //    properties.put(AvailableSettings.CLASSLOADERS, Collections.singletonList(contextClassLoader));
    // this is PF workound
    properties.put(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, true);
    return properties;
  }

}
