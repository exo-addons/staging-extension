package org.exoplatform.management.backup.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipWriterImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.hibernate.internal.SessionFactoryImpl;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class IDMBackup {
  protected static final Log LOG = ExoLogger.getLogger(IDMBackup.class);
  protected static final String CONTENT_ZIP_FILE = "idm-dump.zip";
  protected static final String CONTENT_LEN_ZIP_FILE = "idm-dump-len.zip";
  protected static final String[] TABLE_NAMES = { "jbid_attr_bin_value", "jbid_creden_bin_value", "jbid_io", "jbid_io_attr", "jbid_io_attr_text_values", "jbid_io_creden", "jbid_io_creden_props",
      "jbid_io_creden_type", "jbid_io_props", "jbid_io_rel", "jbid_io_rel_name", "jbid_io_rel_name_props", "jbid_io_rel_props", "jbid_io_rel_type", "jbid_io_type", "jbid_real_props", "jbid_realm" };

  public static void backup(PortalContainer portalContainer, final File storageDir) throws BackupException {
    try {
      // using existing DataSource to get a JDBC Connection.
      SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) getHibernateService(portalContainer).getSessionFactory();
      Connection jdbcConn = sessionFactoryImpl.getConnectionProvider().getConnection();

      Map<String, String> scripts = new HashMap<String, String>();
      for (String tableName : TABLE_NAMES) {
        scripts.put(tableName, "select * from " + tableName);
      }

      backup(storageDir, jdbcConn, scripts);
    } catch (Exception e) {
      throw new BackupException(e);
    }
  }

  public static void backup(File storageDir, Connection jdbcConn, Map<String, String> scripts) throws BackupException {
    Exception exc = null;

    ObjectZipWriterImpl contentWriter = null;
    ObjectZipWriterImpl contentLenWriter = null;

    try {
      contentWriter = new ObjectZipWriterImpl(PrivilegedFileHelper.zipOutputStream(new File(storageDir, CONTENT_ZIP_FILE)));
      contentLenWriter = new ObjectZipWriterImpl(PrivilegedFileHelper.zipOutputStream(new File(storageDir, CONTENT_LEN_ZIP_FILE)));

      for (Entry<String, String> entry : scripts.entrySet()) {
        dumpTable(jdbcConn, entry.getKey(), entry.getValue(), contentWriter, contentLenWriter);
      }
    } catch (IOException e) {
      exc = e;
      throw new BackupException(e);
    } catch (SQLException e) {
      exc = e;
      throw new BackupException("SQL Exception: " + JDBCUtils.getFullMessage(e), e);
    } finally {
      if (jdbcConn != null) {
        try {
          jdbcConn.close();
        } catch (SQLException e) {
          if (exc != null) {
            LOG.error("Can't close connection", e);
            throw new BackupException(exc);
          } else {
            throw new BackupException(e);
          }
        }
      }

      try {
        if (contentWriter != null) {
          contentWriter.close();
        }

        if (contentLenWriter != null) {
          contentLenWriter.close();
        }
      } catch (IOException e) {
        if (exc != null) {
          LOG.error("Can't close zip", e);
          throw new BackupException(exc);
        } else {
          throw new BackupException(e);
        }
      }
    }
  }

  /**
   * Dump table.
   * 
   * @throws IOException
   * @throws SQLException
   */
  private static void dumpTable(Connection jdbcConn, String tableName, String script, ObjectZipWriterImpl contentWriter, ObjectZipWriterImpl contentLenWriter) throws IOException, SQLException {
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
      security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
    }

    Statement stmt = null;
    ResultSet rs = null;
    try {
      contentWriter.putNextEntry(new ZipEntry(tableName));
      contentLenWriter.putNextEntry(new ZipEntry(tableName));

      stmt = jdbcConn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(1000);
      rs = stmt.executeQuery(script);
      ResultSetMetaData metaData = rs.getMetaData();

      int columnCount = metaData.getColumnCount();
      int[] columnType = new int[columnCount];

      contentWriter.writeInt(columnCount);
      for (int i = 0; i < columnCount; i++) {
        columnType[i] = metaData.getColumnType(i + 1);
        contentWriter.writeInt(columnType[i]);
        contentWriter.writeString(metaData.getColumnName(i + 1));
      }

      byte[] tmpBuff = new byte[2048];

      // Now we can output the actual data
      while (rs.next()) {
        for (int i = 0; i < columnCount; i++) {
          InputStream value;
          if (columnType[i] == Types.VARBINARY || columnType[i] == Types.LONGVARBINARY || columnType[i] == Types.BLOB || columnType[i] == Types.BINARY || columnType[i] == Types.OTHER) {
            value = rs.getBinaryStream(i + 1);
          } else {
            String str = rs.getString(i + 1);
            value = str == null ? null : new ByteArrayInputStream(str.getBytes(Constants.DEFAULT_ENCODING));
          }

          if (value == null) {
            contentLenWriter.writeLong(-1);
          } else {
            long len = 0;
            int read = 0;

            while ((read = value.read(tmpBuff)) >= 0) {
              contentWriter.write(tmpBuff, 0, read);
              len += read;
            }
            contentLenWriter.writeLong(len);
          }
        }
      }

      contentWriter.closeEntry();
      contentLenWriter.closeEntry();
    } finally {
      if (rs != null) {
        rs.close();
      }

      if (stmt != null) {
        stmt.close();
      }
    }
  }

  private static HibernateService getHibernateService(PortalContainer portalContainer) {
    return (HibernateService) portalContainer.getComponentInstanceOfType(HibernateService.class);
  }
}
