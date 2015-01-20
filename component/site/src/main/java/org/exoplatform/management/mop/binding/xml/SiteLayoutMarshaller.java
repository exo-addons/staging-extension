/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelUnmarshaller;
import org.exoplatform.portal.config.model.PortalConfig;
import org.gatein.common.xml.stax.writer.StaxWriter;
import org.gatein.management.api.binding.BindingException;
import org.staxnav.StaxNavException;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class SiteLayoutMarshaller extends AbstractMarshaller<PortalConfig> {
  @Override
  public void marshal(PortalConfig object, OutputStream outputStream, boolean pretty) throws BindingException {
    marshal(object, outputStream);
  }

  public void marshal(PortalConfig object, OutputStream outputStream) throws BindingException {
    try {
      StaxWriter<Element> writer = createWriter(Element.class, outputStream);

      // root element
      writer.writeStartElement(Element.PORTAL_CONFIG);
      writeGateinObjectsNamespace(writer);

      marshalPortalConfig(writer, object);

      writer.finish();
    } catch (StaxNavException e) {
      throw new BindingException(e);
    } catch (XMLStreamException e) {
      throw new BindingException(e);
    }
  }

  @Override
  public PortalConfig unmarshal(InputStream is) throws BindingException {
    try {
      return ModelUnmarshaller.unmarshall(PortalConfig.class, is).getObject();
    } catch (Exception e) {
      throw new BindingException(e);
    }
  }

  private void marshalPortalConfig(StaxWriter<Element> writer, PortalConfig portalConfig) throws XMLStreamException {
    writer.writeElement(Element.PORTAL_NAME, portalConfig.getName());
    writeOptionalElement(writer, Element.LABEL, portalConfig.getLabel());
    writeOptionalElement(writer, Element.DESCRIPTION, portalConfig.getDescription());
    writeOptionalElement(writer, Element.LOCALE, portalConfig.getLocale());

    // Access permissions
    marshalAccessPermissions(writer, portalConfig.getAccessPermissions());

    // Edit permission
    marshalEditPermission(writer, portalConfig.getEditPermission());

    writeOptionalElement(writer, Element.SKIN, portalConfig.getSkin());

    boolean propertiesWritten = false;
    Map<String, String> properties = portalConfig.getProperties();
    if (properties != null) {
      for (String key : properties.keySet()) {
        if (!propertiesWritten) {
          writer.writeStartElement(Element.PROPERTIES);
          propertiesWritten = true;
        }
        String value = properties.get(key);
        if (value != null) {
          writer.writeStartElement(Element.PROPERTIES_ENTRY);
          writer.writeAttribute(Attribute.PROPERTIES_KEY.getLocalName(), key);
          writer.writeContent(value).writeEndElement();
        }
      }
      if (propertiesWritten) {
        writer.writeEndElement();
      }
    }

    Container container = portalConfig.getPortalLayout();
    if (container != null) {
      writer.writeStartElement(Element.PORTAL_LAYOUT);
      List<ModelObject> children = container.getChildren();
      if (children != null && !children.isEmpty()) {
        for (ModelObject child : children) {
          marshalModelObject(writer, child);
        }
      }
      writer.writeEndElement();
    }
  }

  private static enum Attribute
  {
    PROPERTIES_KEY("key");

    private final String name;

    Attribute(final String name) {
      this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
      return name;
    }
  }
}