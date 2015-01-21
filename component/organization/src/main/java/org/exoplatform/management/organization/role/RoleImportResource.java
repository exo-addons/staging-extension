package org.exoplatform.management.organization.role;

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.organization.OrganizationManagementExtension;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class RoleImportResource extends AbstractOperationHandler {
  private static final Log log = ExoLogger.getLogger(RoleImportResource.class);
  private OrganizationService organizationService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (organizationService == null) {
      organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    }

    InputStream attachmentInputStream = null;

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // get attachement input stream
    OperationAttachment attachment = operationContext.getAttachment(false);
    attachmentInputStream = attachment.getStream();

    final ZipInputStream zin = new ZipInputStream(attachmentInputStream);
    ZipEntry entry;
    try {
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (!filePath.startsWith(OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_ROLE + "/")) {
          continue;
        }
        if (entry.isDirectory() || filePath.trim().isEmpty() || !filePath.endsWith(".xml")) {
          continue;
        }
        if (filePath.endsWith("_role.xml")) {
          log.debug("Parsing : " + filePath);
          createRole(zin, replaceExisting);
        }
        try {
          zin.closeEntry();
        } catch (Exception e) {
          // Already closed, expected
        }
      }
      zin.close();
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while reading group from Stream.", e);
    }
  }

  private void createRole(final ZipInputStream zin, Boolean replaceExisting) throws Exception {
    MembershipType membershipType = deserializeObject(zin, MembershipTypeImpl.class);
    MembershipType oldMembershipType = organizationService.getMembershipTypeHandler().findMembershipType(membershipType.getName());
    boolean alreadyExists = (oldMembershipType != null);
    if (alreadyExists && replaceExisting) {
      log.info("ReplaceExisting is On: Deleting role '" + membershipType.getName() + "'");
      organizationService.getMembershipTypeHandler().removeMembershipType(membershipType.getName(), true);
      oldMembershipType = null;
    }
    if (oldMembershipType == null) {
      organizationService.getMembershipTypeHandler().createMembershipType(membershipType, true);
    } else {
      log.info("ReplaceExisting is Off: Ignoring role '" + membershipType.getName() + "'");
    }
  }

  private <T> T deserializeObject(final ZipInputStream zin, Class<T> objectClass) {
    XStream xStream = new XStream();
    xStream.alias("organization", objectClass);
    @SuppressWarnings("unchecked")
    T object = (T) xStream.fromXML(zin);
    return object;
  }
}
