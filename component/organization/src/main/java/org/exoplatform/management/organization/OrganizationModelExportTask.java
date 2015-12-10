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
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class OrganizationModelExportTask implements ExportTask {
  private final Object organizationObject;
  private StringBuilder serializationPath = new StringBuilder(50);

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
      // no need for a / before the group id since the group id already
      // starts with a /
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_GROUP).append(((Group) organizationObject).getId()).append("/group.xml");
    } else if (organizationObject instanceof MembershipType) {
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_ROLE).append("/").append(((MembershipType) organizationObject).getName()).append("_role.xml");
    }
  }

  @Override
  public String getEntry() {
    return serializationPath.toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("organization", organizationObject.getClass());
    String xmlContent = xStream.toXML(organizationObject);
    outputStream.write(xmlContent.getBytes());
  }
}