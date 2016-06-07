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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.xstream.AliasMap;
import org.projectforge.framework.xstream.ProjectForgeRootElement;
import org.projectforge.framework.xstream.Status;
import org.projectforge.framework.xstream.XmlConstants;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlHelper;
import org.projectforge.framework.xstream.XmlObject;
import org.projectforge.framework.xstream.XmlObjectReader;
import org.projectforge.framework.xstream.XmlObjectWriter;
import org.projectforge.framework.xstream.XmlRegistry;
import org.projectforge.framework.xstream.converter.ISODateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Repository
public class GanttChartDao extends BaseDao<GanttChartDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.PM_GANTT;

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "task.title", "task.taskpath",
      "owner.username",
      "owner.firstname", "owner.lastname" };

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private UserDao userDao;

  private Field[] taskFields = BeanHelper.getDeclaredPropertyFields(TaskDO.class);

  /**
   * Mapping of GanttTask fields to TaskDO fields.
   */
  private Map<String, String> fieldMapping;

  private AliasMap xmlGanttObjectAliasMap;

  public GanttChartDao()
  {
    super(GanttChartDO.class);
    userRightId = USER_RIGHT_ID;
    AccessibleObject.setAccessible(taskFields, true);
    fieldMapping = new HashMap<String, String>();
    fieldMapping.put("predecessor", "ganttPredecessor");
    fieldMapping.put("predecessorOffset", "ganttPredecessorOffset");
    fieldMapping.put("relationType", "ganttRelationType");
    fieldMapping.put("type", "ganttObjectType");
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  protected void onSaveOrModify(final GanttChartDO obj)
  {
    final String styleAsXml = XmlObjectWriter.writeAsXml(obj.getStyle());
    obj.setStyleAsXml(styleAsXml);
    final String settingsAsXml = XmlObjectWriter.writeAsXml(obj.getSettings());
    obj.setSettingsAsXml(settingsAsXml);
  }

  public String exportAsXml(final GanttChart ganttChart)
  {
    return exportAsXml(ganttChart, false);
  }

  @XmlObject(alias = "ProjectForge")
  public class MyRootElement extends ProjectForgeRootElement
  {
    @SuppressWarnings("unused")
    private GanttChart ganttChart;
  }

  public String exportAsXml(final GanttChart ganttChart, final boolean prettyFormat)
  {
    final Document document = DocumentHelper.createDocument();
    final XmlObjectWriter writer = getXmlGanttObjectWriter();
    final XmlRegistry xmlRegistry = new XmlRegistry();
    xmlRegistry.registerConverter(Date.class, new ISODateConverter());
    writer.setXmlRegistry(xmlRegistry);
    final MyRootElement root = new MyRootElement();
    root.ganttChart = ganttChart;
    root.setCreated().setTimeZone(ThreadLocalUserContext.getTimeZone()).setVersion("1.0");
    final Element element = writer.write(document, root);
    // Now, remove all elements with no information from the DOM:
    final String xml;
    if (removeUnnecessaryElements(element) == true) {
      // Nothing to write (no further information in the GanttObject tree given).
      xml = "";
    } else {
      xml = XmlHelper.toString(element, prettyFormat);
    }
    return XmlHelper.XML_HEADER + xml;
  }

  /**
   * Writes all Gantt objects as tree as xml. Writes only those values which are different to the original values of the
   * task with the same id.
   * 
   * @param obj
   * @param rootObject
   */
  public void writeGanttObjects(final GanttChartDO obj, final GanttTask rootObject)
  {
    final Document document = DocumentHelper.createDocument();
    final Element element = getXmlGanttObjectWriter().write(document, rootObject);
    // Now, remove all elements with no information from the DOM:
    final String xml;
    if (removeUnnecessaryElements(element) == true) {
      // Nothing to write (no further information in the GanttObject tree given).
      xml = "";
    } else {
      xml = XmlHelper.toString(element);
    }
    obj.setGanttObjectsAsXml(xml);
  }

  /**
   * Removes all unnecessary GanttObject elements from the DOM (those without any information rather than the id).
   */
  private boolean removeUnnecessaryElements(final Element element)
  {
    if (CollectionUtils.isNotEmpty(element.elements()) == true) {
      for (final Object childObj : element.elements()) {
        final Element child = (Element) childObj;
        if (removeUnnecessaryElements(child) == true) {
          element.remove(child);
        }
      }
    }
    if (CollectionUtils.isNotEmpty(element.elements()) == true) {
      // Element has descendants.
      return false;
    }
    if (StringUtils.isBlank(element.getText()) == false) {
      // Element has non blank content.
      return false;
    }
    // Element has no descendants:
    if (CollectionUtils.isEmpty(element.attributes()) == true) {
      // Element has no attributes.
      return true;
    }
    if ("predecessor".equals(element.getName()) == true) {
      if (element.attribute(XmlObjectWriter.ATTR_ID) != null) {
        // Describes a complete Gantt task which is referenced, so full output is needed.
        return false;
      } else {
        final Attribute idAttr = element.attribute("id");
        final Attribute refIdAttr = element.attribute(XmlObjectWriter.ATTR_REF_ID);
        element.setAttributes(null);
        if (refIdAttr != null) {
          element.addAttribute(XmlObjectWriter.ATTR_REF_ID, refIdAttr.getValue());
        } else if (idAttr != null) {
          // External reference (no ref-id, but task id):
          element.addAttribute("id", idAttr.getValue());
        } else {
          // Should not occur.
          return true;
        }
        return false;
      }
    } else if (element.attributes().size() == 1 && element.attribute("id") != null) {
      // Element has only id attribute and is not a predecessor definition for tasks outside the current Gantt object tree.
      return true;
    }
    return false;
  }

  /**
   * Reads all Gantt objects as tree from xml and TaskTree.
   * 
   * @param obj
   * @return The root object of the read xml data.
   */
  public GanttChartData readGanttObjects(final GanttChartDO obj)
  {
    final TaskTree taskTree = taskDao.getTaskTree();
    final GanttChartData ganttChartData = Task2GanttTaskConverter.convertToGanttObjectTree(taskTree, obj.getTask());
    final XmlObjectReader reader = new XmlObjectReader()
    {
      @Override
      protected Object newInstance(final Class<?> clazz, final Element el, final String attrName,
          final String attrValue)
      {
        if ("predecessor".equals(attrName) == true && XmlConstants.NULL_IDENTIFIER.equals(attrValue) == true) {
          // Field should set to null.
          return Status.IGNORE;
        }
        if (GanttTask.class.isAssignableFrom(clazz) == true) {
          final GanttTask ganttObject = getGanttObject(taskTree, ganttChartData, el);
          if (ganttObject == null) {
            return new GanttTaskImpl(); // Gantt task not related to a ProjectForge task.
          }
          return ganttObject;
        } else if (Collection.class.isAssignableFrom(clazz) == true) {
          final GanttTask ganttObject = getGanttObject(taskTree, ganttChartData, el.getParent());
          if (ganttObject != null && ganttObject.getChildren() != null) {
            return ganttObject.getChildren();
          }
        }
        return null;
      }

      @Override
      protected boolean addCollectionEntry(final Collection<?> col, final Object obj, final Element el)
      {
        if (obj instanceof GanttTask == false) {
          return false;
        }
        final GanttTask ganttTask = (GanttTask) obj;
        if (ganttChartData.findById(ganttTask.getId()) != null) {
          // GanttTask already added to the Gantt object tree.
          return true;
        }
        if (taskTree.getTaskById((Integer) ganttTask.getId()) != null) {
          // External task, so ignore it:
          return true;
        }
        return false;
      }

      @Override
      protected void setField(final Field field, final Object obj, final Object value, final Element element,
          final String key,
          final String attrValue)
      {
        if (XmlConstants.NULL_IDENTIFIER.equals(attrValue) == true) {
          // Overwrite value from task with null.
          setField(field, obj, null);
          return;
        }
        super.setField(field, obj, value, element, key, attrValue);
      }

    };
    reader.setAliasMap(getXmlGanttObjectAliasMap()).setIgnoreEmptyCollections(true);
    reader.read(obj.getGanttObjectsAsXml()); // Ignore the return value. If the task tree has changed, the task tree of root rules.
    return ganttChartData;
  }

  private GanttTask getGanttObject(final TaskTree taskTree, final GanttChartData ganttChartData, final Element el)
  {
    final String idString = el.attributeValue("id");
    final Integer id = NumberHelper.parseInteger(idString);
    GanttTask ganttObject = ganttChartData.findById(id);
    if (ganttObject == null) {
      ganttObject = ganttChartData.ensureAndGetExternalGanttObject(taskTree.getTaskById(id));
    }
    return ganttObject;
  }

  @Override
  public void afterLoad(final GanttChartDO obj)
  {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(GanttChartStyle.class);
    reader.initialize(GanttChartSettings.class);
    final String styleAsXml = obj.getStyleAsXml();
    final GanttChartStyle style;
    if (StringUtils.isEmpty(styleAsXml) == true) {
      style = new GanttChartStyle();
    } else {
      style = (GanttChartStyle) reader.read(styleAsXml);
    }
    obj.setStyle(style);
    final String settingsAsXml = obj.getSettingsAsXml();
    final GanttChartSettings settings;
    if (StringUtils.isEmpty(settingsAsXml) == true) {
      settings = new GanttChartSettings();
    } else {
      settings = (GanttChartSettings) reader.read(settingsAsXml);
    }
    obj.setSettings(settings);
  }

  private AliasMap getXmlGanttObjectAliasMap()
  {
    if (this.xmlGanttObjectAliasMap == null) {
      this.xmlGanttObjectAliasMap = new AliasMap();
      this.xmlGanttObjectAliasMap.put(GanttTaskImpl.class, "ganttObject");
    }
    return this.xmlGanttObjectAliasMap;
  }

  /**
   * Ignores all field values in output which are equal to the values of the corresponding task.
   */
  private XmlObjectWriter getXmlGanttObjectWriter()
  {
    final XmlObjectWriter xmlGanttObjectWriter = new XmlObjectWriter()
    {
      @Override
      protected boolean ignoreField(final Object obj, final Field field)
      {
        if (super.ignoreField(obj, field) == true) {
          return true;
        }
        if (obj instanceof GanttTask) {
          final TaskTree taskTree = taskDao.getTaskTree();
          final String fieldName = field.getName();
          if ("id".equals(fieldName) == true) {
            // Id should always be equals and needed in output for the identification of the gantt object.
            return false;
          }
          if ("description".equals(fieldName) == true) {
            return true;
          }
          final GanttTask ganttObject = (GanttTask) obj;
          final TaskDO task = taskTree.getTaskById((Integer) ganttObject.getId());
          if (task != null) {
            if ("predecessor".equals(field.getName()) == true) {
              // Predecessor unmodified?
              return NumberHelper.isEqual((Integer) ganttObject.getPredecessorId(), task.getGanttPredecessorId());
            }
            String taskFieldname = fieldMapping.get(fieldName);
            if (taskFieldname == null) {
              taskFieldname = fieldName;
            }
            for (final Field taskField : taskFields) {
              if (taskFieldname.equals(taskField.getName()) == true) {
                final Object value = BeanHelper.getFieldValue(obj, field);
                final Object taskValue = BeanHelper.getFieldValue(task, taskField);
                if (value instanceof BigDecimal) {
                  // Needed, because 10.0 is not equal to 10.000 (if scale is different).
                  return NumberHelper.isEqual((BigDecimal) value, (BigDecimal) taskValue);
                }
                return ObjectUtils.equals(value, taskValue) == true;
              }
            }
          }
        }
        return false;
      }

      @Override
      protected void writeField(final Field field, final Object obj, final Object fieldValue, final XmlField annotation,
          final Element element)
      {
        if (GanttTask.class.isAssignableFrom(field.getDeclaringClass()) == true) {
          final String fieldName = field.getName();
          if ("id".equals(fieldName) == false) {
            final TaskTree taskTree = taskDao.getTaskTree();
            final GanttTask ganttObject = (GanttTask) obj;
            final TaskDO task = taskTree.getTaskById((Integer) ganttObject.getId());
            if (task != null) {
              String taskFieldname = fieldMapping.get(fieldName);
              if (taskFieldname == null) {
                taskFieldname = fieldName;
              }
              for (final Field taskField : taskFields) {
                if (taskFieldname.equals(taskField.getName()) == true) {
                  final Object value = BeanHelper.getFieldValue(obj, field);
                  final Object taskValue = BeanHelper.getFieldValue(task, taskField);
                  if (taskValue != null && value == null) {
                    // Reader should interpret this as null, so value from task will be overwritten by null.
                    element.addAttribute(field.getName(), XmlConstants.NULL_IDENTIFIER);
                    return;
                  }
                }
              }
            }
          }
        }
        super.writeField(field, obj, fieldValue, annotation, element);
      }
    };
    xmlGanttObjectWriter.setAliasMap(getXmlGanttObjectAliasMap());
    return xmlGanttObjectWriter;
  }

  /**
   * @param ganttChart
   * @param taskId If null, then task will be set to null;
   * @see TaskTree#getTaskById(Integer)
   */
  public void setTask(final GanttChartDO ganttChart, final Integer taskId)
  {
    final TaskDO task = taskDao.getOrLoad(taskId);
    ganttChart.setTask(task);
  }

  /**
   * @param sheet
   * @param userId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setOwner(final GanttChartDO ganttChart, final Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    ganttChart.setOwner(user);
  }

  @Override
  public GanttChartDO newInstance()
  {
    return new GanttChartDO().setSettings(new GanttChartSettings()).setStyle(new GanttChartStyle());
  }

}
