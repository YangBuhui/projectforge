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

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class ChangePasswordForm extends AbstractStandardForm<ChangePasswordForm, ChangePasswordPage>
{
  private static final long serialVersionUID = -3424973755250980758L;

  private String oldPassword, newPassword, passwordRepeat;

  public ChangePasswordForm(final ChangePasswordPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user.changePassword.oldPassword"));
      final PasswordTextField oldPassword = new PasswordTextField(fs.getTextFieldId(), new PropertyModel<String>(this, "oldPassword"));
      oldPassword.setResetPassword(true).setRequired(true);
      WicketUtils.setFocus(oldPassword);
      fs.add(oldPassword);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user.changePassword.newPassword"));
      final PasswordTextField newPassword = new PasswordTextField(fs.getTextFieldId(), new PropertyModel<String>(this, "newPassword"));
      newPassword.setResetPassword(true).setRequired(true);
      fs.add(newPassword);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("passwordRepeat"));
      final PasswordTextField passwordRepeat = new PasswordTextField(fs.getTextFieldId(), new PropertyModel<String>(this, "passwordRepeat"));
      passwordRepeat.setResetPassword(true).setRequired(true);
      fs.add(passwordRepeat);
    }

    {
      final Button cancelButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("cancel")) {
        @Override
        public final void onSubmit()
        {
          parentPage.cancel();
        }
      };
      cancelButton.setDefaultFormProcessing(false);
      final SingleButtonPanel cancelButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cancelButton, getString("cancel"),
          SingleButtonPanel.CANCEL);
      actionButtons.add(cancelButtonPanel);

      final Button changeButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("change")) {
        @Override
        public final void onSubmit()
        {
          parentPage.update();
        }
      };
      final SingleButtonPanel changeButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), changeButton, getString("update"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(changeButtonPanel);
      setDefaultButton(changeButton);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractForm#onBeforeRender()
   */
  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    actionButtons.render();
  }

  /**
   * @return the oldPassword
   */
  public String getOldPassword()
  {
    return oldPassword;
  }

  /**
   * @return the newPassword
   */
  public String getNewPassword()
  {
    return newPassword;
  }

  /**
   * @return the passwordRepeat
   */
  public String getPasswordRepeat()
  {
    return passwordRepeat;
  }
}
