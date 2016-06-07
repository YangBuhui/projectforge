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

package org.projectforge.framework.persistence.database;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.user.UserCache;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import de.micromata.genome.jpa.ConstraintPersistenceException;

public class InitDatabaseDaoWithTestDataTestFork extends AbstractTestBase
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(InitDatabaseDaoWithTestDataTestFork.class);
  // old format.

  @Autowired
  private MyDatabaseUpdateService myDatabaseUpdateService;

  @Autowired
  private InitDatabaseDao initDatabaseDao;

  @Autowired
  private PfJpaXmlDumpService pfJpaXmlDumpService;

  @Autowired
  private AccessDao accessDao;

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private AuftragDao auftragDao;

  @Autowired
  private BookDao bookDao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private UserCache userCache;

  @Autowired
  private TenantDao tenantDao;

  @Autowired
  private TenantService tenantService;

  @Override
  protected void initDb()
  {
    init(false);
  }

  @Test
  public void initializeEmptyDatabase()
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final String testPassword = "demo123";
    TenantRegistryMap.getInstance().setAllUserGroupCachesAsExpired(); // Force reload (because it's may be expired due to previous tests).
    userCache.setExpired(); // Force reload (because it's may be expired due to previous tests).
    assertTrue(myDatabaseUpdateService.databaseTablesWithEntriesExists());
    PFUserDO admin = new PFUserDO();
    admin.setUsername("myadmin");
    userDao.createEncryptedPassword(admin, testPassword);
    pfJpaXmlDumpService.createTestDatabase();
    admin = initDatabaseDao.updateAdminUser(admin, null);
    Set<TenantDO> tenantsToAssign = new HashSet<>();
    tenantsToAssign.add(tenantService.getDefaultTenant());
    tenantDao.internalAssignTenants(admin, tenantsToAssign, null, false, false);
    initDatabaseDao.afterCreatedTestDb(true);
    final PFUserDO initialAdminUser = userDao.authenticateUser("myadmin", testPassword);
    assertNotNull(initialAdminUser);
    assertEquals("myadmin", initialAdminUser.getUsername());
    final Collection<Integer> col = userGroupCache.getUserGroups(initialAdminUser);
    assertEquals(5, col.size());
    assertTrue(userGroupCache.isUserMemberOfAdminGroup(initialAdminUser.getId()));
    assertTrue(userGroupCache.isUserMemberOfFinanceGroup(initialAdminUser.getId()));

    final List<PFUserDO> userList = userDao.internalLoadAll();
    assertTrue(userList.size() > 0);
    for (final PFUserDO user : userList) {
      assertNull("For security reasons the stay-logged-in-key should be null.", user.getStayLoggedInKey());
    }

    final List<GroupTaskAccessDO> accessList = accessDao.internalLoadAll();
    assertTrue(accessList.size() > 0);
    for (final GroupTaskAccessDO access : accessList) {
      assertNotNull("Access entries should be serialized.", access.getAccessEntries());
      assertTrue("Access entries should be serialized.", access.getAccessEntries().size() > 0);
    }

    final List<AddressDO> addressList = addressDao.internalLoadAll();
    assertTrue(addressList.size() > 0);

    final List<BookDO> bookList = bookDao.internalLoadAll();
    assertTrue(bookList.size() > 2);

    final List<TaskDO> taskList = taskDao.internalLoadAll();
    assertTrue(taskList.size() > 10);

    final List<AuftragDO> orderList = auftragDao.internalLoadAll();
    AuftragDO order = null;
    for (final AuftragDO ord : orderList) {
      if (ord.getNummer() == 1) {
        order = ord;
        break;
      }
    }
    assertNotNull("Order #1 not found.", order);
    assertEquals("Order #1 must have 3 order positions.", 3, order.getPositionen().size());

    final List<PfHistoryMasterDO> list = hibernateTemplate.loadAll(PfHistoryMasterDO.class);
    // assertTrue("At least 10 history entries expected: " + list.size(), list.size() >= 10);

    log.error("****> Next exception and error message are OK (part of the test).");
    boolean exception = false;
    admin.setUsername(InitDatabaseDao.DEFAULT_ADMIN_USER);
    try {
      pfJpaXmlDumpService.createTestDatabase();
      initDatabaseDao.updateAdminUser(admin, null);
      initDatabaseDao.afterCreatedTestDb(false);
      fail("AccessException expected.");
    } catch (final AccessException | ConstraintPersistenceException ex) {
      exception = true;
      // Everything fine.
    }
    log.error("Last exception and error messages were OK (part of the test). <****");
    assertTrue(exception);

    log.error("****> Next exception and error message are OK (part of the test).");
    exception = false;
    try {
      pfJpaXmlDumpService.createTestDatabase();
      initDatabaseDao.updateAdminUser(admin, null);
      initDatabaseDao.afterCreatedTestDb(true);
      fail("AccessException expected.");
    } catch (AccessException | ConstraintPersistenceException ex) {
      exception = true;
      // Everything fine.
    }
    log.error("Last exception and error messages were OK (part of the test). <****");
    assertTrue(exception);
  }
}
