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

package org.projectforge.business.gantt;

import static org.testng.AssertJUnit.assertEquals;

import org.projectforge.business.gantt.GanttTaskImpl;
import org.testng.annotations.Test;

public class GanttTaskImplTest
{
  @Test
  public void getNextId()
  {
    GanttTaskImpl root = new GanttTaskImpl();
    root.addChild(new GanttTaskImpl(5));
    assertEquals(-1, root.getNextId());
    root.addChild(new GanttTaskImpl(-1));
    assertEquals(-2, root.getNextId());
    root.addChild(new GanttTaskImpl(-5));
    assertEquals(-6, root.getNextId());
    root.addChild(new GanttTaskImpl(-6));
    assertEquals(-7, root.getNextId());
  }
}
