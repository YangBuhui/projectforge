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

package org.projectforge.plugins.poll.attendee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.plugins.poll.NewPollFrontendModel;
import org.projectforge.plugins.poll.NewPollOverviewPage;
import org.projectforge.plugins.poll.NewPollPage;
import org.projectforge.plugins.poll.PollBasePage;
import org.projectforge.plugins.poll.PollListPage;
import org.projectforge.plugins.poll.event.PollEventEditPage;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.GroupsWicketProvider;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class PollAttendeePage extends PollBasePage
{

  private static final long serialVersionUID = 8780858653279140945L;

  private static Integer SECURE_KEY_LENGTH = 50;

  @SpringBean
  private UserDao userDao;

  @SpringBean
  GroupService groupService;

  private MultiChoiceListHelper<GroupDO> assignGroupsListHelper;

  private MultiChoiceListHelper<PFUserDO> assignUsersListHelper;

  private String emailList;

  private List<String> existingMailAddresses;

  private final NewPollFrontendModel model;

  public PollAttendeePage(final PageParameters parameters)
  {
    super(parameters);
    NewPollPage.redirectToNewPollPage(parameters);
    this.model = null;
  }

  /**
   * @param parameters
   */
  public PollAttendeePage(final PageParameters parameters, final NewPollFrontendModel model)
  {
    super(parameters);
    this.model = model;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();

    emailList = "";

    // User select
    final UsersProvider usersProvider = new UsersProvider(userDao);
    final FieldsetPanel fsUserSelect = gridBuilder.newFieldset(getString("plugins.poll.attendee.users"));
    assignUsersListHelper = new MultiChoiceListHelper<PFUserDO>().setComparator(new UsersComparator()).setFullList(
        usersProvider.getSortedUsers());
    assignUsersListHelper.setAssignedItems(model.getUserDoFromAttendees());
    final Select2MultiChoice<PFUserDO> users = new Select2MultiChoice<PFUserDO>(fsUserSelect.getSelect2MultiChoiceId(),
        new PropertyModel<Collection<PFUserDO>>(this.assignUsersListHelper, "assignedItems"), usersProvider);
    fsUserSelect.add(users);

    // Group select
    assignGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator());
    assignGroupsListHelper.setAssignedItems(model.getPollGroupList());
    final FieldsetPanel fsGroupSelect = gridBuilder.newFieldset(getString("plugins.poll.attendee.groups"));
    final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fsGroupSelect.getSelect2MultiChoiceId(),
        new PropertyModel<Collection<GroupDO>>(this.assignGroupsListHelper, "assignedItems"),
        new GroupsWicketProvider(groupService));
    fsGroupSelect.add(groups);

    // TODO fancy email select
    final FieldsetPanel fsEMail = gridBuilder.newFieldset(getString("email"));
    fsEMail.add(getNewEMailField(fsEMail.getTextFieldId()));

  }

  private TextField<String> getNewEMailField(final String wicketId)
  {
    existingMailAddresses = new ArrayList<String>();
    if (model.getPollAttendeeList() != null) {
      for (final PollAttendeeDO attendee : model.getPollAttendeeList()) {
        if (attendee.getEmail() != null) {
          emailList += attendee.getEmail() + "; ";
          existingMailAddresses.add(attendee.getEmail());
        }
      }
    }
    final PropertyModel<String> mailModel = new PropertyModel<String>(this, "emailList");
    final TextField<String> eMailField = new TextField<String>(wicketId, mailModel);
    return eMailField;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString("plugins.poll.attendee");
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onConfirm()
   */
  @Override
  protected void onConfirm()
  {
    final Set<PollAttendeeDO> allAttendeeList = new HashSet<PollAttendeeDO>();
    final Set<PollAttendeeDO> userAttendees = new HashSet<PollAttendeeDO>();
    final Set<GroupDO> assignedGroups = new HashSet<GroupDO>();

    if (assignGroupsListHelper.getAssignedItems() != null) {
      if (assignGroupsListHelper.getAssignedItems().isEmpty() == false) {
        for (final GroupDO group : assignGroupsListHelper.getAssignedItems()) {
          assignedGroups.add(group);
          for (final PFUserDO user : group.getAssignedUsers()) {
            allAttendeeList.add(createAttendee(user));
          }
        }
      }
    }

    if (assignUsersListHelper.getAssignedItems() != null) {
      if (assignUsersListHelper.getAssignedItems().isEmpty() == false) {
        for (final PFUserDO user : assignUsersListHelper.getAssignedItems()) {
          final PollAttendeeDO attendee = createAttendee(user);
          allAttendeeList.add(attendee);
          userAttendees.add(attendee);
        }
      }
    }

    // add new email addresses, if available
    final String[] emails = StringUtils.split(emailList, ";");
    if (emails != null) {
      if (emails.length > 0) {
        for (final String email : emails) {
          if (existingMailAddresses.contains(email.trim()) == false) {
            final PollAttendeeDO newAttendee = new PollAttendeeDO();
            newAttendee.setEmail(email.trim());
            newAttendee.setSecureKey(NumberHelper.getSecureRandomUrlSaveString(SECURE_KEY_LENGTH));
            allAttendeeList.add(newAttendee);
          }
        }
      }
    }

    // add existing mail addresses
    if (model.isNew() == false) {
      allAttendeeList.addAll(model.getUserOrEmailList(false));
    }

    if (allAttendeeList.isEmpty() == true) {
      error(getString("plugins.poll.attendee.error"));
    } else {
      model.getPollAttendeeList().clear();
      //      model.getCalculatedAttendeeList().clear();
      model.getPollGroupList().clear();

      model.getPollAttendeeList().addAll(allAttendeeList);
      //      model.getCalculatedAttendeeList().addAll(allAttendeeList);
      model.getPollGroupList().addAll(assignedGroups);
      setResponsePage(new NewPollOverviewPage(getPageParameters(), model));
    }
  }

  private PollAttendeeDO createAttendee(final PFUserDO user)
  {
    final PollAttendeeDO newAttendee = new PollAttendeeDO();
    newAttendee.setUser(user);
    return newAttendee;
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onCancel()
   */
  @Override
  protected void onCancel()
  {
    setResponsePage(PollListPage.class);
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onBack()
   */
  @Override
  protected void onBack()
  {
    setResponsePage(new PollEventEditPage(getPageParameters(), model));
  }
}
