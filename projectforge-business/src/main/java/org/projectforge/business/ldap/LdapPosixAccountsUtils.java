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

package org.projectforge.business.ldap;

import java.util.Collection;

import org.apache.commons.lang.ObjectUtils;
import org.projectforge.business.user.UserCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class LdapPosixAccountsUtils
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapPosixAccountsUtils.class);

  @Autowired
  UserCache userCache;

  @Autowired
  LdapService ldapService;

  /**
   * Get all given uid numbers of all ProjectForge users including any deleted user and get the next highest and free
   * number. The number is 1000 if no uid number (with an value greater than 999) is found.
   */
  public int getNextFreeUidNumber()
  {
    final Collection<PFUserDO> allUsers = userCache.getAllUsers();
    int currentMaxNumber = 999;
    for (final PFUserDO user : allUsers) {
      final LdapUserValues ldapUserValues = PFUserDOConverter.readLdapUserValues(user.getLdapValues());
      if (ldapUserValues == null) {
        continue;
      }
      if (ldapUserValues.getUidNumber() != null && ldapUserValues.getUidNumber().intValue() > currentMaxNumber) {
        currentMaxNumber = ldapUserValues.getUidNumber();
      }
    }
    return currentMaxNumber + 1;
  }

  /**
   * For preventing double uidNumbers.
   * 
   * @param user
   * @param uidNumber
   * @return Returns true if any user (also deleted user) other than the given user has the given uidNumber, otherwise
   *         false.
   */
  public boolean isGivenNumberFree(final PFUserDO currentUser, final int uidNumber)
  {
    final Collection<PFUserDO> allUsers = userCache.getAllUsers();
    for (final PFUserDO user : allUsers) {
      final LdapUserValues ldapUserValues = PFUserDOConverter.readLdapUserValues(user.getLdapValues());
      if (ObjectUtils.equals(user.getId(), currentUser.getId()) == true) {
        // The current user may have the given uidNumber already, so ignore this entry.
        continue;
      }
      if (ldapUserValues != null && ldapUserValues.getUidNumber() != null
          && ldapUserValues.getUidNumber().intValue() == uidNumber) {
        // Number isn't free.
        log.info("The uidNumber (posix account) '" + uidNumber + "' is already occupied by user: " + user);
        return false;
      }
    }
    return true;
  }

  /**
   * Sets next free uid, the gid (configured in config.xml), the home directory (built of standard prefix and the given
   * user's username) and the configured login-shell.
   * 
   * @param ldapUserValues
   * @param user
   */
  public void setDefaultValues(final LdapUserValues ldapUserValues, final PFUserDO user)
  {
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    LdapPosixAccountsConfig ldapPosixAccountsConfig = ldapConfig != null ? ldapConfig.getPosixAccountsConfig() : null;
    if (ldapPosixAccountsConfig == null) {
      ldapPosixAccountsConfig = new LdapPosixAccountsConfig();
    }
    ldapUserValues.setUidNumber(getNextFreeUidNumber());
    ldapUserValues.setGidNumber(ldapPosixAccountsConfig.getDefaultGidNumber());
    ldapUserValues.setHomeDirectory(ldapPosixAccountsConfig.getHomeDirectoryPrefix() + user.getUsername());
    ldapUserValues.setLoginShell(ldapPosixAccountsConfig.getDefaultLoginShell());
  }
}
