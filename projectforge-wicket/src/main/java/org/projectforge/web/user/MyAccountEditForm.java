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

package org.projectforge.web.user;

import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class MyAccountEditForm extends AbstractEditForm<PFUserDO, MyAccountEditPage>
{
  private static final long serialVersionUID = 4137560623244324454L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MyAccountEditForm.class);

  boolean invalidateAllStayLoggedInSessions;

  @SpringBean
  private GroupService groupService;

  public MyAccountEditForm(final MyAccountEditPage parentPage, final PFUserDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getUsername()).setStrong());
    }
    UserEditForm.createFirstName(gridBuilder, data);
    UserEditForm.createLastName(gridBuilder, data);
    UserEditForm.createOrganization(gridBuilder, data);
    UserEditForm.createEMail(gridBuilder, data);
    UserEditForm.createAuthenticationToken(gridBuilder, data, (UserDao) getBaseDao(), this);
    UserEditForm.createJIRAUsername(gridBuilder, data);
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("user.assignedGroups")).suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), groupService.getGroupnames(data.getId())));
    gridBuilder.newSplitPanel(GridSize.COL50);
    UserEditForm.createLastLoginAndDeleteAllStayLogins(gridBuilder, data, (UserDao) getBaseDao(), this);
    UserEditForm.createLocale(gridBuilder, data);
    UserEditForm.createDateFormat(gridBuilder, data);
    UserEditForm.createExcelDateFormat(gridBuilder, data);
    UserEditForm.createTimeNotation(gridBuilder, data);
    UserEditForm.createTimeZone(gridBuilder, data);
    UserEditForm.createPhoneIds(gridBuilder, data);
    UserEditForm.createMEBPhoneNumbers(gridBuilder, data);
    gridBuilder.newGridPanel();
    UserEditForm.createDescription(gridBuilder, data);
    UserEditForm.createSshPublicKey(gridBuilder, data);
  }

  @Override
  public void updateButtonVisibility()
  {
    super.updateButtonVisibility();
    createButtonPanel.setVisible(false);
    updateButtonPanel.setVisible(true);
    deleteButtonPanel.setVisible(false);
    markAsDeletedButtonPanel.setVisible(false);
    undeleteButtonPanel.setVisible(false);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
