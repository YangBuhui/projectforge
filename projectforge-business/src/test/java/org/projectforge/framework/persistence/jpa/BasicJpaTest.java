package org.projectforge.framework.persistence.jpa;

import java.util.List;

import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import de.micromata.genome.util.runtime.LocalSettings;
import de.micromata.genome.util.runtime.LocalSettingsEnv;

public class BasicJpaTest
{
  static LocalSettingsEnv localSettingsEnv;

  @BeforeClass
  public static void initLs()
  {
    LocalSettings ls = LocalSettings.get();
    prepareJndi();
  }

  protected static void prepareJndi()
  {
    //    JndiMockupNamingContextBuilder contextBuilder = new JndiMockupNamingContextBuilder();
    //    Hashtable<String, Object> env = new Hashtable<String, Object>();
    //    InitialContextFactory initialContextFactory = contextBuilder.createInitialContextFactory(env);
    //    try {
    //      Context initialContext = initialContextFactory.getInitialContext(env);
    //      localSettingsEnv = new LocalSettingsEnv(initialContext);
    //      contextBuilder.activate();
    //
    //    } catch (NamingException ex) {
    //      throw new RuntimeException(ex);
    //    }
  }

  @Test
  public void testJpa()
  {
    List<PFUserDO> res = PfEmgrFactory.get().runInTrans((emgr) -> {
      List<PFUserDO> ret = emgr.selectDetached(PFUserDO.class, "select e from " + PFUserDO.class.getName() + " e");
      return ret;
    });
  }
}
