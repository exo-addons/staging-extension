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
  private String serializationPath;

  public OrganizationModelExportTask(Object organizationObject) {
    this.organizationObject = organizationObject;
    if (organizationObject instanceof UserProfile) {
      serializationPath = "users/" + ((UserProfile) organizationObject).getUserName() + "/profile.xml";
    } else if (organizationObject instanceof User) {
      serializationPath = "users/" + ((User) organizationObject).getUserName() + "/user.xml";
    } else if (organizationObject instanceof Membership) {
      serializationPath = "users/" + ((Membership) organizationObject).getUserName() + "/memberships/" + ((Membership) organizationObject).getId().replace(":", "-") + "_membership.xml";
    } else if (organizationObject instanceof Group) {
      serializationPath = "groups" + ((Group) organizationObject).getId() + "/group.xml";
    } else if (organizationObject instanceof MembershipType) {
      serializationPath = "roles/" + ((MembershipType) organizationObject).getName() + "_role.xml";
    }
  }

  @Override
  public String getEntry() {
    return serializationPath;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("organization", organizationObject.getClass());
    String xmlContent = xStream.toXML(organizationObject);
    outputStream.write(xmlContent.getBytes());
  }
}