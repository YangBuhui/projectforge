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

package org.projectforge.web.teamcal.event;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.common.DateHelperTest;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.teamcal.TeamCalTestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class TeamEventDaoTestFork extends TeamCalTestHelper
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventDaoTestFork.class);

  @Autowired
  private TeamEventDao teamEventDao;

  private Integer calId;
  private Integer eventId;

  @Test
  public void accessTest()
  {
    final TeamCalDO cal = prepareUsersAndGroups("teamEvent");
    calId = cal.getId();
    logon(getOwner());
    final TeamEventDO event = new TeamEventDO();
    event.setStartDate(new Timestamp(DateHelperTest.createDate(2012, Calendar.DECEMBER, 8, 8, 0, 0, 0).getTime()));
    event.setEndDate(new Timestamp(DateHelperTest.createDate(2012, Calendar.DECEMBER, 8, 15, 0, 0, 0).getTime()));
    event.setSubject("Testing the event dao.");
    event.setAttendees(new TreeSet<TeamEventAttendeeDO>());
    event.getAttendees().add(new TeamEventAttendeeDO().setUrl("k.reinhard@acme.com"));
    event.setLocation("At home").setNote("This is a note.");
    try {
      log.info("Next AccessException is expected:");
      teamEventDao.save(event);
      fail("AccessException expected, no calendar given in event.");
    } catch (final AccessException ex) {
      // OK
    }
    event.setCalendar(cal);
    eventId = teamEventDao.save(event);

    checkSelectAccess(true, getOwner(), getFullUser1(), getFullUser3(),
        getReadonlyUser1(),
        getReadonlyUser3());
    checkSelectAccess(false, getNoAccessUser());

    checkUpdateAccess(event, true, getOwner(), getFullUser1(), getFullUser3());
    checkUpdateAccess(event, false, getReadonlyUser1(), getReadonlyUser3(),
        getMinimalUser1(),
        getMinimalUser3(), getNoAccessUser(), getUser(TEST_ADMIN_USER));
    checkMinimalAccess(eventId, getMinimalUser1(), getMinimalUser3());
  }

  private void checkSelectAccess(final boolean access, final PFUserDO... users)
  {
    for (final PFUserDO user : users) {
      logon(user);
      try {
        assertEquals("Testing the event dao.", teamEventDao.getById(eventId).getSubject());
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

  private void checkUpdateAccess(final TeamEventDO event, final boolean access, final PFUserDO... users)
  {
    for (final PFUserDO user : users) {
      logon(user);
      try {
        event.setSubject("Event of " + user.getUsername());
        teamEventDao.update(event);
        TeamEventDO levent = teamEventDao.getById(eventId);
        Assert.assertNotNull(levent);
        assertEquals("Event of " + user.getUsername(), levent.getSubject());
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

  private void checkMinimalAccess(final Integer eventId, final PFUserDO... users)
  {
    for (final PFUserDO user : users) {
      logon(user);
      final TeamEventDO event = teamEventDao.getById(eventId);
      assertTrue("Field 'attendees' should be null for minimal users.", CollectionUtils.isEmpty(event.getAttendees()));
      assertTrue("Field 'attachments' should be null for minimal users.",
          CollectionUtils.isEmpty(event.getAttachments()));
      assertNull("Field 'subject' should be null for minimal users.", event.getSubject());
      assertNull("Field 'localtion' should be null for minimal users.", event.getLocation());
      assertNull("Field 'note' should be null for minimal users.", event.getNote());
    }
  }

}
