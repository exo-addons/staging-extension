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

package org.exoplatform.management.mop.binding.xml;

import static org.gatein.common.xml.stax.writer.StaxWriterUtils.createWriter;
import static org.gatein.common.xml.stax.writer.StaxWriterUtils.writeOptionalElement;

import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelUnmarshaller;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.Page.PageSet;
import org.gatein.common.xml.stax.writer.StaxWriter;
import org.gatein.common.xml.stax.writer.WritableValueTypes;
import org.gatein.management.api.binding.BindingException;
import org.staxnav.StaxNavException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.stream.XMLStreamException;

/**
 * The Class PageMarshaller.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PageMarshaller extends AbstractMarshaller<Page.PageSet> {

  /**
   * {@inheritDoc}
   */
  public void marshal(PageSet object, OutputStream outputStream, boolean pretty) throws BindingException {
    marshal(object, outputStream);
  }

  /**
   * Marshal.
   *
   * @param pageSet the page set
   * @param outputStream the output stream
   * @throws BindingException the binding exception
   */
  public void marshal(Page.PageSet pageSet, OutputStream outputStream) throws BindingException {
    try {
      StaxWriter<Element> writer = createWriter(Element.class, outputStream);

      writer.writeStartElement(Element.PAGE_SET);
      writeGateinObjectsNamespace(writer);

      // Marshal pages
      for (Page page : pageSet.getPages()) {
        marshalPage(writer, page);
      }

      writer.finish();
    } catch (StaxNavException e) {
      throw new BindingException(e);
    } catch (XMLStreamException e) {
      throw new BindingException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page.PageSet unmarshal(InputStream inputStream) throws BindingException {
    try {
      return ModelUnmarshaller.unmarshall(Page.PageSet.class, inputStream).getObject();
    } catch (Exception e) {
      throw new BindingException(e);
    }
  }

  /**
   * Marshal page.
   *
   * @param writer the writer
   * @param page the page
   * @throws XMLStreamException the XML stream exception
   */
  private void marshalPage(StaxWriter<Element> writer, Page page) throws XMLStreamException {
    writer.writeStartElement(Element.PAGE);

    // name, title description
    writer.writeElement(Element.NAME, page.getName());
    writeOptionalElement(writer, Element.TITLE, page.getTitle());
    writeOptionalElement(writer, Element.DESCRIPTION, page.getDescription());

    // Access/Edit permissions
    marshalAccessPermissions(writer, page.getAccessPermissions());
    marshalEditPermission(writer, page.getEditPermission());

    writeOptionalElement(writer, Element.SHOW_MAX_WINDOW, WritableValueTypes.BOOLEAN, page.isShowMaxWindow());

    List<ModelObject> children = page.getChildren();
    for (ModelObject child : children) {
      marshalModelObject(writer, child);
    }

    writer.writeEndElement(); // End of page element
  }
}
