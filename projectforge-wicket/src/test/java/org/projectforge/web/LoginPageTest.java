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

package org.projectforge.web;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.address.AddressListPage;
import org.projectforge.web.admin.SystemUpdatePage;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.wicket.WicketPageTestBase;
import org.projectforge.web.wicket.WicketUtils;
import org.testng.annotations.Test;

public class LoginPageTest extends WicketPageTestBase
{

  private void waitForNewLogin()
  {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      log.info("Something went wrong while thread sleep");
    }
  }

  @Test
  public void testInternalCheckLogin()
  {
    login(AbstractTestBase.TEST_USER, AbstractTestBase.TEST_USER_PASSWORD);
    logout();
    login(AbstractTestBase.TEST_DELETED_USER, AbstractTestBase.TEST_DELETED_USER_PASSWORD, false);
    tester.assertRenderedPage(LoginPage.class);
    waitForNewLogin();

    login(AbstractTestBase.TEST_ADMIN_USER, "wrongPassword", false);
    tester.assertRenderedPage(LoginPage.class);

    //Hier kein wait, um 1 sec Sperre zu testen
    login(AbstractTestBase.TEST_ADMIN_USER, AbstractTestBase.TEST_ADMIN_USER_PASSWORD, false);
    tester.assertRenderedPage(LoginPage.class);
    logout();
  }

  @Test
  public void testInternalCheckLoginUpdateRequiredTrueNormalUser()
  {
    // Update required with normal User
    UserFilter.setUpdateRequiredFirst(true);
    login(AbstractTestBase.TEST_USER, AbstractTestBase.TEST_USER_PASSWORD, false);
    tester.assertRenderedPage(LoginPage.class);
    UserFilter.setUpdateRequiredFirst(false);
    logout();
  }

  @Test
  public void testInternalCheckLoginUpdateRequiredTrueAdminUser()
  {
    waitForNewLogin();
    // Update required with admin User
    UserFilter.setUpdateRequiredFirst(true);
    login(AbstractTestBase.TEST_ADMIN_USER, AbstractTestBase.TEST_ADMIN_USER_PASSWORD, false);
    tester.assertRenderedPage(SystemUpdatePage.class);
    UserFilter.setUpdateRequiredFirst(false);
    logout();
  }

  @Test
  public void testLoginAndLogoutAdmin()
  {
    final LoginPage loginPage = new LoginPage(new PageParameters());
    // start and render the test page
    tester.startPage(loginPage);
    // assert rendered page class
    tester.assertRenderedPage(LoginPage.class);
    // assert rendered label component
    tester.assertVisible("body:form:username");
    FormTester form = tester.newFormTester("body:form");
    form = tester.newFormTester("body:form");
    form.setValue(findComponentByLabel(form, "username"), AbstractTestBase.TEST_ADMIN_USER);
    form.setValue(findComponentByLabel(form, "password"), AbstractTestBase.TEST_ADMIN_USER_PASSWORD);
    form.submit(KEY_LOGINPAGE_BUTTON_LOGIN);
    tester.assertRenderedPage(CalendarPage.class);
    tester.startPage(AddressListPage.class);
    tester.assertRenderedPage(AddressListPage.class);

    loginTestAdmin(); // login should be ignored.
    tester.assertRenderedPage(WicketUtils.getDefaultPage());

    logout();
    tester.startPage(AddressListPage.class);
    tester.assertRenderedPage(LoginPage.class);
  }

  @Test
  public void testLoginAndLogoutWrongData()
  {
    final LoginPage loginPage = new LoginPage(new PageParameters());
    // start and render the test page
    tester.startPage(loginPage);
    // assert rendered page class
    tester.assertRenderedPage(LoginPage.class);
    // assert rendered label component
    tester.assertVisible("body:form:username");
    FormTester form = tester.newFormTester("body:form");
    form.setValue(findComponentByLabel(form, "username"), "demo");
    form.setValue(findComponentByLabel(form, "password"), "wrong");
    form.submit(KEY_LOGINPAGE_BUTTON_LOGIN);
    tester.assertRenderedPage(LoginPage.class);
    waitForNewLogin();
  }

}
