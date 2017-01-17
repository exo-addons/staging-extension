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
package org.exoplatform.management.backup.service.web;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.operations.BackupImportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.web.filter.Filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The Class BackupInProgressFilter.
 */
public class BackupInProgressFilter implements Filter {
  
  /** The Constant MAINTENANCE_PAGE_PATH. */
  private static final String MAINTENANCE_PAGE_PATH = System.getProperty("exo.staging.maintenance.page", "html/backupInProgress.html");
  
  /** The maintenance page content. */
  private static String MAINTENANCE_PAGE_CONTENT;
  static {
    try {
      MAINTENANCE_PAGE_CONTENT = IOUtil.getResourceAsString(MAINTENANCE_PAGE_PATH);
    } catch (IOException e) {
      MAINTENANCE_PAGE_CONTENT = "Server is in maintenance";
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (BackupImportResource.restoreInProgress) {
      displayBackupMaintenancePage(response);
      return;
    }
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    try {
      if (BackupExportResource.backupInProgress) {
        checkWantLogout(httpServletRequest, httpServletResponse);
      }
      chain.doFilter(request, response);
    } catch (Throwable e) {
      if (checkException(e)) {
        if (checkWantLogout(httpServletRequest, httpServletResponse)) {
          // Nothing to do
        } else if (checkLoginURI(httpServletRequest)) {
          httpServletResponse.sendRedirect("/");
        } else {
          displayBackupMaintenancePage(response);
        }
      } else {
        throw e;
      }
    }
  }

  /**
   * Display backup maintenance page.
   *
   * @param response the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void displayBackupMaintenancePage(ServletResponse response) throws IOException {
    response.getWriter().append(MAINTENANCE_PAGE_CONTENT);
  }

  /**
   * Check want logout.
   *
   * @param httpServletRequest the http servlet request
   * @param httpServletResponse the http servlet response
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private boolean checkWantLogout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
    if (checkLogoutURI(httpServletRequest)) {
      try {
        httpServletRequest.getSession().invalidate();
      } catch (Exception e) {
        // Nothing to do
      }
      Cookie[] cookies = httpServletRequest.getCookies();
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("rememberme")) {
          cookie.setMaxAge(0);
          httpServletResponse.addCookie(cookie);
          break;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Check login URI.
   *
   * @param request the request
   * @return true, if successful
   */
  private boolean checkLoginURI(HttpServletRequest request) {
    return ((HttpServletRequest) request).getRequestURI().contains("/login");
  }

  /**
   * Check logout URI.
   *
   * @param request the request
   * @return true, if successful
   */
  private boolean checkLogoutURI(HttpServletRequest request) {
    return ((HttpServletRequest) request).getRequestURI().toLowerCase().contains("logout")
        || (((HttpServletRequest) request).getQueryString() != null && ((HttpServletRequest) request).getQueryString().toLowerCase().contains("=logout"));
  }

  /**
   * Check exception.
   *
   * @param e the e
   * @return true, if successful
   */
  private boolean checkException(Throwable e) {
    if (e == null) {
      return false;
    } else if (e instanceof BackupInProgressException) {
      BackupInProgressException.untreatedException.set(false);
      return true;
    } else if (e.getCause() == null) {
      return false;
    } else {
      return checkException(e.getCause());
    }
  }

}
