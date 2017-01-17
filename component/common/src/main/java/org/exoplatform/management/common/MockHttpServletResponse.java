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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * This is for a workaround for INTEG-333.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class MockHttpServletResponse implements HttpServletResponse {

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
  public String getContentType() {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServletOutputStream getOutputStream() throws IOException {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PrintWriter getWriter() throws IOException {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCharacterEncoding(String charset) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setContentLength(int len) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setContentType(String type) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setBufferSize(int size) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getBufferSize() {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flushBuffer() throws IOException {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetBuffer() {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCommitted() {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLocale(Locale loc) {

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
  @Override
  public void addCookie(Cookie cookie) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsHeader(String name) {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String encodeURL(String url) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String encodeRedirectURL(String url) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String encodeUrl(String url) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String encodeRedirectUrl(String url) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendError(int sc, String msg) throws IOException {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendError(int sc) throws IOException {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendRedirect(String location) throws IOException {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDateHeader(String name, long date) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addDateHeader(String name, long date) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setHeader(String name, String value) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addHeader(String name, String value) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIntHeader(String name, int value) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addIntHeader(String name, int value) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setStatus(int sc) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setStatus(int sc, String sm) {

  }

  /**
   * {@inheritDoc}
   */
  public int getStatus() {

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  public String getHeader(String name) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Collection<String> getHeaders(String name) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Collection<String> getHeaderNames() {

    return null;
  }

}
