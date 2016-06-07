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

package org.projectforge.web.rest;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.MDC;
import org.projectforge.business.login.LoginProtection;
import org.projectforge.business.user.UserCache;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.rest.Authentication;
import org.projectforge.rest.ConnectionSettings;
import org.projectforge.rest.converter.DateTimeFormat;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.ClientIpResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Does the authentication stuff for restfull requests.
 * 
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RestUserFilter implements Filter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RestUserFilter.class);

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserCache userCache;

  private WebApplicationContext springContext;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException
  {
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
  }

  /**
   * Authentication via request header.
   * <ol>
   * <li>Authentication userId (authenticationUserId) and authenticationToken (authenticationToken) or</li>
   * <li>Authentication username (authenticationUsername) and password (authenticationPassword) or</li>
   * </ol>
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException,
      ServletException
  {
    if (WebConfiguration.isUpAndRunning() == false) {
      log.error(
          "System isn't up and running, rest call denied. The system is may-be in start-up phase or in maintenance mode.");
      final HttpServletResponse resp = (HttpServletResponse) response;
      resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }
    final HttpServletRequest req = (HttpServletRequest) request;
    String userString = getAttribute(req, Authentication.AUTHENTICATION_USER_ID);
    final LoginProtection loginProtection = LoginProtection.instance();
    final String clientIpAddress = ClientIpResolver.getClientIp(request);
    PFUserDO user = null;
    if (userString != null) {
      final Integer userId = NumberHelper.parseInteger(userString);
      if (userId != null) {
        final long offset = loginProtection.getFailedLoginTimeOffsetIfExists(userString, clientIpAddress);
        if (offset > 0) {
          final String seconds = String.valueOf(offset / 1000);
          log.warn("The account for '"
              + userString
              + "' is locked for "
              + seconds
              + " seconds due to failed login attempts (ip=" + clientIpAddress + ").");
          final HttpServletResponse resp = (HttpServletResponse) response;
          resp.sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }
        final String authenticationToken = getAttribute(req, Authentication.AUTHENTICATION_TOKEN);
        if (authenticationToken != null) {
          if (authenticationToken.equals(userDao.getCachedAuthenticationToken(userId)) == true) {
            user = userDao.getUserCache().getUser(userId);
          } else {
            log.error(Authentication.AUTHENTICATION_TOKEN
                + " doesn't match for "
                + Authentication.AUTHENTICATION_USER_ID
                + " '"
                + userId
                + "'. Rest call forbidden.");
          }
        } else {
          log.error(
              Authentication.AUTHENTICATION_TOKEN + " not given for userId '" + userId + "'. Rest call forbidden.");
        }
      } else {
        log.error(
            Authentication.AUTHENTICATION_USER_ID + " is not an integer: '" + userString + "'. Rest call forbidden.");
      }
    } else {
      userString = getAttribute(req, Authentication.AUTHENTICATION_USERNAME);
      final String password = getAttribute(req, Authentication.AUTHENTICATION_PASSWORD);
      final long offset = loginProtection.getFailedLoginTimeOffsetIfExists(userString, clientIpAddress);
      if (offset > 0) {
        final String seconds = String.valueOf(offset / 1000);
        log.warn("The account for '"
            + userString
            + "' is locked for "
            + seconds
            + " seconds due to failed login attempts (ip=" + clientIpAddress + ").");
        final HttpServletResponse resp = (HttpServletResponse) response;
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      if (userString != null && password != null) {
        user = userDao.authenticateUser(userString, password);
        if (user == null) {
          log.error("Authentication failed for "
              + Authentication.AUTHENTICATION_USERNAME
              + "='"
              + userString
              + "' with given password. Rest call forbidden.");
        }
      } else {
        log.error("Neither "
            + Authentication.AUTHENTICATION_USER_ID
            + " nor "
            + Authentication.AUTHENTICATION_USERNAME
            + "/"
            + Authentication.AUTHENTICATION_PASSWORD
            + " is given. Rest call forbidden.");
      }
    }
    if (user == null) {
      loginProtection.incrementFailedLoginTimeOffset(userString, clientIpAddress);
      final HttpServletResponse resp = (HttpServletResponse) response;
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    try {
      loginProtection.clearLoginTimeOffset(userString, clientIpAddress);
      ThreadLocalUserContext.setUser(userCache, user);
      final ConnectionSettings settings = getConnectionSettings(req);
      ConnectionSettings.set(settings);
      final String ip = request.getRemoteAddr();
      if (ip != null) {
        MDC.put("ip", ip);
      } else {
        // Only null in test case:
        MDC.put("ip", "unknown");
      }
      MDC.put("user", user.getUsername());
      log.info("Rest-call: " + ((HttpServletRequest) request).getRequestURI());
      chain.doFilter(request, response);
    } finally {
      ThreadLocalUserContext.setUser(userCache, null);
      ConnectionSettings.set(null);
      MDC.remove("ip");
      MDC.remove("user");
    }
  }

  private ConnectionSettings getConnectionSettings(final HttpServletRequest req)
  {
    final ConnectionSettings settings = new ConnectionSettings();
    final String dateTimeFormatString = getAttribute(req, ConnectionSettings.DATE_TIME_FORMAT);
    if (dateTimeFormatString != null) {
      final DateTimeFormat dateTimeFormat = DateTimeFormat.valueOf(dateTimeFormatString.toUpperCase());
      if (dateTimeFormat != null) {
        settings.setDateTimeFormat(dateTimeFormat);
      }
    }
    return settings;
  }

  private String getAttribute(final HttpServletRequest req, final String key)
  {
    String value = req.getHeader(key);
    if (value == null) {
      value = req.getParameter(key);
    }
    return value;
  }

  @Override
  public void destroy()
  {
    // NOOP
  }

  /**
   * Only for tests
   * 
   * @param userDao
   */
  public void setUserDao(UserDao userDao)
  {
    this.userDao = userDao;
  }

  /**
   * Only for tests
   * 
   * @param userDao
   */
  public void setUserCache(UserCache userCache)
  {
    this.userCache = userCache;
  }

}
