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
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;

import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DayHolder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DayHolderTest
{
  @BeforeClass
  public static void setUp()
  {
    // Needed if this tests runs before the ConfigurationTest.
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void testDayHolder()
  {
    final DayHolder day = new DayHolder();
    assertFields(day);
    final DateHolder dh = day.clone();
    assertFields(dh);
  }

  @Test
  public void isToday()
  {
    final DayHolder day = new DayHolder();
    assertTrue(day.isToday());
  }

  @Test
  public void testGetNumberOfWorkingDays()
  {
    final DayHolder fromDay = new DayHolder();
    fromDay.setDate(2009, Calendar.JANUARY, 1, 0, 0, 0);
    final DayHolder toDay = new DayHolder();
    toDay.setDate(2009, Calendar.JANUARY, 31, 0, 0, 0);
    assertBigDecimal(21, DayHolder.getNumberOfWorkingDays(fromDay, toDay));
    toDay.setDate(2009, Calendar.FEBRUARY, 28, 0, 0, 0);
    assertBigDecimal(41, DayHolder.getNumberOfWorkingDays(fromDay, toDay));
  }

  @Test
  public void testAdd()
  {
    final DayHolder day = new DayHolder();
    day.setDate(2008, Calendar.JANUARY, 1, 0, 0, 0);
    day.add(Calendar.DAY_OF_YEAR, -1);
    assertEquals(day.getYear(), 2007);
    assertEquals(day.getMonth(), Calendar.DECEMBER);
    assertEquals(day.getDayOfMonth(), 31);
  }

  private void assertFields(final DateHolder day)
  {
    assertEquals("Hours of day should be 0", 0, day.getHourOfDay());
    assertEquals("Minutes should be 0", 0, day.getMinute());
    assertEquals("Seconds should be 0", 0, day.getSecond());
    assertEquals("Millis should be 0", 0, day.getMilliSecond());
  }

  private void assertBigDecimal(final double expected, final BigDecimal value)
  {
    assertEquals(expected, value.doubleValue(), 0.00001);
  }
}
