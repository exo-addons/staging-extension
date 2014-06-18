package org.exoplatform.management.forum.operations;

import java.io.Serializable;

import org.exoplatform.social.core.space.model.Space;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceMetaData implements Serializable {

  private static final long serialVersionUID = 8423379678046279118L;

  private String displayName;
  private String app;
  private String description;
  private String editor;
  private String[] managers;
  private String[] members;
  private String[] invitedUsers;
  private String prettyName;
  private String priority;
  private String registration;
  private String tag;
  private String type;
  private String url;
  private String visibility;

  public SpaceMetaData() {
  }

  public SpaceMetaData(Space space) {
    this.displayName = space.getDisplayName();
    this.app = space.getApp();
    this.description = space.getDescription();
    this.editor = space.getEditor();
    this.managers = space.getManagers();
    this.members = space.getMembers();
    this.invitedUsers = space.getInvitedUsers();
    this.prettyName = space.getPrettyName();
    this.priority = space.getPriority();
    this.registration = space.getRegistration();
    this.tag = space.getTag();
    this.type = space.getType();
    this.url = space.getUrl();
    this.visibility = space.getVisibility();
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getApp() {
    return app;
  }

  public void setApp(String app) {
    this.app = app;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEditor() {
    return editor;
  }

  public void setEditor(String editor) {
    this.editor = editor;
  }

  public String[] getManagers() {
    return managers;
  }

  public void setManagers(String[] managers) {
    this.managers = managers;
  }

  public String[] getMembers() {
    return members;
  }

  public void setMembers(String[] members) {
    this.members = members;
  }

  public String getPrettyName() {
    return prettyName;
  }

  public void setPrettyName(String prettyName) {
    this.prettyName = prettyName;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getRegistration() {
    return registration;
  }

  public void setRegistration(String registration) {
    this.registration = registration;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public String[] getInvitedUsers() {
    return invitedUsers;
  }

  public void setInvitedUsers(String[] invitedUsers) {
    this.invitedUsers = invitedUsers;
  }
}
