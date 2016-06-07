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

package org.projectforge.web.teamcal;

import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.InitTestDB;
import org.projectforge.web.wicket.WicketPageTestBase;
import org.springframework.beans.factory.annotation.Autowired;

public class TeamCalTestHelper extends WicketPageTestBase
{
  private PFUserDO owner, fullUser1, fullUser2, fullUser3, readonlyUser1, readonlyUser2, readonlyUser3, minimalUser1,
      minimalUser2,
      minimalUser3, noAccessUser;

  private GroupDO fullGroup1, readonlyGroup1, minimalGroup1;

  @Autowired
  private InitTestDB initTestDB;

  @Autowired
  protected TeamCalDao teamCalDao;

  protected TeamCalDO prepareUsersAndGroups(final String prefix)
  {
    logon(TEST_ADMIN_USER);
    owner = initTestDB.addUser(prefix + "OwnerUser");
    fullUser1 = initTestDB.addUser(prefix + "FullUser1");
    fullUser2 = initTestDB.addUser(prefix + "FullUser2");
    fullUser3 = initTestDB.addUser(prefix + "FullUser3");
    readonlyUser1 = initTestDB.addUser(prefix + "ReadonlyUser1");
    readonlyUser2 = initTestDB.addUser(prefix + "ReadonlyUser2");
    readonlyUser3 = initTestDB.addUser(prefix + "ReadonlyUser3");
    minimalUser1 = initTestDB.addUser(prefix + "MinimalUser1");
    minimalUser2 = initTestDB.addUser(prefix + "MinimalUser2");
    minimalUser3 = initTestDB.addUser(prefix + "MinimalUser3");
    noAccessUser = initTestDB.addUser(prefix + "NoAccessUser");

    fullGroup1 = initTestDB.addGroup(prefix + "FullGroup1", fullUser1.getUsername());
    readonlyGroup1 = initTestDB.addGroup(prefix + "ReadonlyGroup1", readonlyUser1.getUsername());
    minimalGroup1 = initTestDB.addGroup(prefix + "MinimalGroup", minimalUser1.getUsername());

    logon(owner);
    final TeamCalDO cal = new TeamCalDO();
    cal.setOwner(owner);
    cal.setFullAccessGroupIds("" + fullGroup1.getId());
    cal.setReadonlyAccessGroupIds("" + readonlyGroup1.getId());
    cal.setMinimalAccessGroupIds("" + minimalGroup1.getId());
    cal.setFullAccessUserIds("" + fullUser3.getId());
    cal.setReadonlyAccessUserIds("" + readonlyUser3.getId());
    cal.setMinimalAccessUserIds("" + minimalUser3.getId());
    cal.setTitle(prefix + ".title");
    final Integer calId = (Integer) teamCalDao.save(cal);
    return teamCalDao.getById(calId);
  }

  protected PFUserDO getOwner()
  {
    return owner;
  }

  protected PFUserDO getFullUser1()
  {
    return fullUser1;
  }

  protected PFUserDO getFullUser2()
  {
    return fullUser2;
  }

  protected PFUserDO getFullUser3()
  {
    return fullUser3;
  }

  protected PFUserDO getReadonlyUser1()
  {
    return readonlyUser1;
  }

  protected PFUserDO getReadonlyUser2()
  {
    return readonlyUser2;
  }

  protected PFUserDO getReadonlyUser3()
  {
    return readonlyUser3;
  }

  protected PFUserDO getMinimalUser1()
  {
    return minimalUser1;
  }

  protected PFUserDO getMinimalUser2()
  {
    return minimalUser2;
  }

  protected PFUserDO getMinimalUser3()
  {
    return minimalUser3;
  }

  protected PFUserDO getNoAccessUser()
  {
    return noAccessUser;
  }

  protected GroupDO getFullGroup1()
  {
    return fullGroup1;
  }

  protected GroupDO getReadonlyGroup1()
  {
    return readonlyGroup1;
  }

  protected GroupDO getMinimalGroup1()
  {
    return minimalGroup1;
  }
}
