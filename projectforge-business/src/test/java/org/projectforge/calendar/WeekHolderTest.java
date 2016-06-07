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
import java.util.Locale;

import org.projectforge.framework.calendar.WeekHolder;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WeekHolderTest
{
  //private final static Logger log = Logger.getLogger(WeekHolderTest.class);

  @BeforeClass
  public static void setUp()
  {
    // Needed if this tests runs before the ConfigurationTest.
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void testWeekHolder()
  {
    final Calendar cal = Calendar.getInstance(Locale.GERMAN);
    WeekHolder week = new WeekHolder(cal);
    assertEquals(7, week.getDays().length);
    assertEquals(2, week.getDays()[0].getDayOfWeek());
    assertEquals("monday", week.getDays()[0].getDayKey());
    final DateHolder dateHolder = new DateHolder(DatePrecision.DAY, Locale.GERMAN);
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    week = new WeekHolder(dateHolder.getCalendar());
    assertEquals(7, week.getDays().length);
    assertEquals(2, week.getDays()[0].getDayOfWeek());
    assertEquals("monday", week.getDays()[0].getDayKey());
    assertEquals("sunday", week.getDays()[6].getDayKey());
    assertEquals(16, week.getDays()[0].getDayOfMonth());
    assertEquals("saturday", week.getDays()[5].getDayKey());
    assertEquals(21, week.getDays()[5].getDayOfMonth());
    dateHolder.setDate(2007, Calendar.MARCH, 1, 4, 50, 23);
    assertEquals(Calendar.MARCH, dateHolder.getMonth());
    week = new WeekHolder(dateHolder.getCalendar(), dateHolder.getMonth());
    assertEquals("monday", week.getDays()[0].getDayKey());
    assertEquals(26, week.getDays()[0].getDayOfMonth());
    assertEquals(true, week.getDays()[0].isMarker()); // February, 26
    assertEquals(true, week.getDays()[1].isMarker()); // February, 27
    assertEquals(true, week.getDays()[2].isMarker()); // February, 28
    assertEquals(1, week.getDays()[3].getDayOfMonth());
    assertEquals("Day is not of current month and should be marked.", false, week.getDays()[3].isMarker()); // March, 1
  }
}
