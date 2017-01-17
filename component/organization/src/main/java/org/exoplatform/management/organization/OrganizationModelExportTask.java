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
package org.exoplatform.management.organization;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * The Class OrganizationModelExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class OrganizationModelExportTask implements ExportTask {
  
  /** The organization object. */
  private final Object organizationObject;
  
  /** The serialization path. */
  private StringBuilder serializationPath = new StringBuilder(50);

  /**
   * Instantiates a new organization model export task.
   *
   * @param organizationObject the organization object
   */
  public OrganizationModelExportTask(Object organizationObject) {
    serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION).append("/");

    this.organizationObject = organizationObject;
    if (organizationObject instanceof UserProfile) {
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_USER).append("/").append(((UserProfile) organizationObject).getUserName()).append("/profile.xml");
    } else if (organizationObject instanceof User) {
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_USER).append("/").append(((User) organizationObject).getUserName()).append("/user.xml");
    } else if (organizationObject instanceof Membership) {
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_USER).append("/").append(((Membership) organizationObject).getUserName()).append("/memberships/").append(((Membership) organizationObject).getId().replace(":", "-")).append("_membership.xml");
    } else if (organizationObject instanceof Group) {
      // no need for a / before the group id since the group id already starts
      // with a /
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_GROUP).append(((Group) organizationObject).getId()).append("/group.xml");
    } else if (organizationObject instanceof MembershipType) {
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_ROLE).append("/").append(((MembershipType) organizationObject).getName()).append("_role.xml");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return serializationPath.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("organization", organizationObject.getClass());
    String xmlContent = xStream.toXML(organizationObject);
    outputStream.write(xmlContent.getBytes());
  }
}