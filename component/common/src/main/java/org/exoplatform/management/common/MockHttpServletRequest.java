/*
 * Copyright (C) 2009 eXo Platform SAS.
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
 * 
 * This is for a workaround for INTEG-333
 * 
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "deprecation" })
public class MockHttpServletRequest implements HttpServletRequest {

  @Override
  public Object getAttribute(String name) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getAttributeNames() {

    return null;
  }

  @Override
  public String getCharacterEncoding() {

    return null;
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

  }

  @Override
  public int getContentLength() {

    return 0;
  }

  @Override
  public String getContentType() {

    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {

    return null;
  }

  @Override
  public String getParameter(String name) {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getParameterNames() {

    return null;
  }

  @Override
  public String[] getParameterValues(String name) {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map getParameterMap() {
    return Collections.emptyMap();
  }

  @Override
  public String getProtocol() {

    return null;
  }

  @Override
  public String getScheme() {

    return null;
  }

  @Override
  public String getServerName() {

    return null;
  }

  @Override
  public int getServerPort() {

    return 0;
  }

  @Override
  public BufferedReader getReader() throws IOException {

    return null;
  }

  @Override
  public String getRemoteAddr() {

    return null;
  }

  @Override
  public String getRemoteHost() {

    return null;
  }

  @Override
  public void setAttribute(String name, Object o) {

  }

  @Override
  public void removeAttribute(String name) {

  }

  @Override
  public Locale getLocale() {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getLocales() {

    return null;
  }

  @Override
  public boolean isSecure() {

    return false;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {

    return null;
  }

  @Override
  public String getRealPath(String path) {

    return null;
  }

  @Override
  public int getRemotePort() {

    return 0;
  }

  @Override
  public String getLocalName() {

    return null;
  }

  @Override
  public String getLocalAddr() {

    return null;
  }

  @Override
  public int getLocalPort() {

    return 0;
  }

  @Override
  public String getAuthType() {

    return null;
  }

  @Override
  public Cookie[] getCookies() {

    return null;
  }

  @Override
  public long getDateHeader(String name) {

    return 0;
  }

  @Override
  public String getHeader(String name) {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getHeaders(String name) {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getHeaderNames() {

    return null;
  }

  @Override
  public int getIntHeader(String name) {

    return 0;
  }

  @Override
  public String getMethod() {

    return null;
  }

  @Override
  public String getPathInfo() {

    return null;
  }

  @Override
  public String getPathTranslated() {

    return null;
  }

  @Override
  public String getContextPath() {
    return "/portal/intranet/calendar";
  }

  @Override
  public String getQueryString() {

    return null;
  }

  @Override
  public String getRemoteUser() {

    return null;
  }

  @Override
  public boolean isUserInRole(String role) {

    return false;
  }

  @Override
  public Principal getUserPrincipal() {

    return null;
  }

  @Override
  public String getRequestedSessionId() {

    return null;
  }

  @Override
  public String getRequestURI() {

    return null;
  }

  @Override
  public StringBuffer getRequestURL() {

    return null;
  }

  @Override
  public String getServletPath() {

    return null;
  }

  @Override
  public HttpSession getSession(boolean create) {

    return null;
  }

  @Override
  public HttpSession getSession() {
    return new MockHttpSession();
  }

  @Override
  public boolean isRequestedSessionIdValid() {

    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {

    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {

    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {

    return false;
  }

  public class MockHttpSession implements HttpSession {

    @Override
    public long getCreationTime() {

      return 0;
    }

    @Override
    public String getId() {
      return "MockSessionId";
    }

    @Override
    public long getLastAccessedTime() {

      return 0;
    }

    @Override
    public ServletContext getServletContext() {

      return null;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {

    }

    @Override
    public int getMaxInactiveInterval() {

      return 0;
    }

    @Override
    public HttpSessionContext getSessionContext() {

      return null;
    }

    @Override
    public Object getAttribute(String name) {

      return null;
    }

    @Override
    public Object getValue(String name) {

      return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getAttributeNames() {

      return null;
    }

    @Override
    public String[] getValueNames() {

      return null;
    }

    @Override
    public void setAttribute(String name, Object value) {

    }

    @Override
    public void putValue(String name, Object value) {

    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public void removeValue(String name) {

    }

    @Override
    public void invalidate() {

    }

    @Override
    public boolean isNew() {

      return false;
    }
  }

  public ServletContext getServletContext() {

    return null;
  }

  public AsyncContext startAsync() throws IllegalStateException {

    return null;
  }

  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {

    return null;
  }

  public boolean isAsyncStarted() {

    return false;
  }

  public boolean isAsyncSupported() {

    return false;
  }

  public AsyncContext getAsyncContext() {

    return null;
  }

  public DispatcherType getDispatcherType() {

    return null;
  }

  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {

    return false;
  }

  public void login(String username, String password) throws ServletException {

  }

  public void logout() throws ServletException {

  }

  public Collection<Part> getParts() throws IOException, ServletException {

    return null;
  }

  public Part getPart(String name) throws IOException, ServletException {

    return null;
  }

}