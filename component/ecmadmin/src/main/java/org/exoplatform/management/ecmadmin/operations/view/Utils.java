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
package org.exoplatform.management.ecmadmin.operations.view;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.impl.UnmarshallingContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * The Class Utils.
 */
public class Utils {

  /**
   * To XML.
   *
   * @param obj the obj
   * @return the byte[]
   * @throws Exception the exception
   */
  public static byte[] toXML(Object obj) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IBindingFactory bfact = BindingDirectory.getFactory(obj.getClass());
    IMarshallingContext mctx = bfact.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(obj, "UTF-8", null, out);
    String content = new String(out.toByteArray());
    content = content.replaceAll("<field name=\"([A-z])*\"/>", "");
    return content.getBytes();
  }

  /**
   * From XML.
   *
   * @param <T> the generic type
   * @param bytes the bytes
   * @param clazz the clazz
   * @return the t
   * @throws Exception the exception
   */
  public static <T> T fromXML(byte[] bytes, Class<T> clazz) throws Exception {
    ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
    IBindingFactory bfact = BindingDirectory.getFactory(clazz);
    UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
    Object obj = uctx.unmarshalDocument(baos, "UTF-8");
    return clazz.cast(obj);
  }

}
