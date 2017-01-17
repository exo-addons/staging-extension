/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.management.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.Part;

/**
 * This is for a workaround for INTEG-333.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "deprecation" })
public class MockHttpServletRequest implements HttpServletRequest {

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getAttribute(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getAttributeNames() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCharacterEncoding() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getContentLength() {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContentType() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServletInputStream getInputStream() throws IOException {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getParameter(String name) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getParameterNames() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getParameterValues(String name) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Map getParameterMap() {
    return Collections.emptyMap();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getProtocol() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getScheme() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getServerName() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getServerPort() {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BufferedReader getReader() throws IOException {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRemoteAddr() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRemoteHost() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttribute(String name, Object o) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAttribute(String name) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Locale getLocale() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getLocales() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSecure() {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestDispatcher getRequestDispatcher(String path) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRealPath(String path) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getRemotePort() {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLocalName() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLocalAddr() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getLocalPort() {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAuthType() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Cookie[] getCookies() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getDateHeader(String name) {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHeader(String name) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getHeaders(String name) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getHeaderNames() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getIntHeader(String name) {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMethod() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPathInfo() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPathTranslated() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContextPath() {
    return "/portal/intranet/calendar";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getQueryString() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRemoteUser() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUserInRole(String role) {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Principal getUserPrincipal() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRequestedSessionId() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRequestURI() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StringBuffer getRequestURL() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getServletPath() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpSession getSession(boolean create) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpSession getSession() {
    return new MockHttpSession();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRequestedSessionIdValid() {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRequestedSessionIdFromCookie() {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRequestedSessionIdFromURL() {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRequestedSessionIdFromUrl() {

    return false;
  }

  /**
   * The Class MockHttpSession.
   */
  public class MockHttpSession implements HttpSession {

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreationTime() {

      return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
      return "MockSessionId";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastAccessedTime() {

      return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletContext getServletContext() {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxInactiveInterval(int interval) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxInactiveInterval() {

      return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpSessionContext getSessionContext() {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(String name) {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(String name) {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getAttributeNames() {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getValueNames() {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(String name, Object value) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putValue(String name, Object value) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAttribute(String name) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeValue(String name) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidate() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNew() {

      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public ServletContext getServletContext() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public AsyncContext startAsync() throws IllegalStateException {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isAsyncStarted() {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isAsyncSupported() {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  public AsyncContext getAsyncContext() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public DispatcherType getDispatcherType() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void login(String username, String password) throws ServletException {

  }

  /**
   * {@inheritDoc}
   */
  public void logout() throws ServletException {

  }

  /**
   * {@inheritDoc}
   */
  public Collection<Part> getParts() throws IOException, ServletException {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Part getPart(String name) throws IOException, ServletException {

    return null;
  }

}