package org.exoplatform.management.backup.service;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.management.backup.operations.BackupImportResource;
import org.exoplatform.web.filter.Filter;

public class BackupInProgressFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (BackupImportResource.restoreInProgress) {
      String content = IOUtil.getResourceAsString("html/backupInProgress.html");
      response.getWriter().append(content);
      return;
    }
    try {
      chain.doFilter(request, response);
    } catch (Throwable e) {
      if (checkException(e)) {
        response.getWriter().append("Backup or a restore is in progress. No write operation is allowed.");
      }
    }
  }

  private boolean checkException(Throwable e) {
    if (e == null) {
      return false;
    } else if (e instanceof BackupInProgressException) {
      return true;
    } else if (e.getCause() == null) {
      return false;
    } else {
      return checkException(e.getCause());
    }
  }

}
