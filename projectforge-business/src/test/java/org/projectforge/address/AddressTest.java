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

package org.projectforge.address;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Order;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.InstantMessagingType;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class AddressTest extends AbstractTestBase
{
  private final static Logger log = Logger.getLogger(AddressTest.class);

  @Autowired
  private AddressDao addressDao;

  @Test
  public void testSaveAndUpdate()
  {
    logon(ADMIN);
    AddressDO a1 = new AddressDO();
    a1.setName("Kai Reinhard");
    a1.setTask(getTask("1.1"));
    addressDao.save(a1);
    log.debug(a1);

    a1.setName("Hurzel");
    addressDao.setTask(a1, getTask("1.2").getId());
    addressDao.update(a1);
    assertEquals("Hurzel", a1.getName());

    AddressDO a2 = addressDao.getById(a1.getId());
    assertEquals("Hurzel", a2.getName());
    assertEquals(getTask("1.2").getId(), a2.getTaskId());
    a2.setName("Micromata GmbH");
    addressDao.setTask(a2, getTask("1").getId());
    addressDao.update(a2);
    log.debug(a2);

    AddressDO a3 = addressDao.getById(a1.getId());
    assertEquals("Micromata GmbH", a3.getName());
    assertEquals(getTask("1").getId(), a3.getTaskId());
    log.debug(a3);
  }

  @Test
  public void testDeleteAndUndelete()
  {
    logon(ADMIN);
    AddressDO a1 = new AddressDO();
    a1.setName("Test");
    a1.setTask(getTask("1.1"));
    addressDao.save(a1);

    Integer id = a1.getId();
    a1 = addressDao.getById(id);
    addressDao.markAsDeleted(a1);
    a1 = addressDao.getById(id);
    assertEquals("Should be marked as deleted.", true, a1.isDeleted());

    addressDao.undelete(a1);
    a1 = addressDao.getById(id);
    assertEquals("Should be undeleted.", false, a1.isDeleted());
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testDelete()
  {
    AddressDO a1 = new AddressDO();
    a1.setName("Not deletable");
    a1.setTask(getTask("1.1"));
    addressDao.save(a1);
    Integer id = a1.getId();
    a1 = addressDao.getById(id);
    addressDao.delete(a1);
  }
  // TODO HISTORY
  //  @Test
  //  public void testHistory()
  //  {
  //    PFUserDO user = getUser(AbstractTestBase.ADMIN);
  //    logon(user.getUsername());
  //    AddressDO a1 = new AddressDO();
  //    a1.setName("History test");
  //    a1.setTask(getTask("1.1"));
  //    addressDao.save(a1);
  //    Integer id = a1.getId();
  //    a1.setName("History 2");
  //    addressDao.update(a1);
  //    HistoryEntry[] historyEntries = addressDao.getHistoryEntries(a1);
  //    assertEquals(2, historyEntries.length);
  //    HistoryEntry entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, "name", String.class, "History test", "History 2");
  //    entry = historyEntries[1];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.INSERT, null, null, null, null);
  //
  //    a1.setTask(getTask("1.2"));
  //    addressDao.update(a1);
  //    historyEntries = addressDao.getHistoryEntries(a1);
  //    assertEquals(3, historyEntries.length);
  //    entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.1").getId(),
  //        getTask("1.2").getId());
  //
  //    a1.setTask(getTask("1.1"));
  //    a1.setName("History test");
  //    addressDao.update(a1);
  //    historyEntries = addressDao.getHistoryEntries(a1);
  //    assertEquals(4, historyEntries.length);
  //    entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, null, null, null, null);
  //    List<PropertyDelta> delta = entry.getDelta();
  //    assertEquals(2, delta.size());
  //    for (int i = 0; i < 2; i++) {
  //      PropertyDelta prop = delta.get(0);
  //      if ("name".equals(prop.getPropertyName()) == true) {
  //        assertPropertyDelta(prop, "name", String.class, "History 2", "History test");
  //      } else {
  //        assertPropertyDelta(prop, "task", TaskDO.class, getTask("1.2").getId(), getTask("1.1").getId());
  //      }
  //    }
  //
  //    List<SimpleHistoryEntry> list = addressDao.getSimpleHistoryEntries(a1);
  //    assertEquals(5, list.size());
  //    for (int i = 0; i < 2; i++) {
  //      SimpleHistoryEntry se = list.get(i);
  //      if ("name".equals(se.getPropertyName()) == true) {
  //        assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "name", String.class, "History 2", "History test");
  //      } else {
  //        assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.2").getId(),
  //            getTask("1.1").getId());
  //      }
  //    }
  //    SimpleHistoryEntry se = list.get(2);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.1").getId(),
  //        getTask("1.2").getId());
  //    se = list.get(3);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "name", String.class, "History test", "History 2");
  //    se = list.get(4);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.INSERT, null, null, null, null);
  //
  //    a1 = addressDao.getById(a1.getId());
  //    Date date = a1.getLastUpdate();
  //    String oldName = a1.getName();
  //    a1.setName("Micromata GmbH");
  //    a1.setName(oldName);
  //    addressDao.update(a1);
  //    a1 = addressDao.getById(a1.getId());
  //    list = addressDao.getSimpleHistoryEntries(a1);
  //    assertEquals(5, list.size());
  //    assertEquals(date, a1.getLastUpdate()); // Fails: Fix AbstractBaseDO.copyDeclaredFields: ObjectUtils.equals(Boolean, boolean) etc.
  //  }

  @Test
  public void checkStandardAccess()
  {
    AddressDO a1 = new AddressDO();
    a1.setName("testa1");
    a1.setTask(getTask("ta_1_siud"));
    addressDao.internalSave(a1);
    AddressDO a2 = new AddressDO();
    a2.setName("testa2");
    a2.setTask(getTask("ta_2_siux"));
    addressDao.internalSave(a2);
    AddressDO a3 = new AddressDO();
    a3.setName("testa3");
    a3.setTask(getTask("ta_3_sxxx"));
    addressDao.internalSave(a3);
    AddressDO a4 = new AddressDO();
    a4.setName("testa4");
    a4.setTask(getTask("ta_4_xxxx"));
    addressDao.internalSave(a4);
    logon(AbstractTestBase.TEST_USER);

    // Select
    try {
      addressDao.getById(a4.getId());
      fail("User has no access to select");
    } catch (AccessException ex) {
      assertAccessException(ex, getTask("ta_4_xxxx").getId(), AccessType.TASKS, OperationType.SELECT);
    }
    AddressDO address = addressDao.getById(a3.getId());
    assertEquals("testa3", address.getName());

    // Select filter
    BaseSearchFilter searchFilter = new BaseSearchFilter();
    searchFilter.setSearchString("testa*");
    QueryFilter filter = new QueryFilter(searchFilter);
    filter.addOrder(Order.asc("name"));
    List<AddressDO> result = addressDao.getList(filter);
    assertEquals("Should found 3 address'.", 3, result.size());
    HashSet<String> set = new HashSet<String>();
    set.add("testa1");
    set.add("testa2");
    set.add("testa3");
    assertTrue("Hit first entry", set.remove(result.get(0).getName()));
    assertTrue("Hit second entry", set.remove(result.get(1).getName()));
    assertTrue("Hit third entry", set.remove(result.get(2).getName()));
    // test_a4 should not be included in result list (no select access)

    // Insert
    address = new AddressDO();
    address.setName("test");
    addressDao.setTask(address, getTask("ta_4_xxxx").getId());
    try {
      addressDao.save(address);
      fail("User has no access to insert");
    } catch (AccessException ex) {
      assertAccessException(ex, getTask("ta_4_xxxx").getId(), AccessType.TASKS, OperationType.INSERT);
    }
    addressDao.setTask(address, getTask("ta_1_siud").getId());
    addressDao.save(address);
    assertEquals("test", address.getName());

    // Update
    a3.setName("test_a3test");
    try {
      addressDao.update(a3);
      fail("User has no access to update");
    } catch (AccessException ex) {
      assertAccessException(ex, getTask("ta_3_sxxx").getId(), AccessType.TASKS, OperationType.UPDATE);
    }
    a2.setName("testa2test");
    addressDao.update(a2);
    address = addressDao.getById(a2.getId());
    assertEquals("testa2test", address.getName());
    a2.setName("testa2");
    addressDao.update(a2);
    address = addressDao.getById(a2.getId());
    assertEquals("testa2", address.getName());

    // Update with moving in task hierarchy
    a2.setName("testa2test");
    addressDao.setTask(a2, getTask("ta_1_siud").getId());
    try {
      addressDao.update(a2);
      fail("User has no access to update");
    } catch (AccessException ex) {
      assertAccessException(ex, getTask("ta_2_siux").getId(), AccessType.TASKS, OperationType.DELETE);
    }
    a2 = addressDao.getById(a2.getId());
    a1.setName("testa1test");
    addressDao.setTask(a1, getTask("ta_5_sxux").getId());
    try {
      addressDao.update(a1);
      fail("User has no access to update");
    } catch (AccessException ex) {
      assertAccessException(ex, getTask("ta_5_sxux").getId(), AccessType.TASKS, OperationType.INSERT);
    }
    a1 = addressDao.getById(a1.getId());
    assertEquals("testa1", a1.getName());

    // Delete
    try {
      addressDao.delete(a1);
      fail("Address is historizable and should not be allowed to delete.");
    } catch (RuntimeException ex) {
      assertEquals(true, ex.getMessage().startsWith(AddressDao.EXCEPTION_HISTORIZABLE_NOTDELETABLE));
    }
    try {
      addressDao.markAsDeleted(a2);
      fail("User has no access to delete");
    } catch (AccessException ex) {
      assertAccessException(ex, getTask("ta_2_siux").getId(), AccessType.TASKS, OperationType.DELETE);
    }
  }

  @Test
  public void testInstantMessagingField() throws Exception
  {
    AddressDO address = new AddressDO();
    assertNull(address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.SKYPE, "skype-name");
    assertEquals("SKYPE=skype-name", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.AIM, "aim-id");
    assertEquals("SKYPE=skype-name\nAIM=aim-id", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.YAHOO, "yahoo-name");
    assertEquals("SKYPE=skype-name\nAIM=aim-id\nYAHOO=yahoo-name", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.YAHOO, "");
    assertEquals("SKYPE=skype-name\nAIM=aim-id", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.SKYPE, "");
    assertEquals("AIM=aim-id", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.AIM, "");
    assertNull(address.getInstantMessaging4DB());
  }
}
