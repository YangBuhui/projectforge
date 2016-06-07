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

package org.projectforge.multitenancy;

import java.util.HashSet;
import java.util.Set;

import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class MultiTenancyTestFork extends AbstractTestBase
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MultiTenancyTestFork.class);

  @Autowired
  private ConfigurationDao configurationDao;

  @Autowired
  private TenantDao tenantDao;

  @Autowired
  private TenantService tenantService;

  private static TenantDO defaultTenant, tenant2, tenant3;

  private static PFUserDO superAdminDefault, superAdmin2, superAdmin3, admin1, admin2, user1;

  private static boolean initialized;

  private void initialize()
  {
    if (initialized == true) {
      return;
    }
    defaultTenant = tenantService.getDefaultTenant();
    logon(TEST_ADMIN_USER);
    final ConfigurationDO configurationDO = configurationDao.getEntry(ConfigurationParam.MULTI_TENANCY_ENABLED);
    configurationDO.setBooleanValue(true);
    configurationDao.internalUpdate(configurationDO);
    superAdminDefault = createUser("mt_superAdminDefault", true);
    superAdmin2 = createUser("mt_superAdmin2", true);
    superAdmin3 = createUser("mt_superAdmin3", true);
    admin1 = createUser("mt_admin1", false);
    admin2 = createUser("mt_admin2", false);
    user1 = initTestDB.addUser(new PFUserDO().setUsername("mt_user1"));
    logon(superAdminDefault);
    //    defaultTenant = createTenant("Tenant 1", true, superAdminDefault, admin1);
    tenant2 = createTenant("Tenant 2", false, superAdmin2, superAdminDefault, admin1, admin2);
    tenant3 = createTenant("Tenant 3", false);
  }

  @Test
  public void testUserDO()
  {
    initialize();
  }

  private PFUserDO createUser(final String username, final boolean superAdmin)
  {
    final PFUserDO user = new PFUserDO().setUsername(username);
    user.setSuperAdmin(superAdmin);
    user.setTenant(defaultTenant);
    userDao.internalSave(user);
    Set<TenantDO> tenantsToAssign = new HashSet<>();
    tenantsToAssign.add(defaultTenant);
    tenantDao.internalAssignTenants(user, tenantsToAssign, null, false, false);
    return user;
  }

  private TenantDO createTenant(final String name, final boolean isDefault, final PFUserDO... assignedUsers)
  {
    final TenantDO tenant = new TenantDO().setName(name).setDefaultTenant(isDefault);
    tenantDao.internalSave(tenant);
    return tenant;
  }
}
