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

package org.projectforge.calendar;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.projectforge.framework.calendar.MonthHolder;
import org.projectforge.framework.calendar.WeekHolder;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.AbstractTestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MonthHolderTest extends AbstractTestBase
{

  @Test
  public void testMonthHolder()
  {
    ThreadLocalUserContext.setUser(userCache,
        new PFUserDO().setLocale(Locale.GERMAN).setTimeZone(DateHelper.EUROPE_BERLIN));
    final DateHolder date = new DateHolder(new Date(), DatePrecision.DAY, Locale.GERMAN);
    date.setDate(1970, Calendar.NOVEMBER, 21, 0, 0, 0);
    final MonthHolder month = new MonthHolder(date.getDate());
    assertEquals(6, month.getWeeks().size());
    WeekHolder week = month.getFirstWeek();
    assertEquals("monday", week.getDays()[0].getDayKey());
    assertEquals(26, week.getDays()[0].getDayOfMonth());
    assertEquals(Calendar.OCTOBER, week.getDays()[0].getMonth());
    assertEquals("Day is marked, because it is not part of the month.", true, week.getDays()[0].isMarker());
    week = month.getWeeks().get(5);
    assertEquals("monday", week.getDays()[0].getDayKey());
    assertEquals(30, week.getDays()[0].getDayOfMonth());
    assertEquals("Day is not marked, because it is part of the month.", false, week.getDays()[0].isMarker());
    assertEquals(6, week.getDays()[6].getDayOfMonth());
    assertEquals("Day is marked, because it is not part of the month.", true, week.getDays()[6].isMarker());
    assertEquals(Calendar.DECEMBER, week.getDays()[6].getMonth());
  }

  @Test
  public void testNumberOfWorkingDays()
  {
    final DateHolder date = new DateHolder(new Date(), DatePrecision.DAY, Locale.GERMAN);
    date.setDate(2009, Calendar.JANUARY, 16, 0, 0, 0);
    MonthHolder month = new MonthHolder(date.getDate());
    assertBigDecimal(21, month.getNumberOfWorkingDays());
    date.setDate(2009, Calendar.FEBRUARY, 16, 0, 0, 0);
    month = new MonthHolder(date.getDate());
    assertBigDecimal(20, month.getNumberOfWorkingDays());
    date.setDate(2009, Calendar.NOVEMBER, 16, 0, 0, 0);
    month = new MonthHolder(date.getDate());
    assertBigDecimal(21, month.getNumberOfWorkingDays());
    date.setDate(2009, Calendar.DECEMBER, 16, 0, 0, 0);
    month = new MonthHolder(date.getDate());
    assertBigDecimal(21, month.getNumberOfWorkingDays());
  }

  @Test
  public void testDays()
  {
    final MonthHolder mh = new MonthHolder(2013, Calendar.MAY);
    final List<DayHolder> list = mh.getDays();
    Assert.assertEquals(31, list.size());
    for (final DayHolder dh : list) {
      Assert.assertEquals(Calendar.MAY, dh.getMonth());
    }
    Assert.assertEquals(1, list.get(0).getDayOfMonth());
    Assert.assertEquals(31, list.get(30).getDayOfMonth());
  }

}
