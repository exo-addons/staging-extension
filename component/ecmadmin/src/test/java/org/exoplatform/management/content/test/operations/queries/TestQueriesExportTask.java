package org.exoplatform.management.content.test.operations.queries;

import org.exoplatform.container.xml.Configuration;
import org.exoplatform.management.ecmadmin.operations.queries.QueriesExportTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class TestQueriesExportTask {

  @Before
  public void setup() {

  }

  @Test
  public void exportEntry() {
    String userId = "root";
    Configuration configuration = new Configuration();

    Assert.assertEquals(new QueriesExportTask(configuration, userId).getEntry(), "ecmadmin/queries/users/root-queries-configuration.xml");
    Assert.assertEquals(new QueriesExportTask(configuration, null).getEntry(), "ecmadmin/queries/shared-queries-configuration.xml");
  }
}