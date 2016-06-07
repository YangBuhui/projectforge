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

package org.projectforge.web.registry;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.WebPage;
import org.projectforge.framework.persistence.DaoConst;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.registry.Registry;
import org.projectforge.web.LoginPage;
import org.projectforge.web.access.AccessEditPage;
import org.projectforge.web.access.AccessListPage;
import org.projectforge.web.address.AddressEditPage;
import org.projectforge.web.address.AddressListPage;
import org.projectforge.web.address.AddressMobileEditPage;
import org.projectforge.web.address.AddressMobileListPage;
import org.projectforge.web.address.AddressMobileViewPage;
import org.projectforge.web.address.AddressViewPage;
import org.projectforge.web.address.PhoneCallPage;
import org.projectforge.web.address.SendSmsPage;
import org.projectforge.web.admin.AdminPage;
import org.projectforge.web.admin.ConfigurationListPage;
import org.projectforge.web.admin.SetupPage;
import org.projectforge.web.admin.SystemUpdatePage;
import org.projectforge.web.book.BookEditPage;
import org.projectforge.web.book.BookListPage;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.core.SearchPage;
import org.projectforge.web.doc.DocumentationPage;
import org.projectforge.web.doc.TutorialPage;
import org.projectforge.web.fibu.AccountingRecordEditPage;
import org.projectforge.web.fibu.AccountingRecordListPage;
import org.projectforge.web.fibu.AuftragEditPage;
import org.projectforge.web.fibu.AuftragListPage;
import org.projectforge.web.fibu.CustomerEditPage;
import org.projectforge.web.fibu.CustomerListPage;
import org.projectforge.web.fibu.DatevImportPage;
import org.projectforge.web.fibu.EingangsrechnungEditPage;
import org.projectforge.web.fibu.EingangsrechnungListPage;
import org.projectforge.web.fibu.EmployeeEditPage;
import org.projectforge.web.fibu.EmployeeListPage;
import org.projectforge.web.fibu.EmployeeSalaryEditPage;
import org.projectforge.web.fibu.EmployeeSalaryListPage;
import org.projectforge.web.fibu.KontoEditPage;
import org.projectforge.web.fibu.KontoListPage;
import org.projectforge.web.fibu.Kost1EditPage;
import org.projectforge.web.fibu.Kost1ListPage;
import org.projectforge.web.fibu.Kost2ArtEditPage;
import org.projectforge.web.fibu.Kost2ArtListPage;
import org.projectforge.web.fibu.Kost2EditPage;
import org.projectforge.web.fibu.Kost2ListPage;
import org.projectforge.web.fibu.MonthlyEmployeeReportPage;
import org.projectforge.web.fibu.ProjektEditPage;
import org.projectforge.web.fibu.ProjektListPage;
import org.projectforge.web.fibu.RechnungEditPage;
import org.projectforge.web.fibu.RechnungListPage;
import org.projectforge.web.fibu.ReportObjectivesPage;
import org.projectforge.web.gantt.GanttChartEditPage;
import org.projectforge.web.gantt.GanttChartListPage;
import org.projectforge.web.humanresources.HRListPage;
import org.projectforge.web.humanresources.HRPlanningEditPage;
import org.projectforge.web.humanresources.HRPlanningListPage;
import org.projectforge.web.imagecropper.ImageCropperPage;
import org.projectforge.web.meb.MebEditPage;
import org.projectforge.web.meb.MebListPage;
import org.projectforge.web.mobile.LoginMobilePage;
import org.projectforge.web.mobile.MenuMobilePage;
import org.projectforge.web.multitenancy.TenantEditPage;
import org.projectforge.web.multitenancy.TenantListPage;
import org.projectforge.web.orga.ContractEditPage;
import org.projectforge.web.orga.ContractListPage;
import org.projectforge.web.orga.PostausgangEditPage;
import org.projectforge.web.orga.PostausgangListPage;
import org.projectforge.web.orga.PosteingangEditPage;
import org.projectforge.web.orga.PosteingangListPage;
import org.projectforge.web.scripting.ScriptEditPage;
import org.projectforge.web.scripting.ScriptExecutePage;
import org.projectforge.web.scripting.ScriptListPage;
import org.projectforge.web.scripting.ScriptingPage;
import org.projectforge.web.statistics.PersonalStatisticsPage;
import org.projectforge.web.statistics.SystemStatisticsPage;
import org.projectforge.web.task.TaskEditPage;
import org.projectforge.web.task.TaskListPage;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.teamcal.admin.TeamCalListPage;
import org.projectforge.web.teamcal.event.TeamEventListPage;
import org.projectforge.web.teamcal.integration.TeamCalCalendarPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.timesheet.TimesheetListPage;
import org.projectforge.web.user.ChangePasswordPage;
import org.projectforge.web.user.GroupEditPage;
import org.projectforge.web.user.GroupListPage;
import org.projectforge.web.user.MyAccountEditPage;
import org.projectforge.web.user.UserEditPage;
import org.projectforge.web.user.UserListPage;
import org.projectforge.web.user.UserPrefEditPage;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.wicket.ErrorPage;
import org.projectforge.web.wicket.FeedbackPage;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for dao's. Here you can register additional daos and plugins (extensions of ProjectForge). This registry is
 * used e. g. by the general SearchPage.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class WebRegistry
{
  public static final WebRegistry instance = new WebRegistry();

  public static final String BOOKMARK_LOGIN = "login";

  private static final String BOOKMARK_MOBILE_PREFIX = "m-";

  public static final String BOOKMARK_MOBILE_LOGIN = BOOKMARK_MOBILE_PREFIX + BOOKMARK_LOGIN;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WebRegistry.class);

  private final Map<String, WebRegistryEntry> map = new HashMap<String, WebRegistryEntry>();

  private final List<WebRegistryEntry> orderedList = new ArrayList<WebRegistryEntry>();

  private final Map<String, Class<? extends WebPage>> mountPages = new HashMap<String, Class<? extends WebPage>>();

  public static WebRegistry getInstance()
  {
    return instance;
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   */
  public WebRegistryEntry register(final String id)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id));
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   * @param listPageColumnsCreatorClass Needed by SearchPage.
   */
  public WebRegistryEntry register(final String id,
      final Class<? extends IListPageColumnsCreator<?>> listPageColumnsCreatorClass)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id, listPageColumnsCreatorClass));
  }

  public WebRegistryEntry register(final WebRegistryEntry entry)
  {
    Validate.notNull(entry);
    map.put(entry.getId(), entry);
    orderedList.add(entry);
    return entry;
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   */
  public WebRegistryEntry register(final String id, final boolean insertBefore, final WebRegistryEntry entry)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id), insertBefore, entry);
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   */
  public WebRegistryEntry register(final String id,
      final Class<? extends IListPageColumnsCreator<?>> listPageColumnsCreatorClass,
      final boolean insertBefore, final WebRegistryEntry entry)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id, listPageColumnsCreatorClass), insertBefore, entry);
  }

  /**
   * @param existingEntry
   * @param insertBefore  If true then the given entry will be inserted before the existing entry, otherwise after.
   * @param entry
   * @return
   */
  public WebRegistryEntry register(final WebRegistryEntry existingEntry, final boolean insertBefore,
      final WebRegistryEntry entry)
  {
    Validate.notNull(existingEntry);
    Validate.notNull(entry);
    map.put(entry.getId(), entry);
    final int idx = orderedList.indexOf(existingEntry);
    if (idx < 0) {
      log.error("Registry entry '" + existingEntry.getId() + "' not found. Appending the given entry to the list.");
      orderedList.add(entry);
    } else if (insertBefore == true) {
      orderedList.add(idx, entry);
    } else {
      orderedList.add(idx + 1, entry);
    }
    return entry;
  }

  public WebRegistryEntry getEntry(final String id)
  {
    return map.get(id);
  }

  public List<WebRegistryEntry> getOrderedList()
  {
    return orderedList;
  }

  public BaseDao<?> getDao(final String id)
  {
    final WebRegistryEntry entry = getEntry(id);
    return entry != null ? entry.getDao() : null;
  }

  /**
   * Adds the page class as mount page.
   *
   * @param mountPage
   * @param pageClass
   * @return this for chaining.
   */
  public WebRegistry addMountPage(final String mountPage, final Class<? extends WebPage> pageClass)
  {
    this.mountPages.put(mountPage, pageClass);
    return this;
  }

  /**
   * Adds the both page classes as mount pages: mountPageBasename + "{List,Edit}.
   *
   * @param mountPageBasename
   * @param pageListClass
   * @param pageEditClass
   * @return this for chaining.
   */
  public WebRegistry addMountPages(final String mountPageBasename, final Class<? extends WebPage> pageListClass,
      final Class<? extends WebPage> pageEditClass)
  {
    addMountPage(mountPageBasename + "List", pageListClass);
    addMountPage(mountPageBasename + "Edit", pageEditClass);
    return this;
  }

  /**
   * Adds all page classes as mount pages: mountPageBasename + "{List,Edit,View}.
   *
   * @param mountPageBasename
   * @param pageListClass
   * @param pageEditClass
   * @param pageViewClass
   * @return this for chaining.
   */
  public WebRegistry addMountPages(final String mountPageBasename, final Class<? extends WebPage> pageListClass,
      final Class<? extends WebPage> pageEditClass, final Class<? extends WebPage> pageViewClass)
  {
    addMountPage(mountPageBasename + "List", pageListClass);
    addMountPage(mountPageBasename + "Edit", pageEditClass);
    addMountPage(mountPageBasename + "View", pageViewClass);
    return this;
  }

  public Map<String, Class<? extends WebPage>> getMountPages()
  {
    return mountPages;
  }

  public void init()
  {
    // This order is used by SearchPage:
    register(DaoConst.ADDRESS, AddressListPage.class);
    addMountPages(DaoConst.ADDRESS, AddressListPage.class, AddressEditPage.class);
    addMountPage(DaoConst.ADDRESS + "View", AddressViewPage.class);

    register(DaoConst.BOOK, BookListPage.class);
    addMountPages(DaoConst.BOOK, BookListPage.class, BookEditPage.class);

    register(DaoConst.TASK, TaskListPage.class);
    addMountPages(DaoConst.TASK, TaskListPage.class, TaskEditPage.class);

    register(DaoConst.TIMESHEET, TimesheetListPage.class);
    addMountPages(DaoConst.TIMESHEET, TimesheetListPage.class, TimesheetEditPage.class);

    register(DaoConst.TENANT, TenantListPage.class);
    addMountPages(DaoConst.TENANT, TenantListPage.class, TenantEditPage.class);

    register(DaoConst.USER, UserListPage.class);
    addMountPages(DaoConst.USER, UserListPage.class, UserEditPage.class);

    register(DaoConst.GROUP, GroupListPage.class);
    addMountPages(DaoConst.GROUP, GroupListPage.class, GroupEditPage.class);

    register(DaoConst.ORDERBOOK, AuftragListPage.class);
    addMountPages(DaoConst.ORDERBOOK, AuftragListPage.class, AuftragEditPage.class);

    register(DaoConst.CONTRACT, ContractListPage.class);
    addMountPages(DaoConst.CONTRACT, ContractListPage.class, ContractEditPage.class);

    register(DaoConst.INCOMING_INVOICE, EingangsrechnungListPage.class);
    addMountPages(DaoConst.INCOMING_INVOICE, EingangsrechnungListPage.class, EingangsrechnungEditPage.class);

    register(DaoConst.OUTGOING_INVOICE, RechnungListPage.class);
    addMountPages(DaoConst.OUTGOING_INVOICE, RechnungListPage.class, RechnungEditPage.class);

    register(DaoConst.INCOMING_MAIL, PosteingangListPage.class);
    addMountPages(DaoConst.INCOMING_MAIL, PosteingangListPage.class, PosteingangEditPage.class);

    register(DaoConst.OUTGOING_MAIL, PostausgangListPage.class);
    addMountPages(DaoConst.OUTGOING_MAIL, PostausgangListPage.class, PostausgangEditPage.class);

    register(DaoConst.ACCESS, AccessListPage.class);
    addMountPages(DaoConst.ACCESS, AccessListPage.class, AccessEditPage.class);
    register(DaoConst.ACCOUNT, KontoListPage.class);
    addMountPages(DaoConst.ACCOUNT, KontoListPage.class, KontoEditPage.class);
    register(DaoConst.ACCOUNTING_RECORD, AccountingRecordListPage.class);
    addMountPages(DaoConst.ACCOUNTING_RECORD, AccountingRecordListPage.class, AccountingRecordEditPage.class);
    register(DaoConst.COST1, Kost1ListPage.class);
    addMountPages(DaoConst.COST1, Kost1ListPage.class, Kost1EditPage.class);
    register(DaoConst.COST2, Kost2ListPage.class);
    addMountPages(DaoConst.COST2, Kost2ListPage.class, Kost2EditPage.class);
    register(DaoConst.COST2_Type, Kost2ArtListPage.class);
    addMountPages(DaoConst.COST2_Type, Kost2ArtListPage.class, Kost2ArtEditPage.class);
    register(DaoConst.CUSTOMER, CustomerListPage.class);
    addMountPages(DaoConst.CUSTOMER, CustomerListPage.class, CustomerEditPage.class);
    register(DaoConst.EMPLOYEE, EmployeeListPage.class);
    addMountPages(DaoConst.EMPLOYEE, EmployeeListPage.class, EmployeeEditPage.class);
    register(DaoConst.MEB, MebListPage.class);
    addMountPages(DaoConst.MEB, MebListPage.class, MebEditPage.class);
    register(DaoConst.PROJECT, ProjektListPage.class);
    addMountPages(DaoConst.PROJECT, ProjektListPage.class, ProjektEditPage.class);

    addMountPages(DaoConst.EMPLOYEE_SALARY, EmployeeSalaryListPage.class, EmployeeSalaryEditPage.class);
    addMountPages(DaoConst.GANTT, GanttChartListPage.class, GanttChartEditPage.class);
    addMountPages(DaoConst.HR_PLANNING, HRPlanningListPage.class, HRPlanningEditPage.class);
    addMountPage(DaoConst.HR_LIST, HRListPage.class);
    addMountPages(DaoConst.SCRIPT, ScriptListPage.class, ScriptEditPage.class);
    addMountPages(DaoConst.USER_PREF, UserPrefListPage.class, UserPrefEditPage.class);

    addMountPage("admin", AdminPage.class);
    addMountPage("imageCropper", ImageCropperPage.class);
    addMountPage("calendar", CalendarPage.class);
    addMountPage("changePassword", ChangePasswordPage.class);
    addMountPage("configuration", ConfigurationListPage.class);
    addMountPage("datevImport", DatevImportPage.class);
    addMountPage("doc", DocumentationPage.class);
    addMountPage("error", ErrorPage.class);
    addMountPage("feedback", FeedbackPage.class);
    addMountPage(BOOKMARK_LOGIN, LoginPage.class);
    addMountPage("monthlyEmployeeReport", MonthlyEmployeeReportPage.class);
    addMountPage("myAccount", MyAccountEditPage.class);
    addMountPage("personalStatistics", PersonalStatisticsPage.class);
    addMountPage("phoneCall", PhoneCallPage.class);
    addMountPage("reportObjectives", ReportObjectivesPage.class);
    addMountPage("scriptExecute", ScriptExecutePage.class);
    addMountPage("scripting", ScriptingPage.class);
    addMountPage("search", SearchPage.class);
    addMountPage("sendSms", SendSmsPage.class);
    addMountPage("setup", SetupPage.class);
    addMountPage("systemStatistics", SystemStatisticsPage.class);
    addMountPage("systemUpdate", SystemUpdatePage.class);
    addMountPage("taskTree", TaskTreePage.class);
    addMountPage("tutorial", TutorialPage.class);

    addMountPages(BOOKMARK_MOBILE_PREFIX + "address", AddressMobileListPage.class, AddressMobileEditPage.class,
        AddressMobileViewPage.class);

    addMountPage(BOOKMARK_MOBILE_LOGIN, LoginMobilePage.class);
    addMountPage(BOOKMARK_MOBILE_PREFIX + "menu", MenuMobilePage.class);

    register("teamCal", TeamCalListPage.class);
    register("teamEvent", TeamEventListPage.class);
    addMountPage("teamCalendar", TeamCalCalendarPage.class);
  }
}
