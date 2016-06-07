package org.projectforge.business.user.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserService.class);

  private UserGroupCache userGroupCache;

  @Autowired
  private UserDao userDao;

  private final UsersComparator usersComparator = new UsersComparator();


  /**
   * @param userIds
   * @return
   */
  @Override
  public List<String> getUserNames(final String userIds)
  {
    if (StringUtils.isEmpty(userIds) == true) {
      return null;
    }
    final int[] ids = StringHelper.splitToInts(userIds, ",", false);
    final List<String> list = new ArrayList<String>();
    for (final int id : ids) {
      final PFUserDO user = getUserGroupCache().getUser(id);
      if (user != null) {
        list.add(user.getFullname());
      } else {
        log.warn("User with id '" + id + "' not found in UserGroupCache. userIds string was: " + userIds);
      }
    }
    return list;
  }

  @Override
  public Collection<PFUserDO> getSortedUsers()
  {
    TreeSet<PFUserDO> sortedUsers = new TreeSet<PFUserDO>(usersComparator);
    final Collection<PFUserDO> allusers = getUserGroupCache().getAllUsers();
    final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    for (final PFUserDO user : allusers) {
      if (user.isDeleted() == false && user.isDeactivated() == false
          && userDao.hasSelectAccess(loggedInUser, user, false) == true) {
        sortedUsers.add(user);
      }
    }
    return sortedUsers;
  }

  /**
   * 
   * @param userIds
   * @return
   */
  @Override
  public Collection<PFUserDO> getSortedUsers(final String userIds)
  {
    if (StringUtils.isEmpty(userIds) == true) {
      return null;
    }
    TreeSet<PFUserDO> sortedUsers = new TreeSet<PFUserDO>(usersComparator);
    final int[] ids = StringHelper.splitToInts(userIds, ",", false);
    for (final int id : ids) {
      final PFUserDO user = getUserGroupCache().getUser(id);
      if (user != null) {
        sortedUsers.add(user);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + userIds);
      }
    }
    return sortedUsers;
  }

  @Override
  public String getUserIds(final Collection<PFUserDO> users)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final PFUserDO user : users) {
      if (user.getId() != null) {
        first = StringHelper.append(buf, first, String.valueOf(user.getId()), ",");
      }
    }
    return buf.toString();
  }

  /**
   * @return the useruserCache
   */
  private UserGroupCache getUserGroupCache()
  {
    if (userGroupCache == null) {
      userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    }
    return userGroupCache;
  }
}
