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
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.projectforge.business.gantt.GanttChartDO;
import org.projectforge.business.gantt.GanttChartDao;
import org.projectforge.business.gantt.GanttChartData;
import org.projectforge.business.gantt.GanttTask;
import org.projectforge.business.gantt.GanttTaskImpl;
import org.projectforge.business.gantt.Task2GanttTaskConverter;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class GanttChartTest extends AbstractTestBase
{
  @Autowired
  private GanttChartDao ganttChartDao;

  @Autowired
  private TaskDao taskDao;

  @Test
  public void testReadWriteGanttObjects()
  {
    logon(TEST_ADMIN_USER);
    final String prefix = "GantChartTest";
    final TaskTree taskTree = taskDao.getTaskTree();
    final TaskDO rootTask = initTestDB.addTask(prefix, "root");
    final DateHolder dh = new DateHolder();
    dh.setDate(2010, Calendar.AUGUST, 3);
    taskDao.update(initTestDB.addTask(prefix + "1", prefix).setStartDate(dh.getDate()).setDuration(BigDecimal.TEN));
    initTestDB.addTask(prefix + "1.1", prefix + "1");
    taskDao.update(initTestDB.addTask(prefix + "2", prefix).setGanttPredecessor(getTask(prefix + "1"))
        .setDuration(BigDecimal.ONE));
    taskDao.update(initTestDB.addTask(prefix + "3", prefix).setGanttPredecessor(getTask(prefix + "2"))
        .setGanttPredecessorOffset(10)
        .setDuration(BigDecimal.TEN));
    final GanttChartData data = Task2GanttTaskConverter.convertToGanttObjectTree(taskTree, rootTask);
    final GanttTask rootObject = data.getRootObject();
    final GanttChartDO ganttChartDO = new GanttChartDO().setTask(rootTask);
    ganttChartDao.writeGanttObjects(ganttChartDO, rootObject);
    assertEquals("No output because there is no further information in the GanttObject tree.", "",
        ganttChartDO.getGanttObjectsAsXml());
    findById(rootObject, getTask(prefix + "2").getId()).setPredecessorOffset(5).setDuration(new BigDecimal("12"));
    findById(rootObject, getTask(prefix + "1.1").getId()).setDuration(new BigDecimal("2"));
    ganttChartDao.writeGanttObjects(ganttChartDO, rootObject);
    String xml = transform(prefix, "<ganttObject id='{}'>"
        + "<children>"
        + "<ganttObject id='{1}'>"
        + "<children>"
        + "<ganttObject id='{1.1}' duration='2'/>"
        + "</children>"
        + "</ganttObject>"
        + "<ganttObject id='{2}' predecessorOffset='5' duration='12'/></children></ganttObject>");
    assertEquals("3 has no further information.", xml, ganttChartDO.getGanttObjectsAsXml());

    GanttTask ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
    assertEquals(xml, ganttChartDO.getGanttObjectsAsXml());
    assertEquals("duration", BigDecimal.TEN, findById(ganttObject, getTask(prefix + "1").getId()).getDuration());
    assertEquals("startDate", dh.getDate(), findById(ganttObject, getTask(prefix + "1").getId()).getStartDate());

    initTestDB.addTask(prefix + "II", "root");
    taskDao.update(getTask(prefix + "1.1").setParentTask(getTask(prefix))); // One level higher
    taskDao.update(getTask(prefix + "2").setParentTask(getTask(prefix + "II"))); // Moved anywhere.
    taskDao.update(getTask(prefix + "3").setParentTask(getTask(prefix + "II"))); // Moved anywhere.

    ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
    assertEquals("1 has no further information, 2 and 3 are moved anywhere.", transform(prefix, "<ganttObject id='{}'>"
        + "<children>"
        + "<ganttObject id='{1.1}' duration='2'/>"
        + "</children>"
        + "</ganttObject>"), ganttChartDO.getGanttObjectsAsXml());
    findById(ganttObject, getTask(prefix + "1").getId()).setStartDate(null);
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
    ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
    assertNull("Start date should be stored as null (start date of task is set).",
        findById(ganttObject, getTask(prefix + "1").getId())
            .getStartDate());
    findById(ganttObject, getTask(prefix + "1").getId()).addChild(new GanttTaskImpl(-1).setTitle("Child of 1"));
    findById(ganttObject, getTask(prefix + "1.1").getId()).addChild(
        new GanttTaskImpl(-2).setTitle("Child of 1.1").addChild(new GanttTaskImpl(-3).setTitle("Grand child of 1.1")));
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
    xml = transform(prefix, "<ganttObject id='{}'>"
        + "<children>"
        + "<ganttObject id='{1}' startDate='null'>"
        + "<children>"
        + "<ganttObject id='-1'><title>Child of 1</title></ganttObject>"
        + "</children>"
        + "</ganttObject>"
        + "<ganttObject id='{1.1}' duration='2'>"
        + "<children>"
        + "<ganttObject id='-2'><title>Child of 1.1</title>"
        + "<children>"
        + "<ganttObject id='-3'><title>Grand child of 1.1</title></ganttObject>"
        + "</children>"
        + "</ganttObject>"
        + "</children>"
        + "</ganttObject>"
        + "</children>"
        + "</ganttObject>");
    assertEquals("Test with activities not related to tasks.", xml, ganttChartDO.getGanttObjectsAsXml());
    ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
    assertEquals("Read-write-cycle (identity check).", xml, ganttChartDO.getGanttObjectsAsXml());
  }

  @Test
  public void testIgnoringOfNumberFields()
  {
    logon(TEST_ADMIN_USER);
    final String prefix = "GanttTest3";
    final TaskTree taskTree = taskDao.getTaskTree();
    final TaskDO rootTask = initTestDB.addTask(prefix, "root");
    final Integer id1 = addTask(prefix + "1", null, null);
    final Integer id2 = addTask(prefix + "2", null, null);
    final Integer id3 = addTask(prefix + "3", BigDecimal.TEN, 10);
    final Integer id4 = addTask(prefix + "4", BigDecimal.TEN, 10);
    final Integer id5 = addTask(prefix + "5", BigDecimal.TEN, 10);
    // final Integer id3 = task.getId();
    final GanttChartData data = Task2GanttTaskConverter.convertToGanttObjectTree(taskTree, rootTask);
    final GanttTask rootObject = data.getRootObject();
    final GanttChartDO ganttChartDO = new GanttChartDO().setTask(rootTask);
    findById(rootObject, id1).setDuration(new BigDecimal("10.000")).setProgress(10); // Modified.
    findById(rootObject, id3).setDuration(new BigDecimal("2")).setProgress(2); // Modified.
    findById(rootObject, id4).setDuration(new BigDecimal("10.000")).setProgress(10); // Unmodified
    findById(rootObject, id5).setDuration(null).setProgress(null); // Modified
    ganttChartDao.writeGanttObjects(ganttChartDO, rootObject);
    final String xml = transform(prefix, "<ganttObject id='{}'>"
        + "<children>"
        + "<ganttObject id='{1}' duration='10.000' progress='10'/>"
        + "<ganttObject id='{3}' duration='2' progress='2'/>"
        + "<ganttObject id='{5}' duration='null' progress='null'/>"
        + "</children>"
        + "</ganttObject>");
    assertEquals("check xml output.", xml, ganttChartDO.getGanttObjectsAsXml());
    GanttTask ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
    assertDurationAndProgress(ganttObject, id1, BigDecimal.TEN, 10);
    assertDurationAndProgress(ganttObject, id2, null, null);
    assertDurationAndProgress(ganttObject, id3, new BigDecimal("2"), 2);
    assertDurationAndProgress(ganttObject, id4, BigDecimal.TEN, 10);
    assertDurationAndProgress(ganttObject, id5, null, null);
  }

  private void assertDurationAndProgress(final GanttTask root, final Integer id, final BigDecimal expectedDuration,
      final Integer expectedProgress)
  {
    final GanttTask task = root.findById(id);
    assertTrue(NumberHelper.isEqual(expectedDuration, task.getDuration()));
    if (expectedProgress == null) {
      assertNull(task.getProgress());
    } else {
      assertEquals(expectedProgress, task.getProgress());
    }
  }

  private Integer addTask(final String name, final BigDecimal duration, final Integer progress)
  {
    final TaskDO task = initTestDB.addTask(name, "GanttTest3");
    task.setDuration(duration).setProgress(progress);
    taskDao.update(task);
    return task.getId();
  }

  private GanttTaskImpl findById(final GanttTask ganttObject, final Serializable id)
  {
    return (GanttTaskImpl) ((GanttTaskImpl) ganttObject).findById(id);
  }

  private String transform(final String prefix, final String str)
  {
    final String text = str.replace('\'', '"');
    final Pattern p = Pattern.compile("\\{([0-9\\.]*)\\}", Pattern.MULTILINE);
    final StringBuffer buf = new StringBuffer();
    final Matcher m = p.matcher(text);
    while (m.find()) {
      if (m.group(1) != null) {
        final TaskDO task = getTask(prefix + m.group(1));
        if (task != null) {
          m.appendReplacement(buf, String.valueOf(task.getId()));
        } else {
          m.appendReplacement(buf, "*** task " + m.group(1) + " not-found***");
        }
      }
    }
    m.appendTail(buf);
    return buf.toString();
  }

}
