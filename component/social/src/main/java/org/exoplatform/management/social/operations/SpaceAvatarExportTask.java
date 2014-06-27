package org.exoplatform.management.social.operations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.exoplatform.social.core.model.AvatarAttachment;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

public class SpaceAvatarExportTask implements ExportTask {
  public static final String FILENAME = "spaceAvatar.metadata";
  private String spacePrettyName;
  private AvatarAttachment avatarAttachment;

  public SpaceAvatarExportTask(String spacePrettyName, AvatarAttachment avatarAttachment) {
    this.spacePrettyName = spacePrettyName;
    this.avatarAttachment = avatarAttachment;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("social/space/").append(spacePrettyName).append("/").append(FILENAME).toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    xStream.toXML(avatarAttachment, writer);
    writer.flush();
  }

}
