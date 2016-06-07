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

package org.projectforge.web.teamcal.admin;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.teamcal.TeamCalTestHelper;
import org.testng.annotations.Test;

public class TeamCalDaoTestFork extends TeamCalTestHelper
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalDaoTestFork.class);

  private Integer calId;

  @Test
  public void accessTest()
  {
    final TeamCalDO cal = prepareUsersAndGroups("teamCal");
    calId = cal.getId();
    logon(getOwner());
    assertEquals("teamCal.title", teamCalDao.getById(calId).getTitle());
    checkSelectAccess(true, getOwner(), getFullUser1(), getFullUser3(),
        getReadonlyUser1(),
        getReadonlyUser3(), getMinimalUser1(), getMinimalUser3());
    checkSelectAccess(false, getNoAccessUser());

    checkUpdateAccess(cal, true, getOwner(), getUser(TEST_ADMIN_USER));
    checkUpdateAccess(cal, false, getFullUser1(), getFullUser3(), getReadonlyUser1(),
        getReadonlyUser3(), getMinimalUser1(), getMinimalUser3(),
        getNoAccessUser());
  }

  private void checkSelectAccess(final boolean access, final PFUserDO... users)
  {
    for (final PFUserDO user : users) {
      logon(user);
      try {
        assertEquals("teamCal.title", teamCalDao.getById(calId).getTitle());
        if (access == false) {
          fail("Select-AccessException expected for user: " + user.getUsername());
        }
      } catch (final AccessException ex) {
        if (access == true) {
          fail("Unexpected Selected-AccessException for user: " + user.getUsername());
        } else {
          log.info("Last AccessException was expected (OK).");
        }
      }
    }
  }

  private void checkUpdateAccess(final TeamCalDO cal, final boolean access, final PFUserDO... users)
  {
    for (final PFUserDO user : users) {
      logon(user);
      try {
        cal.setTitle("Calendar of " + user.getUsername());
        teamCalDao.update(cal);
        assertEquals("Calendar of " + user.getUsername(), teamCalDao.getById(calId).getTitle());
        if (access == false) {
          fail("Update-AccessException expected for user: " + user.getUsername());
        }
      } catch (final AccessException ex) {
        if (access == true) {
          fail("Unexpected Update-AccessException for user: " + user.getUsername());
        } else {
          log.info("Last AccessException was expected (OK).");
        }
      }
    }
  }
}
