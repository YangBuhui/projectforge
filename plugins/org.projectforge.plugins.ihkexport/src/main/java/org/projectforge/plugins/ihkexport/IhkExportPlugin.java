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

package org.projectforge.plugins.ihkexport;

import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.web.MenuItemDef;
import org.projectforge.web.MenuItemDefId;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Your plugin initialization. Register all your components such as i18n files, data-access object etc.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class IhkExportPlugin extends AbstractPlugin
{
  public static final String ID = "ihkexport";

  public static final String RESOURCE_BUNDLE_NAME = "IhkExportI18nResources";

  @Autowired
  private TimesheetDao ihkExportDao;

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  @Override
  protected void initialize()
  {

    // Register it:
    register(ID, TimesheetDao.class, ihkExportDao, "plugins.ihkexport");

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID);

    // Register the menu entry as sub menu entry of the misc menu:
    final MenuItemDef parentMenu = pluginWicketRegistrationService.getMenuItemDef(MenuItemDefId.MISC);
    pluginWicketRegistrationService
        .registerMenuItem(new MenuItemDef(parentMenu, ID, 10, "plugins.ihkexport.menu", IhkExportPage.class));

    // Define the access management:
    registerRight(new IhkExportRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);
  }
}
