package org.exoplatform.management.answer.operations;

import java.io.ByteArrayInputStream;

public class InputStreamWrapper extends ByteArrayInputStream {
  public InputStreamWrapper() {
    super(new byte[0]);
  }

  public InputStreamWrapper(byte[] buf) {
    super(buf);
  }

}
