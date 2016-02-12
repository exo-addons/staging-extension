package org.exoplatform.management.backup.service.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.operations.BackupImportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.web.filter.Filter;

public class BackupInProgressFilter implements Filter {
  private static final String MAINTENANCE_PAGE_PATH = System.getProperty("exo.staging.maintenance.page", "html/backupInProgress.html");
  private static String MAINTENANCE_PAGE_CONTENT;
  static {
    try {
      MAINTENANCE_PAGE_CONTENT = IOUtil.getResourceAsString(MAINTENANCE_PAGE_PATH);
    } catch (IOException e) {
      MAINTENANCE_PAGE_CONTENT = "Server is in maintenance";
    }
  }

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

  private void displayBackupMaintenancePage(ServletResponse response) throws IOException {
    response.getWriter().append(MAINTENANCE_PAGE_CONTENT);
  }

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

  private boolean checkLoginURI(HttpServletRequest request) {
    return ((HttpServletRequest) request).getRequestURI().contains("/login");
  }

  private boolean checkLogoutURI(HttpServletRequest request) {
    return ((HttpServletRequest) request).getRequestURI().toLowerCase().contains("logout")
        || (((HttpServletRequest) request).getQueryString() != null && ((HttpServletRequest) request).getQueryString().toLowerCase().contains("=logout"));
  }

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
