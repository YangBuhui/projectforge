/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.LoginDefaultHandler;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserCache;
import org.projectforge.business.user.UserDao;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.configuration.DatabaseOrmConfiguration;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.registry.Registry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.db.jpa.history.entities.EntityOpType;
import de.micromata.genome.util.jdbc.TraceCreationBasicDataSource;
import de.micromata.genome.util.runtime.LocalSettings;
import de.micromata.genome.util.runtime.LocalSettingsEnv;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@ContextConfiguration(
    classes = { DatabaseOrmConfiguration.class, TestConfiguration.class },
    loader = AnnotationConfigContextLoader.class)
public class AbstractTestBase extends AbstractTestNGSpringContextTests
{
  protected static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(AbstractTestBase.class);

  public static final String ADMIN = "PFAdmin";

  public static final String TEST_ADMIN_USER = "testSysAdmin";

  public static final String TEST_ADMIN_USER_PASSWORD = "testSysAdmin42";

  public static final String TEST_FINANCE_USER = "testFinanceUser";

  public static final String TEST_FULL_ACCESS_USER = "testFullAccessUser";

  public static final String TEST_FULL_ACCESS_USER_PASSWORD = "testFullAccessUser42";

  public static final String TEST_GROUP = "testGroup";

  public static final String TEST_USER = "testUser";

  public static final String TEST_USER_PASSWORD = "testUser42";

  public static final String TEST_USER2 = "testUser2";

  public static final String TEST_DELETED_USER = "deletedTestUser";

  public static final String TEST_DELETED_USER_PASSWORD = "deletedTestUser42";

  public static final String TEST_PROJECT_MANAGER_USER = "testProjectManager";

  public static final String TEST_PROJECT_ASSISTANT_USER = "testProjectAssistant";

  public static final String TEST_CONTROLLING_USER = "testController";

  public static final String TEST_MARKETING_USER = "testMarketingUser";

  public static final String ADMIN_GROUP = ProjectForgeGroup.ADMIN_GROUP.toString();

  public static final String FINANCE_GROUP = ProjectForgeGroup.FINANCE_GROUP.toString();

  public static final String CONTROLLING_GROUP = ProjectForgeGroup.CONTROLLING_GROUP.toString();

  public static final String PROJECT_MANAGER = ProjectForgeGroup.PROJECT_MANAGER.toString();

  public static final String PROJECT_ASSISTANT = ProjectForgeGroup.PROJECT_ASSISTANT.toString();

  public static final String MARKETING_GROUP = ProjectForgeGroup.MARKETING_GROUP.toString();

  public static final String ORGA_GROUP = ProjectForgeGroup.ORGA_TEAM.toString();

  @Autowired
  protected ApplicationContext applicationContext;

  public static PFUserDO ADMIN_USER;

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  @Autowired
  protected UserDao userDao;

  @Autowired
  protected AccessChecker accessChecker;

  @Autowired
  protected InitTestDB initTestDB;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private InitDatabaseDao initDatabaseDao;

  @Autowired
  protected UserCache userCache;

  protected int mCount = 0;

  static {
    LocalSettingsEnv.dataSourceSuplier = () -> new TraceCreationBasicDataSource();
  }

  @BeforeClass
  public void setUp()
  {
    TimeZone.setDefault(DateHelper.UTC);
    log.info("user.timezone is: " + System.getProperty("user.timezone"));
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    try {
      jdbc.execute("CHECKPOINT DEFRAG");
    } catch (final org.springframework.jdbc.BadSqlGrammarException ex) {
      // ignore
    }

    clearDatabase();
    initDatabaseDao.insertDefaultTenant();

    GlobalConfiguration.createConfiguration(configurationService);
    TenantRegistryMap tenantRegistryMap = TenantRegistryMap.getInstance();
    tenantRegistryMap.setApplicationContext(applicationContext);
    ConfigXmlTest.createTestConfiguration();

    if (DatabaseSupport.getInstance() == null) {
      DatabaseSupport.setInstance(new DatabaseSupport(HibernateUtils.getDialect()));
    }
    Registry.getInstance().init(applicationContext);

    try {
      initDb();
    } catch (BeansException e) {
      log.error("Something in setUp go wrong: " + e.getMessage(), e);
    }
  }

  @AfterMethod
  public void dumpOpenJdbcConnections()
  {
    if (LocalSettings.get().getBooleanValue("pf.debug.dumpopends", false) == false) {
      return;
    }
    for (BasicDataSource bd : LocalSettingsEnv.get().getDataSources().values()) {
      if (bd instanceof TraceCreationBasicDataSource) {
        TraceCreationBasicDataSource tbd = (TraceCreationBasicDataSource) bd;
        String dump = tbd.getOpenConnectionsDump();
        if (StringUtils.isNotBlank(dump) == true) {
          log.warn("Still open Connections:\n" + dump);
        }
      }
    }
  }

  protected void initDb()
  {
    init(true);
  }

  /**
   * Init and reinitialise context before each run
   */
  public void init(final boolean createTestData)
  {
    final LoginDefaultHandler loginHandler = applicationContext.getBean(LoginDefaultHandler.class);
    loginHandler.initialize();
    Login.getInstance().setLoginHandler(loginHandler);
    if (createTestData == true) {
      initTestDB.initDatabase();
    }
  }

  protected void clearDatabase()
  {
    log.info("clearDatabase...");
    PfEmgrFactory.get().getJpaSchemaService().clearDatabase();
    TenantRegistryMap.getInstance().setAllUserGroupCachesAsExpired();
    userCache.setExpired();
  }

  public PFUserDO logon(final String username)
  {
    final PFUserDO user = userDao.getInternalByName(username);
    if (user == null) {
      fail("User not found: " + username);
    }
    ThreadLocalUserContext.setUser(userCache, PFUserDO.createCopyWithoutSecretFields(user));
    return user;
  }

  public void logon(final PFUserDO user)
  {
    ThreadLocalUserContext.setUser(userCache, user);
  }

  protected void logoff()
  {
    ThreadLocalUserContext.setUser(userCache, null);
  }

  public GroupDO getGroup(final String groupName)
  {
    return initTestDB.getGroup(groupName);
  }

  public Integer getGroupId(final String groupName)
  {
    return initTestDB.getGroup(groupName).getId();
  }

  public TaskDO getTask(final String taskName)
  {
    return initTestDB.getTask(taskName);
  }

  public PFUserDO getUser(final String userName)
  {
    return initTestDB.getUser(userName);
  }

  public Integer getUserId(final String userName)
  {
    return initTestDB.getUser(userName).getId();
  }

  protected void logStart(final String name)
  {
    logStartPublic(name);
    mCount = 0;
  }

  protected void logEnd()
  {
    logEndPublic();
    mCount = 0;
  }

  protected void logDot()
  {
    log(".");
  }

  protected void log(final String string)
  {
    logPublic(string);
    if (++mCount % 40 == 0) {
      System.out.println("");
    }
  }

  public static void logStartPublic(final String name)
  {
    System.out.print(name + ": ");
  }

  public static void logEndPublic()
  {
    System.out.println(" (OK)");
  }

  public static void logDotPublic()
  {
    logPublic(".");
  }

  public static void logPublic(final String string)
  {
    System.out.print(string);
  }

  public static void logSingleEntryPublic(final String string)
  {
    System.out.println(string);
  }

  protected void assertAccessException(final AccessException ex, final Integer taskId, final AccessType accessType,
      final OperationType operationType)
  {
    assertEquals(accessType, ex.getAccessType());
    assertEquals(operationType, ex.getOperationType());
    assertEquals(taskId, ex.getTaskId());
  }

  @SuppressWarnings("rawtypes")
  protected void assertHistoryEntry(final HistoryEntry entry, final Integer entityId, final PFUserDO user,
      final EntityOpType type)
  {
    assertHistoryEntry(entry, entityId, user, type, null, null, null, null);
  }

  @SuppressWarnings("rawtypes")
  protected void assertHistoryEntry(final HistoryEntry entry, final Integer entityId, final PFUserDO user,
      final EntityOpType type,
      final String propertyName, final Class<?> classType, final Object oldValue, final Object newValue)
  {
    assertEquals(user.getId().toString(), entry.getUserName());
    // assertEquals(AddressDO.class.getSimpleName(), entry.getClassName());
    assertEquals(null, entry.getUserComment());
    assertEquals(type, entry.getEntityOpType());
    assertEquals(entityId, entry.getEntityId());
    if (propertyName != null) {
      fail("TODO HISTORY History not yet implemented");
    }
  }

  protected void assertBigDecimal(final int v1, final BigDecimal v2)
  {
    assertBigDecimal(new BigDecimal(v1), v2);
  }

  protected void assertBigDecimal(final BigDecimal v1, final BigDecimal v2)
  {
    assertTrue("BigDecimal values are not equal.", v1.compareTo(v2) == 0);
  }

  protected Calendar assertUTCDate(final Date date, final int year, final int month, final int day, final int hour,
      final int minute,
      final int second)
  {
    final Calendar cal = Calendar.getInstance(DateHelper.UTC);
    cal.setTime(date);
    assertEquals(year, cal.get(Calendar.YEAR));
    assertEquals(month, cal.get(Calendar.MONTH));
    assertEquals(day, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(hour, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(minute, cal.get(Calendar.MINUTE));
    assertEquals(second, cal.get(Calendar.SECOND));
    return cal;
  }

  protected Calendar assertUTCDate(final Date date, final int year, final int month, final int day, final int hour,
      final int minute,
      final int second, final int millis)
  {
    final Calendar cal = assertUTCDate(date, year, month, day, hour, minute, second);
    assertEquals(millis, cal.get(Calendar.MILLISECOND));
    return cal;
  }

}
