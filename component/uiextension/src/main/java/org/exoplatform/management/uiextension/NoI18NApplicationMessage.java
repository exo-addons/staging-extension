package org.exoplatform.management.uiextension;

import org.exoplatform.web.application.ApplicationMessage;

public class NoI18NApplicationMessage extends ApplicationMessage {
  private static final long serialVersionUID = -336565264837075757L;

  /**
   * {@inheritDoc}
   */
  public NoI18NApplicationMessage(String key, Object[] args) {
    super(key, args);
  }

  /**
   * {@inheritDoc}
   */
  public NoI18NApplicationMessage(String key, Object[] args, int type) {
    super(key, args, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMessage() {
    return super.getMessageKey();
  }
}
