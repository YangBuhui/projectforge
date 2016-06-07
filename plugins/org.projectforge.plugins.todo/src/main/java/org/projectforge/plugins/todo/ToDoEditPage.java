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

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.user.UserPrefEditPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.WicketUtils;

@EditPage(defaultReturnPage = ToDoListPage.class)
public class ToDoEditPage extends AbstractEditPage<ToDoDO, ToDoEditForm, ToDoDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = -5058143025817192156L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ToDoEditPage.class);

  @SpringBean
  private ToDoDao toDoDao;

  private ToDoDO oldToDo;

  public ToDoEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.todo");
    init();
    if (isNew() == true) {
      final ToDoDO pref = getToDoPrefData(false);
      if (pref != null) {
        copyPrefValues(pref, getData());
      }
      getData().setReporter(ThreadLocalUserContext.getUser());
      getData().setStatus(ToDoStatus.OPENED);
    } else {
      // Store old to-do for sending e-mail notification after major changes.
      oldToDo = new ToDoDO();
      oldToDo.copyValuesFrom(getData());
    }
  }

  @Override
  protected void onAfterRender()
  {
    super.onAfterRender();
    if (accessChecker.isRestrictedOrDemoUser() == true) {
      // Do nothing.
      return;
    }
    if (ObjectUtils.equals(ThreadLocalUserContext.getUserId(), getData().getAssigneeId()) == true) {
      // OK, user has now seen this to-do: delete recent flag:
      if (isNew() == false && getData().isRecent() == true) {
        getData().setRecent(false);
        toDoDao.update(getData());
      }
    }
  }

  private void sendNotification()
  {
    final String url = getPageAsLink(WicketUtils.getEditPageParameters(getData().getId()));
    toDoDao.sendNotification(form.getData(), url);
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    // Save to-do as recent to-do
    final ToDoDO pref = getToDoPrefData(true);
    copyPrefValues(getData(), pref);
    // Does the user want to store this to-do as template?
    boolean sendNotification = false;
    if (form.sendNotification == true) {
      sendNotification = true;
    } else if (oldToDo == null) {
      // Send notification on new to-do's.
      sendNotification = true;
    } else {
      if (ObjectUtils.equals(oldToDo.getAssigneeId(), getData().getAssigneeId()) == false) {
        // Assignee was changed.
        sendNotification = true;
      } else if (oldToDo.getStatus() != getData().getStatus()) {
        // Status was changed.
        sendNotification = true;
      } else if (oldToDo.isDeleted() != getData().isDeleted()) {
        // Deletion status was changed.
        sendNotification = true;
      }
    }
    if (sendNotification == true) {
      sendNotification();
    }
    // if (form.sendShortMessage == true) {
    // final PFUserDO assignee = getData().getAssignee();
    // final String mobileNumber = assignee != null ? assignee.getPersonalMebMobileNumbers() : null;
    // }
    if (BooleanUtils.isTrue(form.saveAsTemplate) == true) {
      final UserPrefEditPage userPrefEditPage = new UserPrefEditPage(ToDoPlugin.USER_PREF_AREA, getData());
      userPrefEditPage.setReturnToPage(this.returnToPage);
      return userPrefEditPage;
    }
    return null;
  }

  private void copyPrefValues(final ToDoDO src, final ToDoDO dest)
  {
    dest.setPriority(src.getPriority()).setType(src.getType());
  }

  /**
   * @param force If true then a pre entry is created if not exist.
   */
  private ToDoDO getToDoPrefData(final boolean force)
  {
    ToDoDO pref = (ToDoDO) getUserPrefEntry(ToDoDO.class.getName());
    if (pref == null && force == true) {
      pref = new ToDoDO();
      putUserPrefEntry(ToDoDO.class.getName(), pref, true);
    }
    return pref;
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      toDoDao.setTask(getData(), (Integer) selectedValue);
    } else if ("groupId".equals(property) == true) {
      toDoDao.setGroup(getData(), (Integer) selectedValue);
      form.groupSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      getData().setTask(null);
    } else if ("groupId".equals(property) == true) {
      getData().setGroup(null);
      form.groupSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  protected void updateAndClose()
  {
    update();
  }

  @Override
  protected ToDoDao getBaseDao()
  {
    return toDoDao;
  }

  @Override
  protected ToDoEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final ToDoDO data)
  {
    return new ToDoEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
