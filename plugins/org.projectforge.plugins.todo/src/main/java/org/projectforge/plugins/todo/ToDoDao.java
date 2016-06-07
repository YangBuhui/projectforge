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

package org.projectforge.plugins.todo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.ObjectUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.user.UserDao;
import org.projectforge.continuousdb.Table;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Repository
public class ToDoDao extends BaseDao<ToDoDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ToDoDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "reporter.username", "reporter.firstname",
      "reporter.lastname",
      "assignee.username", "assignee.firstname", "assignee.lastname", "task.title", "task.taskpath", "group.name" };

  private final Table table = new Table(ToDoDO.class);

  @Autowired
  private DataSource dataSource;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private SendMail sendMail;

  @Autowired
  private ConfigurationService configurationService;

  private transient TaskTree taskTree;

  private final ToDoCache toDoCache = new ToDoCache(this);

  public ToDoDao()
  {
    super(ToDoDO.class);
    userRightId = TodoPluginUserRightId.PLUGIN_TODO;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public List<ToDoDO> getList(final BaseSearchFilter filter)
  {
    final ToDoFilter myFilter;
    if (filter instanceof ToDoFilter) {
      myFilter = (ToDoFilter) filter;
    } else {
      myFilter = new ToDoFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final Collection<ToDoStatus> col = new ArrayList<ToDoStatus>(5);
    final String searchString = myFilter.getSearchString();
    if (myFilter.isOnlyRecent() == true) {
      final PFUserDO assignee = new PFUserDO();
      assignee.setId(ThreadLocalUserContext.getUserId());
      queryFilter.add(Restrictions.eq("assignee", assignee));
      myFilter.setSearchString(""); // Delete search string for ignoring it.
      queryFilter.add(Restrictions.eq("recent", true));
    } else {
      if (myFilter.isOpened() == true) {
        col.add(ToDoStatus.OPENED);
      }
      if (myFilter.isClosed() == true) {
        col.add(ToDoStatus.CLOSED);
      }
      if (myFilter.isPostponed() == true) {
        col.add(ToDoStatus.POSTPONED);
      }
      if (myFilter.isReopened() == true) {
        col.add(ToDoStatus.RE_OPENED);
      }
      if (myFilter.isInprogress() == true) {
        col.add(ToDoStatus.IN_PROGRESS);
      }
      if (col.size() > 0) {
        queryFilter.add(Restrictions.in("status", col));
      }
      if (myFilter.getTaskId() != null) {
        final TaskNode node = getTaskTree().getTaskNodeById(myFilter.getTaskId());
        final List<Integer> taskIds = node.getDescendantIds();
        taskIds.add(node.getId());
        queryFilter.add(Restrictions.in("task.id", taskIds));
      }
      if (myFilter.getAssigneeId() != null) {
        final PFUserDO assignee = new PFUserDO();
        assignee.setId(myFilter.getAssigneeId());
        queryFilter.add(Restrictions.eq("assignee", assignee));
      }
      if (myFilter.getReporterId() != null) {
        final PFUserDO reporter = new PFUserDO();
        reporter.setId(myFilter.getReporterId());
        queryFilter.add(Restrictions.eq("reporter", reporter));
      }
    }
    queryFilter.addOrder(Order.desc("created"));
    final List<ToDoDO> list = getList(queryFilter);
    myFilter.setSearchString(searchString); // Restore search string.
    return list;
  }

  /**
   * Sends an e-mail to the projekt manager if exists and is not equals to the logged in user.
   * 
   * @param todo
   * @param operationType
   * @return
   */
  public void sendNotification(final ToDoDO todo, final String requestUrl)
  {
    if (configurationService.isSendMailConfigured() == false) {
      // Can't send e-mail because no send mail is configured.
      return;
    }
    final Map<String, Object> data = new HashMap<String, Object>();
    data.put("todo", todo);
    data.put("requestUrl", requestUrl);
    final List<DisplayHistoryEntry> history = getDisplayHistoryEntries(todo);
    final List<DisplayHistoryEntry> list = new ArrayList<DisplayHistoryEntry>();
    int i = 0;
    for (final DisplayHistoryEntry entry : history) {
      list.add(entry);
      if (++i >= 10) {
        break;
      }
    }
    data.put("history", list);
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final Integer userId = user.getId();
    final Integer assigneeId = todo.getAssigneeId();
    final Integer reporterId = todo.getReporterId();
    if (assigneeId != null && userId.equals(assigneeId) == false) {
      sendNotification(todo.getAssignee(), todo, data, true);
    }
    if (reporterId != null && userId.equals(reporterId) == false && reporterId.equals(assigneeId) == false) {
      sendNotification(todo.getReporter(), todo, data, true);
    }
    if (userId != assigneeId && userId != reporterId && hasSelectAccess(user, todo, false) == false) {
      // User is whether reporter nor assignee, so send e-mail (in the case the user hasn't read access anymore).
      sendNotification(ThreadLocalUserContext.getUser(), todo, data, false);
    }
  }

  private void sendNotification(final PFUserDO recipient, final ToDoDO toDo, final Map<String, Object> data,
      final boolean checkAccess)
  {
    if (checkAccess == true && hasSelectAccess(recipient, toDo, false) == false) {
      log.info("Recipient '"
          + recipient.getFullname()
          + "' (id="
          + recipient.getId()
          + ") of the notification has no select access to the todo entry: "
          + toDo);
      return;
    }
    final Locale locale = recipient.getLocale();
    final Mail msg = new Mail();
    msg.setTo(recipient);
    final StringBuffer subject = new StringBuffer();
    final ToDoStatus status = toDo.getStatus();
    if (status != null && status != ToDoStatus.OPENED) {
      subject.append("[").append(I18nHelper.getLocalizedString(locale, "plugins.todo.status")).append(": ")
          .append(I18nHelper.getLocalizedString(locale, status.getI18nKey())).append("] ");
    }
    subject.append(I18nHelper.getLocalizedString(locale, "plugins.todo.todo")).append(": ");
    subject.append(toDo.getSubject());
    msg.setProjectForgeSubject(subject.toString());
    final String content = sendMail.renderGroovyTemplate(msg, "mail/todoChangeNotification.html", data, recipient);
    msg.setContent(content);
    msg.setContentType(Mail.CONTENTTYPE_HTML);
    sendMail.send(msg, null, null);
  }

  @Override
  protected void onSave(final ToDoDO obj)
  {
    if (ObjectUtils.equals(ThreadLocalUserContext.getUserId(), obj.getAssigneeId()) == false) {
      // To-do is changed by other user than assignee, so set recent flag for this to-do for the assignee.
      obj.setRecent(true);
    }
  }

  @Override
  protected void onChange(final ToDoDO obj, final ToDoDO dbObj)
  {
    if (ObjectUtils.equals(ThreadLocalUserContext.getUserId(), obj.getAssigneeId()) == false) {
      // To-do is changed by other user than assignee, so set recent flag for this to-do for the assignee.
      final ToDoDO copyOfDBObj = new ToDoDO();
      copyOfDBObj.copyValuesFrom(dbObj, "deleted");
      if (copyOfDBObj.copyValuesFrom(obj, "deleted") == ModificationStatus.MAJOR) {
        // Modifications done:
        obj.setRecent(true);
      }
    }
  }

  @Override
  protected void afterSaveOrModify(final ToDoDO obj)
  {
    toDoCache.setExpired(); // Force reload of the menu item counters for open to-do entrie.
  }

  public void setAssignee(final ToDoDO todo, final Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    todo.setAssignee(user);
  }

  public void setReporter(final ToDoDO todo, final Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    todo.setReporter(user);
  }

  public void setTask(final ToDoDO todo, final Integer taskId)
  {
    final TaskDO task = getTaskTree().getTaskById(taskId);
    todo.setTask(task);
  }

  public void setGroup(final ToDoDO todo, final Integer groupId)
  {
    final GroupDO group = groupDao.getOrLoad(groupId);
    todo.setGroup(group);
  }

  /**
   * Get the number of open to-do entries for the given user. Entries are open (in this context) when they're not
   * deleted or closed. <br/>
   * The result is cached (therefore you can call this method very often).
   * 
   * @param userId If null then the current logged in user is assumed.
   * @return Number of open to-do entries.
   */
  public int getOpenToDoEntries(Integer userId)
  {
    if (userId == null) {
      userId = ThreadLocalUserContext.getUserId();
    }
    return toDoCache.getOpenToDoEntries(userId);
  }

  /**
   * Called by ToDoCache to get the number of open entries for the given users.
   * 
   * @param userId
   * @return Number of open to-do entries.
   */
  int internalGetOpenEntries(final Integer userId)
  {
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    try {
      return jdbc.queryForObject("SELECT COUNT(*) FROM "
          + table.getName()
          + " where assignee_fk="
          + userId
          + " and recent=true and deleted=false", Integer.class);
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
      return 0;
    }
  }

  @Override
  public ToDoDO newInstance()
  {
    return new ToDoDO();
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }
}
