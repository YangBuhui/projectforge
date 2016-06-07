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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.login.Login;
import org.projectforge.business.user.UserDao;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.MessagePage;

public class ChangePasswordPage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = -2506732790809310722L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChangePasswordPage.class);

  @SpringBean
  private UserDao userDao;

  private final ChangePasswordForm form;

  public ChangePasswordPage(final PageParameters parameters)
  {
    super(parameters);
    if (Login.getInstance().isPasswordChangeSupported(getUser()) == false) {
      throw new RestartResponseException(new MessagePage("user.changePassword.notSupported"));
    }
    form = new ChangePasswordForm(this);
    body.add(form);
    form.init();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredBasePage#isAccess4restrictedUsersAllowed()
   */
  @Override
  public boolean isAccess4restrictedUsersAllowed()
  {
    return true;
  }

  protected void cancel()
  {
    setResponsePage(new MessagePage("message.cancelAction", getString("user.changePassword.title")));
  }

  protected void update()
  {
    if (StringUtils.equals(form.getNewPassword(), form.getPasswordRepeat()) == false) {
      form.addError("user.error.passwordAndRepeatDoesNotMatch");
      return;
    }
    String errorMsgKey = userDao.checkPasswordQuality(form.getNewPassword());
    if (errorMsgKey != null) {
      form.addError(errorMsgKey);
      return;
    }
    log.info("User wants to change his password.");
    errorMsgKey = userDao.changePassword(getUser(), form.getOldPassword(), form.getNewPassword());
    if (errorMsgKey != null) {
      form.addError(errorMsgKey);
      return;
    }
    setResponsePage(new MessagePage("user.changePassword.msg.passwordSuccessfullyChanged"));
  }

  @Override
  protected String getTitle()
  {
    return getString("user.changePassword.title");
  }
}
