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

package org.projectforge.business.user;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.LockMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.PasswordCheckResult;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.framework.utils.Crypt;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.micromata.genome.jpa.Clauses;
import de.micromata.genome.jpa.CriteriaUpdate;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Repository
public class UserDao extends BaseDao<PFUserDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserDao.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private UserCache userCache;

  private static final String MESSAGE_KEY_OLD_PASSWORD_WRONG = "user.changePassword.error.oldPasswordWrong";

  private static final short AUTHENTICATION_TOKEN_LENGTH = 20;

  private static final short STAY_LOGGED_IN_KEY_LENGTH = 20;

  public static final String MESSAGE_KEY_PASSWORD_QUALITY_CHECK = "user.changePassword.error.passwordQualityCheck";

  private final List<UserChangedListener> userChangedListeners = new LinkedList<UserChangedListener>();

  public UserDao()
  {
    super(PFUserDO.class);
  }

  /**
   * Register given listener. The listener is called every time a user was inserted, updated or deleted.
   * 
   * @param userChangedListener
   */
  public void register(final UserChangedListener userChangedListener)
  {
    userChangedListeners.add(userChangedListener);
  }

  public QueryFilter getDefaultFilter()
  {
    final QueryFilter queryFilter = new QueryFilter(null, false);
    queryFilter.add(Restrictions.eq("deleted", false));
    return queryFilter;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#createQueryFilter(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  protected QueryFilter createQueryFilter(final BaseSearchFilter filter)
  {
    final boolean superAdmin = TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser()) == true;
    if (superAdmin == false) {
      return super.createQueryFilter(filter);
    }
    return new QueryFilter(filter, true);
  }

  @Override
  public List<PFUserDO> getList(final BaseSearchFilter filter)
  {
    final PFUserFilter myFilter;
    if (filter instanceof PFUserFilter) {
      myFilter = (PFUserFilter) filter;
    } else {
      myFilter = new PFUserFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(myFilter);
    if (myFilter.getDeactivatedUser() != null) {
      queryFilter.add(Restrictions.eq("deactivated", myFilter.getDeactivatedUser()));
    }
    if (Login.getInstance().hasExternalUsermanagementSystem() == true) {
      // Check hasExternalUsermngmntSystem because otherwise the filter is may-be preset for an user and the user can't change the filter
      // (because the fields aren't visible).
      if (myFilter.getRestrictedUser() != null) {
        queryFilter.add(Restrictions.eq("restrictedUser", myFilter.getRestrictedUser()));
      }
      if (myFilter.getLocalUser() != null) {
        queryFilter.add(Restrictions.eq("localUser", myFilter.getLocalUser()));
      }
    }
    if (myFilter.getHrPlanning() != null) {
      queryFilter.add(Restrictions.eq("hrPlanning", myFilter.getHrPlanning()));
    }
    queryFilter.addOrder(Order.asc("username"));
    List<PFUserDO> list = getList(queryFilter);
    if (myFilter.getIsAdminUser() != null) {
      final List<PFUserDO> origList = list;
      list = new LinkedList<PFUserDO>();
      for (final PFUserDO user : origList) {
        if (myFilter.getIsAdminUser() == accessChecker.isUserMemberOfAdminGroup(user, false)) {
          list.add(user);
        }
      }
    }
    if (applicationContext.getBean(TenantService.class).isMultiTenancyAvailable() == true
        && TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser()) == false) {
      final List<PFUserDO> origList = list;
      list = new LinkedList<PFUserDO>();
      for (final PFUserDO user : origList) {
        if (tenantChecker.isPartOfTenant(ThreadLocalUserContext.getUserContext().getCurrentTenant(), user) == true) {
          list.add(user);
        }
      }
    }
    return list;
  }

  public Collection<Integer> getAssignedGroups(final PFUserDO user)
  {
    return getUserGroupCache().getUserGroups(user);
  }

  public Collection<Integer> getAssignedTenants(final PFUserDO user)
  {
    final PFUserDO u = userCache.getUser(user.getId());
    @SuppressWarnings("unchecked")
    final List<TenantDO> list = (List<TenantDO>) getHibernateTemplate()
        .find("from TenantDO t where ? member of t.assignedUsers", u);
    final Set<Integer> result = new HashSet<Integer>();
    if (list != null) {
      for (final TenantDO tenant : list) {
        result.add(tenant.getId());
      }
    }
    return result;
  }

  public List<UserRightDO> getUserRights(final Integer userId)
  {
    return getUserGroupCache().getUserRights(userId);
  }

  public String[] getPersonalPhoneIdentifiers(final PFUserDO user)
  {
    final String[] tokens = StringUtils.split(user.getPersonalPhoneIdentifiers(), ", ;|");
    if (tokens == null) {
      return null;
    }
    int n = 0;
    for (final String token : tokens) {
      if (StringUtils.isNotBlank(token) == true) {
        n++;
      }
    }
    if (n == 0) {
      return null;
    }
    final String[] result = new String[n];
    n = 0;
    for (final String token : tokens) {
      if (StringUtils.isNotBlank(token) == true) {
        result[n] = token.trim();
        n++;
      }
    }
    return result;
  }

  public String getNormalizedPersonalPhoneIdentifiers(final PFUserDO user)
  {
    if (StringUtils.isNotBlank(user.getPersonalPhoneIdentifiers()) == true) {
      final String[] ids = getPersonalPhoneIdentifiers(user);
      if (ids != null) {
        final StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (final String id : ids) {
          if (first == true) {
            first = false;
          } else {
            buf.append(",");
          }
          buf.append(id);
        }
        return buf.toString();
      }
    }
    return null;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#onChange(org.projectforge.core.ExtendedBaseDO,
   *      org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onChange(final PFUserDO obj, final PFUserDO dbObj)
  {
    super.onChange(obj, dbObj);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSave(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterSave(final PFUserDO obj)
  {
    super.afterSave(obj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.INSERT);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterUpdate(org.projectforge.core.ExtendedBaseDO,
   *      org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterUpdate(final PFUserDO obj, final PFUserDO dbObj)
  {
    super.afterUpdate(obj, dbObj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.UPDATE);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterUndelete(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterUndelete(final PFUserDO obj)
  {
    super.afterUndelete(obj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.UPDATE);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterDelete(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterDelete(final PFUserDO obj)
  {
    super.afterDelete(obj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.DELETE);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterSaveOrModify(final PFUserDO obj)
  {
    if (obj.isMinorChange() == false) {
      getUserCache().setExpired();
      getUserGroupCache().setExpired();
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final PFUserDO obj, final PFUserDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * @return false, if no admin user and the context user is not at minimum in one groups assigned to the given user or
   *         false. Also deleted and deactivated users are only visible for admin users.
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(org.projectforge.core.BaseDO, boolean)
   * @see AccessChecker#areUsersInSameGroup(PFUserDO, PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final PFUserDO obj, final boolean throwException)
  {
    boolean result = accessChecker.isUserMemberOfAdminGroup(user)
        || accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP);
    log.debug("UserDao hasSelectAccess. Check user member of admin, finance or controlling group: " + result);
    if (result == false && obj.hasSystemAccess() == true) {
      result = accessChecker.areUsersInSameGroup(user, obj);
      log.debug("UserDao hasSelectAccess. Caller user: " + user.getUsername() + " Check user: " + obj.getUsername()
          + " Check user in same group: " + result);
    }
    if (throwException == true && result == false) {
      throw new AccessException(user, AccessType.GROUP, OperationType.SELECT);
    }
    return result;
  }

  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return accessChecker.isUserMemberOfAdminGroup(user, false);
  }

  @Override
  protected void onSaveOrModify(final PFUserDO obj)
  {
    obj.checkAndFixPassword();
  }

  /**
   * Update user after login success.
   *
   * @param user the user
   */
  private void updateUserAfterLoginSuccess(PFUserDO user)
  {
    PfEmgrFactory.get().runInTrans((emgr) -> {
      CriteriaUpdate<PFUserDO> cu = CriteriaUpdate.createUpdate(PFUserDO.class);
      cu
          .set("lastLogin", new Timestamp(new Date().getTime()))
          .set("loginFailures", 0)
          .addWhere(Clauses.equal("id", user.getId()));
      return emgr.update(cu);
    });
  }

  private void updateIncrementLoginFailure(String userName)
  {
    PfEmgrFactory.get().runInTrans((emgr) -> {
      CriteriaUpdate<PFUserDO> cu = CriteriaUpdate.createUpdate(PFUserDO.class);
      cu
          .setExpression("loginFailures", "loginFailures + 1")
          .addWhere(Clauses.equal("username", userName));
      return emgr.update(cu);
    });
  }

  /**
   * Ohne Zugangsbegrenzung. Wird bei Anmeldung benötigt.
   * 
   * @param username
   * @param encryptedPassword
   * @return
   */
  public PFUserDO authenticateUser(final String username, final String password)
  {
    Validate.notNull(username);
    Validate.notNull(password);

    PFUserDO user = getUser(username, password, true);
    if (user != null) {

      final int loginFailures = user.getLoginFailures();
      final Timestamp lastLogin = user.getLastLogin();
      updateUserAfterLoginSuccess(user);
      if (user.hasSystemAccess() == false) {
        log.warn("Deleted/deactivated user tried to login: " + user);
        return null;
      }
      final PFUserDO contextUser = new PFUserDO();
      contextUser.copyValuesFrom(user);
      contextUser.setLoginFailures(loginFailures); // Restore loginFailures for current user session.
      contextUser.setLastLogin(lastLogin); // Restore lastLogin for current user session.
      contextUser.setPassword(null);
      return contextUser;
    }
    updateIncrementLoginFailure(username);
    return null;
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  protected PFUserDO getUser(final String username, final String password, final boolean updateSaltAndPepperIfNeeded)
  {
    final List<PFUserDO> list = (List<PFUserDO>) getHibernateTemplate().find("from PFUserDO u where u.username = ?",
        username);
    if (list == null || list.isEmpty() == true || list.get(0) == null) {
      return null;
    }
    final PFUserDO user = list.get(0);
    final PasswordCheckResult passwordCheckResult = checkPassword(user, password);
    if (passwordCheckResult.isOK() == false) {
      return null;
    }
    if (updateSaltAndPepperIfNeeded == true && passwordCheckResult.isPasswordUpdateNeeded() == true) {
      log.info("Giving salt and/or pepper to the password of the user " + user.getId() + ".");
      createEncryptedPassword(user, password);
    }
    return user;
  }

  @SuppressWarnings("unchecked")
  public PFUserDO getUserByStayLoggedInKey(final String username, final String stayLoggedInKey)
  {
    final List<PFUserDO> list = (List<PFUserDO>) getHibernateTemplate().find(
        "from PFUserDO u where u.username = ? and u.stayLoggedInKey = ?",
        new Object[] { username, stayLoggedInKey });
    PFUserDO user = null;
    if (list != null && list.isEmpty() == false && list.get(0) != null) {
      user = list.get(0);
    }
    if (user != null && user.hasSystemAccess() == false) {
      log.warn("Deleted/deactivated user tried to login (via stay-logged-in): " + user);
      return null;
    }
    return user;
  }

  /**
   * Does an user with the given username already exists? Works also for existing users (if username was modified).
   * 
   * @param user
   * @return
   */
  @SuppressWarnings("unchecked")
  public boolean doesUsernameAlreadyExist(final PFUserDO user)
  {
    Validate.notNull(user);
    List<PFUserDO> list = null;
    if (user.getId() == null) {
      // New user
      list = (List<PFUserDO>) getHibernateTemplate().find("from PFUserDO u where u.username = ?", user.getUsername());
    } else {
      // user already exists. Check maybe changed username:
      list = (List<PFUserDO>) getHibernateTemplate().find("from PFUserDO u where u.username = ? and pk <> ?",
          new Object[] { user.getUsername(), user.getId() });
    }
    if (CollectionUtils.isNotEmpty(list) == true) {
      return true;
    }
    return false;
  }

  /**
   * Encrypts the password with a new generated salt string and the pepper string if configured any.
   * 
   * @param user The user to user.
   * @param password as clear text.
   * @see Crypt#digest(String)
   */
  public void createEncryptedPassword(final PFUserDO user, final String password)
  {
    final String saltString = createSaltString();
    user.setPasswordSalt(saltString);
    final String encryptedPassword = Crypt.digest(getPepperString() + saltString + password);
    user.setPassword(encryptedPassword);
  }

  /**
   * Checks the given password by comparing it with the stored user password. For backward compatibility the password is
   * encrypted with and without pepper (if configured). The salt string of the given user is used.
   * 
   * @param user
   * @param password as clear text.
   * @return true if the password matches the user's password.
   */
  public PasswordCheckResult checkPassword(final PFUserDO user, final String password)
  {
    if (user == null) {
      log.warn("User not given in checkPassword(PFUserDO, String) method.");
      return PasswordCheckResult.FAILED;
    }
    final String userPassword = user.getPassword();
    if (StringUtils.isBlank(userPassword) == true) {
      log.warn("User's password is blank, can't checkPassword(PFUserDO, String) for user with id " + user.getId());
      return PasswordCheckResult.FAILED;
    }
    String saltString = user.getPasswordSalt();
    if (saltString == null) {
      saltString = "";
    }
    final String pepperString = getPepperString();
    String encryptedPassword = Crypt.digest(pepperString + saltString + password);
    if (userPassword.equals(encryptedPassword) == true) {
      // Passwords match!
      if (StringUtils.isEmpty(saltString) == true) {
        log.info("Password of user " + user.getId() + " with username '" + user.getUsername() + "' is not yet salted!");
        return PasswordCheckResult.OK_WITHOUT_SALT;
      }
      return PasswordCheckResult.OK;
    }
    if (StringUtils.isNotBlank(pepperString) == true) {
      // Check password without pepper:
      encryptedPassword = Crypt.digest(saltString + password);
      if (userPassword.equals(encryptedPassword) == true) {
        // Passwords match!
        if (StringUtils.isEmpty(saltString) == true) {
          log.info("Password of user " + user.getId() + " with username '" + user.getUsername()
              + "' is not yet salted and has no pepper!");
          return PasswordCheckResult.OK_WITHOUT_SALT_AND_PEPPER;
        }
        log.info("Password of user " + user.getId() + " with username '" + user.getUsername() + "' has no pepper!");
        return PasswordCheckResult.OK_WITHOUT_PEPPER;
      }
    }
    return PasswordCheckResult.FAILED;
  }

  private String createSaltString()
  {
    return NumberHelper.getSecureRandomBase64String(10);
  }

  private String getPepperString()
  {
    final SecurityConfig securityConfig = applicationContext.getBean(ConfigurationService.class).getSecurityConfig();
    if (securityConfig != null) {
      return securityConfig.getPasswordPepper();
    }
    return "";
  }

  /**
   * Changes the user's password. Checks the password quality and the correct authentication for the old password
   * before. Also the stay-logged-in-key will be renewed, so any existing stay-logged-in cookie will be invalid.
   * 
   * @param user
   * @param oldPassword
   * @param newPassword
   * @return Error message key if any check failed or null, if successfully changed.
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public String changePassword(PFUserDO user, final String oldPassword, final String newPassword)
  {
    Validate.notNull(user);
    Validate.notNull(oldPassword);
    Validate.notNull(newPassword);
    final String errorMsgKey = checkPasswordQuality(newPassword);
    if (errorMsgKey != null) {
      return errorMsgKey;
    }
    accessChecker.checkRestrictedOrDemoUser();
    user = getUser(user.getUsername(), oldPassword, false);
    if (user == null) {
      return MESSAGE_KEY_OLD_PASSWORD_WRONG;
    }
    createEncryptedPassword(user, newPassword);
    onPasswordChange(user);
    Login.getInstance().passwordChanged(user, newPassword);
    log.info("Password changed and stay-logged-key renewed for user: " + user.getId() + " - " + user.getUsername());
    return null;
  }

  public void onPasswordChange(final PFUserDO user)
  {
    user.checkAndFixPassword();
    if (user.getPassword() != null) {
      user.setStayLoggedInKey(createStayLoggedInKey());
      user.setLastPasswordChange(new Date());
    } else {
      throw new IllegalArgumentException(
          "Given password seems to be not encrypted! Aborting due to security reasons (for avoiding storage of clear password in the database).");
    }
  }

  /**
   * Returns the user's stay-logged-in key if exists (must be not blank with a size >= 10). If not, a new stay-logged-in
   * key will be generated.
   * 
   * @param userId
   * @return
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public String getStayLoggedInKey(final Integer userId)
  {
    final PFUserDO user = internalGetById(userId);
    if (StringUtils.isBlank(user.getStayLoggedInKey()) || user.getStayLoggedInKey().trim().length() < 10) {
      user.setStayLoggedInKey(createStayLoggedInKey());
      log.info("Stay-logged-key renewed for user: " + userId + " - " + user.getUsername());
    }
    return user.getStayLoggedInKey();
  }

  /**
   * Renews the user's stay-logged-in key (random string sequence).
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public void renewStayLoggedInKey(final Integer userId)
  {
    if (ThreadLocalUserContext.getUserId().equals(userId) == false) {
      // Only admin users are able to renew authentication token of other users:
      accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    }
    accessChecker.checkRestrictedOrDemoUser(); // Demo users are also not allowed to do this.
    final PFUserDO user = internalGetById(userId);
    user.setStayLoggedInKey(createStayLoggedInKey());
    log.info("Stay-logged-key renewed for user: " + userId + " - " + user.getUsername());
  }

  private String createStayLoggedInKey()
  {
    return NumberHelper.getSecureRandomUrlSaveString(STAY_LOGGED_IN_KEY_LENGTH);
  }

  /**
   * Get authentication key by user. ; )
   *
   * @param userName
   * @param authKey
   * @return
   */
  @SuppressWarnings("unchecked")
  public PFUserDO getUserByAuthenticationToken(final Integer userId, final String authKey)
  {
    final List<PFUserDO> list = (List<PFUserDO>) getHibernateTemplate().find(
        "from PFUserDO u where u.id = ? and u.authenticationToken = ?",
        new Object[] { userId, authKey });
    PFUserDO user = null;
    if (list != null && list.isEmpty() == false && list.get(0) != null) {
      user = list.get(0);
    }
    if (user != null && user.hasSystemAccess() == false) {
      log.warn("Deleted user tried to login (via authentication token): " + user);
      return null;
    }
    return user;
  }

  /**
   * Returns the user's authentication token if exists (must be not blank with a size >= 10). If not, a new token key
   * will be generated.
   * 
   * @param userId
   * @return
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public String getAuthenticationToken(final Integer userId)
  {
    final PFUserDO user = internalGetById(userId);
    if (StringUtils.isBlank(user.getAuthenticationToken()) || user.getAuthenticationToken().trim().length() < 10) {
      user.setAuthenticationToken(createAuthenticationToken());
      log.info("Authentication token renewed for user: " + userId + " - " + user.getUsername());
    }
    return user.getAuthenticationToken();
  }

  /**
   * Renews the user's authentication token (random string sequence).
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public void renewAuthenticationToken(final Integer userId)
  {
    if (ThreadLocalUserContext.getUserId().equals(userId) == false) {
      // Only admin users are able to renew authentication token of other users:
      accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    }
    accessChecker.checkRestrictedOrDemoUser(); // Demo users are also not allowed to do this.
    final PFUserDO user = internalGetById(userId);
    user.setAuthenticationToken(createAuthenticationToken());
    log.info("Authentication token renewed for user: " + userId + " - " + user.getUsername());
  }

  private String createAuthenticationToken()
  {
    return NumberHelper.getSecureRandomUrlSaveString(AUTHENTICATION_TOKEN_LENGTH);
  }

  /**
   * Uses the context user.
   * 
   * @param data
   * @return
   * @see #encrypt(Integer, String)
   */
  public String encrypt(final String data)
  {
    return encrypt(ThreadLocalUserContext.getUserId(), data);
  }

  /**
   * Encrypts the given str with AES. The key is the current authenticationToken of the given user (by id) (first 16
   * bytes of it).
   * 
   * @param userId
   * @param data
   * @return The base64 encoded result (url safe).
   * @see Crypt#encrypt(String, String)
   */
  public String encrypt(final Integer userId, final String data)
  {
    final String authenticationToken = StringUtils.rightPad(getCachedAuthenticationToken(userId), 32, "x");
    return Crypt.encrypt(authenticationToken, data);
  }

  /**
   * for faster access (due to permanent usage e. g. by subscription of calendars
   * 
   * @param userId
   * @return
   */
  public String getCachedAuthenticationToken(final Integer userId)
  {
    final PFUserDO user = getUserCache().getUser(userId);
    final String authenticationToken = user.getAuthenticationToken();
    if (StringUtils.isBlank(authenticationToken) == false && authenticationToken.trim().length() >= 10) {
      return authenticationToken;
    }
    return getAuthenticationToken(userId);
  }

  /**
   * @param userId
   * @param encryptedString
   * @return The decrypted string.
   * @see Crypt#decrypt(String, String)
   */
  public String decrypt(final Integer userId, final String encryptedString)
  {
    // final PFUserDO user = userCache.getUser(userId); // for faster access (due to permanent usage e. g. by subscription of calendars
    // (ics).
    final String authenticationToken = StringUtils.rightPad(getCachedAuthenticationToken(userId), 32, "x");
    return Crypt.decrypt(authenticationToken, encryptedString);
  }

  /**
   * Checks the password quality of a new password. Password must have at least 6 characters and at minimum one letter
   * and one non-letter character.
   * 
   * @param newPassword
   * @return null if password quality is OK, otherwise the i18n message key of the password check failure.
   */
  public String checkPasswordQuality(final String newPassword)
  {
    boolean letter = false;
    boolean nonLetter = false;
    if (newPassword == null || newPassword.length() < 6) {
      return MESSAGE_KEY_PASSWORD_QUALITY_CHECK;
    }
    for (int i = 0; i < newPassword.length(); i++) {
      final char ch = newPassword.charAt(i);
      if (letter == false && Character.isLetter(ch) == true) {
        letter = true;
      } else if (nonLetter == false && Character.isLetter(ch) == false) {
        nonLetter = true;
      }
    }
    if (letter == true && nonLetter == true) {
      return null;
    }
    return MESSAGE_KEY_PASSWORD_QUALITY_CHECK;
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public PFUserDO getInternalByName(final String username)
  {
    final List<PFUserDO> list = (List<PFUserDO>) getHibernateTemplate().find("from PFUserDO u where u.username = ?",
        username);
    if (list != null && list.size() > 0) {
      return list.get(0);
    }
    return null;
  }

  /**
   * User can modify own setting, this method ensures that only such properties will be updated, the user's are allowed
   * to.
   * 
   * @param user
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void updateMyAccount(final PFUserDO user)
  {
    accessChecker.checkRestrictedOrDemoUser();
    final PFUserDO contextUser = ThreadLocalUserContext.getUser();
    Validate.isTrue(user.getId().equals(contextUser.getId()) == true);
    final PFUserDO dbUser = getHibernateTemplate().load(clazz, user.getId(), LockMode.PESSIMISTIC_WRITE);
    if (copyValues(user, dbUser, "deleted", "password", "lastLogin", "loginFailures", "username", "stayLoggedInKey",
        "authenticationToken",
        "rights") != ModificationStatus.NONE) {
      dbUser.setLastUpdate();
      log.info("Object updated: " + dbUser.toString());
      copyValues(user, contextUser, "deleted", "password", "lastLogin", "loginFailures", "username", "stayLoggedInKey",
          "authenticationToken", "rights");
    } else {
      log.info("No modifications detected (no update needed): " + dbUser.toString());
    }
    getUserCache().updateUser(user);
    getUserGroupCache().updateUser(user);
  }

  /**
   * Gets history entries of super and adds all history entries of the AuftragsPositionDO childs.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final PFUserDO obj)
  {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (hasLoggedInUserHistoryAccess(obj, false) == false) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getRights()) == true) {
      for (final UserRightDO right : obj.getRights()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(right);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName(right.getRightIdString() + ":" + entry.getPropertyName()); // Prepend number of positon.
          } else {
            entry.setPropertyName(String.valueOf(right.getRightIdString()));
          }
        }
        list.addAll(entries);
      }
    }
    Collections.sort(list, new Comparator<DisplayHistoryEntry>()
    {
      @Override
      public int compare(final DisplayHistoryEntry o1, final DisplayHistoryEntry o2)
      {
        return (o2.getTimestamp().compareTo(o1.getTimestamp()));
      }
    });
    return list;
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException)
  {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * Re-index all dependent objects only if the username, first or last name was changed.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#wantsReindexAllDependentObjects(org.projectforge.core.ExtendedBaseDO,
   *      org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected boolean wantsReindexAllDependentObjects(final PFUserDO obj, final PFUserDO dbObj)
  {
    if (super.wantsReindexAllDependentObjects(obj, dbObj) == false) {
      return false;
    }
    return StringUtils.equals(obj.getUsername(), dbObj.getUsername()) == false
        || StringUtils.equals(obj.getFirstname(), dbObj.getFirstname()) == false
        || StringUtils.equals(obj.getLastname(), dbObj.getLastname()) == false;
  }

  @Override
  public PFUserDO newInstance()
  {
    return new PFUserDO();
  }
}
