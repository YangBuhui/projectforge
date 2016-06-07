package org.apache.wicket.util.tester;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;

/**
 * Mockup request for servlet.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class Servlet3MockHttpServletRequest extends MockHttpServletRequest
{

  public Servlet3MockHttpServletRequest(Application application, HttpSession session, ServletContext context)
  {
    super(application, session, context);
  }

  @Override
  public String changeSessionId()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void login(String username, String password) throws ServletException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void logout() throws ServletException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException
  {
    return Collections.emptyList();
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getContentLengthLong()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IllegalStateException
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAsyncStarted()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAsyncSupported()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public AsyncContext getAsyncContext()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DispatcherType getDispatcherType()
  {
    return DispatcherType.REQUEST;
  }

}
