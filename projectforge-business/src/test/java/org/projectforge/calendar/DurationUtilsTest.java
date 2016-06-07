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

import org.projectforge.framework.calendar.DurationUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DurationUtilsTest
{
  @Test
  public void formattedHoursAndMinutes()
  {
    Assert.assertEquals("0:00", getFormattedHoursAndMinutes(0, 0));
    Assert.assertEquals("0:01", getFormattedHoursAndMinutes(0, 1));
    Assert.assertEquals("0:59", getFormattedHoursAndMinutes(0, 59));
    Assert.assertEquals("1:00", getFormattedHoursAndMinutes(0, 60));
    Assert.assertEquals("9:59", getFormattedHoursAndMinutes(9, 59));
    Assert.assertEquals("10:59", getFormattedHoursAndMinutes(10, 59));
  }

  private String getFormattedHoursAndMinutes(final int hours, final int minutes)
  {
    return DurationUtils.getFormattedHoursAndMinutes(hours * 3600000 + minutes * 60000);
  }
}
