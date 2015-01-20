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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.exoplatform.portal.config.model.LocalizedString;
import org.exoplatform.portal.config.model.ModelUnmarshaller;
import org.exoplatform.portal.config.model.NavigationFragment;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.gatein.common.xml.stax.writer.StaxWriter;
import org.gatein.common.xml.stax.writer.WritableValueTypes;
import org.gatein.management.api.binding.BindingException;
import org.gatein.management.api.binding.Marshaller;
import org.staxnav.StaxNavException;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class NavigationMarshaller implements Marshaller<PageNavigation> {

  public void marshal(PageNavigation object, OutputStream outputStream, boolean pretty) throws BindingException {
    marshal(object, outputStream);
  }

  public void marshal(PageNavigation navigation, OutputStream outputStream) throws BindingException {
    try {
      StaxWriter<Element> writer = createWriter(Element.class, outputStream);
      marshalNavigation(writer, navigation);
    } catch (StaxNavException e) {
      throw new BindingException(e);
    } catch (XMLStreamException e) {
      throw new BindingException(e);
    }
  }

  @Override
  public PageNavigation unmarshal(InputStream is) throws BindingException {
    try {
      return ModelUnmarshaller.unmarshall(PageNavigation.class, is).getObject();
    } catch (Exception e) {
      throw new BindingException(e);
    }
  }

  private void marshalNavigation(StaxWriter<Element> writer, PageNavigation navigation) throws XMLStreamException {
    writer.writeStartElement(Element.NODE_NAVIGATION);

    // Write gatein_objects xml namespace
    Utils.writeGateinObjectsNamespace(writer);

    // Priority
    writer.writeElement(Element.PRIORITY, WritableValueTypes.INTEGER, navigation.getPriority());

    // Page nodes
    ArrayList<NavigationFragment> fragments = navigation.getFragments();
    for (NavigationFragment fragment : fragments) {
      writer.writeStartElement(Element.PAGE_NODES);
      if (fragment.getParentURI() != null) {
        String parentUri = fragment.getParentURI();
        writeOptionalElement(writer, Element.PARENT_URI, parentUri);
      }

      Collection<PageNode> nodes = fragment.getNodes();
      if (nodes != null && !nodes.isEmpty()) {
        for (PageNode node : nodes) {
          marshallNode(writer, node);
        }
      }
      writer.writeEndElement(); // End page-nodes
    }

    writer.writeEndElement(); // End node-navigation
  }

  public void marshallNode(StaxWriter<Element> writer, PageNode node) throws XMLStreamException {
    writer.writeStartElement(Element.NODE);
    writer.writeElement(Element.NAME, node.getName());

    if (node.getLabels() != null) {
      for (LocalizedString label : node.getLabels()) {
        if (label.getValue() == null)
          continue;

        writer.writeStartElement(Element.LABEL);
        if (label.getLang() != null) {
          String localeString = label.getLang().getLanguage();
          if (localeString == null) {
            throw new XMLStreamException("Language was null for locale " + label.getLang());
          }
          String country = label.getLang().getCountry();
          if (country != null && country.length() > 0) {
            localeString += "-" + country.toLowerCase();
          }

          writer.writeAttribute(new QName(XMLConstants.XML_NS_URI, "lang", XMLConstants.XML_NS_PREFIX), localeString);
        }
        writer.writeContent(label.getValue()).writeEndElement();
      }
    }

    writeOptionalElement(writer, Element.ICON, node.getIcon());

    writeOptionalElement(writer, Element.START_PUBLICATION_DATE, WritableValueTypes.DATE_TIME, node.getStartPublicationDate());
    writeOptionalElement(writer, Element.END_PUBLICATION_DATE, WritableValueTypes.DATE_TIME, node.getEndPublicationDate());

    String visibility = (node.getVisibility() == null) ? null : node.getVisibility().name();
    writeOptionalElement(writer, Element.VISIBILITY, visibility);
    writeOptionalElement(writer, Element.PAGE_REFERENCE, node.getPageReference());

    // Marshall children
    List<PageNode> children = node.getNodes();
    if (children != null && !children.isEmpty()) {
      for (PageNode child : children) {
        marshallNode(writer, child);
      }
    }

    writer.writeEndElement(); // End of node
  }
}
