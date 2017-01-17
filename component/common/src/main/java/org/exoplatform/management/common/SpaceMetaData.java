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
package org.exoplatform.management.common;

import org.exoplatform.social.core.space.model.Space;

import java.io.Serializable;

/**
 * The Class SpaceMetaData.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceMetaData implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -4671727410455859650L;

  /** The display name. */
  private String displayName;
  
  /** The app. */
  private String app;
  
  /** The description. */
  private String description;
  
  /** The editor. */
  private String editor;
  
  /** The managers. */
  private String[] managers;
  
  /** The members. */
  private String[] members;
  
  /** The invited users. */
  private String[] invitedUsers;
  
  /** The pretty name. */
  private String prettyName;
  
  /** The priority. */
  private String priority;
  
  /** The registration. */
  private String registration;
  
  /** The tag. */
  private String tag;
  
  /** The type. */
  private String type;
  
  /** The url. */
  private String url;
  
  /** The group id. */
  private String groupId;
  
  /** The visibility. */
  private String visibility;

  /**
   * Instantiates a new space meta data.
   */
  public SpaceMetaData() {}

  /**
   * Instantiates a new space meta data.
   *
   * @param space the space
   */
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
    this.groupId = space.getGroupId();
  }

  /**
   * Gets the display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the display name.
   *
   * @param displayName the new display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the app.
   *
   * @return the app
   */
  public String getApp() {
    return app;
  }

  /**
   * Sets the app.
   *
   * @param app the new app
   */
  public void setApp(String app) {
    this.app = app;
  }

  /**
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the editor.
   *
   * @return the editor
   */
  public String getEditor() {
    return editor;
  }

  /**
   * Sets the editor.
   *
   * @param editor the new editor
   */
  public void setEditor(String editor) {
    this.editor = editor;
  }

  /**
   * Gets the managers.
   *
   * @return the managers
   */
  public String[] getManagers() {
    return managers;
  }

  /**
   * Sets the managers.
   *
   * @param managers the new managers
   */
  public void setManagers(String[] managers) {
    this.managers = managers;
  }

  /**
   * Gets the members.
   *
   * @return the members
   */
  public String[] getMembers() {
    return members;
  }

  /**
   * Sets the members.
   *
   * @param members the new members
   */
  public void setMembers(String[] members) {
    this.members = members;
  }

  /**
   * Gets the pretty name.
   *
   * @return the pretty name
   */
  public String getPrettyName() {
    return prettyName;
  }

  /**
   * Sets the pretty name.
   *
   * @param prettyName the new pretty name
   */
  public void setPrettyName(String prettyName) {
    this.prettyName = prettyName;
  }

  /**
   * Gets the priority.
   *
   * @return the priority
   */
  public String getPriority() {
    return priority;
  }

  /**
   * Sets the priority.
   *
   * @param priority the new priority
   */
  public void setPriority(String priority) {
    this.priority = priority;
  }

  /**
   * Gets the registration.
   *
   * @return the registration
   */
  public String getRegistration() {
    return registration;
  }

  /**
   * Sets the registration.
   *
   * @param registration the new registration
   */
  public void setRegistration(String registration) {
    this.registration = registration;
  }

  /**
   * Gets the tag.
   *
   * @return the tag
   */
  public String getTag() {
    return tag;
  }

  /**
   * Sets the tag.
   *
   * @param tag the new tag
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the new type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Gets the url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the url.
   *
   * @param url the new url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets the visibility.
   *
   * @return the visibility
   */
  public String getVisibility() {
    return visibility;
  }

  /**
   * Sets the visibility.
   *
   * @param visibility the new visibility
   */
  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  /**
   * Gets the invited users.
   *
   * @return the invited users
   */
  public String[] getInvitedUsers() {
    return invitedUsers;
  }

  /**
   * Sets the invited users.
   *
   * @param invitedUsers the new invited users
   */
  public void setInvitedUsers(String[] invitedUsers) {
    this.invitedUsers = invitedUsers;
  }

  /**
   * Gets the group id.
   *
   * @return the group id
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Sets the group id.
   *
   * @param groupId the new group id
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }
}
