package org.exoplatform.management.backup.service;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.database.utils.DialectConstants;
import org.exoplatform.services.database.utils.DialectDetecter;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ZipObjectReader;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class IDMRestore {
  public static final List<String> ID_COLUMNS_NAME = Arrays.asList(new String[] { "ID", "ATTRIBUTE_ID", "BIN_VALUE_ID" });

  protected static final Log LOG = ExoLogger.getLogger(IDMRestore.class);

  /**
   * The maximum possible batch size.
   */
  private final int MAXIMUM_BATCH_SIZE = 1000;

  /**
   * List of temporary files.
   */
  private final List<File> spoolFileList = new ArrayList<File>();

  /**
   * Temporary directory.
   */
  private final File tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

  /**
   * Maximum buffer size.
   */
  private final int maxBufferSize = 1024 * 200;

  /**
   * Connection to database.
   */
  protected final Connection jdbcConn;

  /**
   * Directory for dumps.
   */
  private final File storageDir;

  /**
   * Database dialect.
   */
  protected final String dialect;

  /**
   * Contains object names which executed queries.
   */
  protected List<String> successfulExecuted;

  protected boolean dbCleanerInAutoCommit;

  protected File contentFile = null;

  protected File contentLenFile = null;

  public IDMRestore(File storageDir, Connection jdbcConn) throws Exception {
    this.jdbcConn = jdbcConn;
    this.storageDir = storageDir;
    this.dialect = DialectDetecter.detect(jdbcConn.getMetaData());
    this.dbCleanerInAutoCommit = dialect.startsWith(DialectConstants.DB_DIALECT_SYBASE);

    contentFile = new File(storageDir, IDMBackup.CONTENT_ZIP_FILE);
    contentLenFile = new File(storageDir, IDMBackup.CONTENT_LEN_ZIP_FILE);
    if (!contentFile.exists() || !contentLenFile.exists()) {
      throw new IllegalStateException("IDM Backup wasn't found in backup folder " + storageDir);
    }
  }

  public static void verifyDSConnections(PortalContainer portalContainer) throws Exception {
    try {
      int numberOfOpenConnections = 0;
      int i = 0;
      do {
        // using existing DataSource to get a JDBC Connection.
        SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) getHibernateService(portalContainer).getSessionFactory();
        DatasourceConnectionProviderImpl connectionProvider = (DatasourceConnectionProviderImpl) sessionFactoryImpl.getConnectionProvider();
        DataSource dataSource = connectionProvider.getDataSource();

        numberOfOpenConnections = (int) connectionProvider.getDataSource().getClass().getMethod("getNumActive", new Class[0]).invoke(dataSource);
        if (numberOfOpenConnections > 0) {
          try {
            LOG.warn("Some IDM datasource connections aren't closed yet, wait 5 seconds before retrying.");
            // Wait for 10 seconds until all HTTP request finishes. The Web
            // Filter will avoid to have new requests and the scheduled jobs are
            // already suspended
            Thread.sleep(5000);
          } catch (Exception e1) {
            // Nothing to do
          }
        }
      } while (numberOfOpenConnections > 0 && i++ < 3);

      if (numberOfOpenConnections > 0) {
        throw new IllegalStateException("There are some unclosed connections on IDM datasource, please verify you don't have a session leak.");
      }
    } catch (NoSuchMethodException e) {
      // In case this is JBoss, this is normal
    }
  }

  public static void restore(PortalContainer portalContainer, File backupDirFile) throws Exception {
    if (new File(backupDirFile, IDMBackup.CONTENT_ZIP_FILE).exists()) {
      // using existing DataSource to get a JDBC Connection.
      SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) getHibernateService(portalContainer).getSessionFactory();
      DatasourceConnectionProviderImpl connectionProvider = (DatasourceConnectionProviderImpl) sessionFactoryImpl.getConnectionProvider();
      Connection jdbcConn = connectionProvider.getConnection();

      // Disable autocommit to be able to rollback all script if there is an
      // error
      jdbcConn.setAutoCommit(false);

      IDMRestore restorer = new IDMRestore(backupDirFile, jdbcConn);
      try {
        restorer.clean();
        restorer.restore();
        restorer.applyConstraints();
        restorer.commit();
        LOG.info("IDM restored");
      } catch (Exception e) {
        restorer.rollback();
        throw e;
      } finally {
        restorer.close();
      }
    } else {
      LOG.info("IDM Backup files was not found. IDM restore ignored.");
    }
  }

  public void clean() throws Exception {
    LOG.info("Clean IDM tables");

    String sqlDrop = null;
    String sqlCreate = null;

    Statement stmt = jdbcConn.createStatement();
    if (dialect.startsWith(DBConstants.DB_DIALECT_HSQLDB)) {
      sqlDrop = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.hsqldb.drop.sql");
      sqlCreate = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.hsqldb.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_MSSQL)) {
      sqlDrop = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mssql.drop.sql");
      sqlCreate = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mssql.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_MYSQL)) {
      sqlDrop = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mysql.drop.sql");
      sqlCreate = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mysql.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_ORACLE)) {
      sqlDrop = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.oracle.drop.sql");
      sqlCreate = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.oracle.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL)) {
      sqlDrop = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.postgresql.drop.sql");
      sqlCreate = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.postgresql.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_SYBASE)) {
      sqlDrop = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.sybase.drop.sql");
      sqlCreate = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.sybase.create.sql");
    } else {
      throw new IllegalStateException("Unknown dialect " + dialect);
    }
    executeSQLFile(sqlDrop, stmt, false);
    executeSQLFile(sqlCreate, stmt, true);
  }

  public void applyConstraints() throws Exception {
    LOG.info("Apply constraints on IDM tables");

    String sql = null;

    Statement stmt = jdbcConn.createStatement();
    if (dialect.startsWith(DBConstants.DB_DIALECT_HSQLDB)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.hsqldb.constraints.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_MSSQL)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mssql.constraints.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_MYSQL)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mysql.constraints.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_ORACLE)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.oracle.constraints.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.postgresql.constraints.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_SYBASE)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.sybase.constraints.sql");
    }
    if (!StringUtils.isEmpty(sql)) {
      executeSQLFile(sql, stmt, true);
    } else {
      throw new IllegalStateException("SQL to clean database was not found");
    }
  }

  /**
   * {@inheritDoc}
   */
  public void restore() throws Exception {
    LOG.info("Restore IDM tables");
    int maxId = 0;
    for (String tableName : IDMBackup.TABLE_NAMES) {
      maxId = restoreTable(storageDir, jdbcConn, tableName, maxId);
    }

    // Update sequence
    if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL) || dialect.startsWith(DBConstants.DB_DIALECT_ORACLE)) {
      Statement statement = jdbcConn.createStatement();
      statement.executeUpdate("create sequence hibernate_sequence START WITH " + (++maxId));
    }
  }

  /**
   * {@inheritDoc}
   */
  public void commit() throws Exception {
    jdbcConn.commit();
  }

  /**
   * {@inheritDoc}
   */
  public void rollback() throws RuntimeException {
    try {
      LOG.warn("Rollback IDM changes.");
      jdbcConn.rollback();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void close() throws RuntimeException {
    try {
      // in case for shared connection
      if (!jdbcConn.isClosed()) {
        jdbcConn.close();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Restore table.
   * 
   * @return
   */
  private int restoreTable(File storageDir, Connection jdbcConn, String tableName, int maxId) throws IOException, SQLException {
    ZipObjectReader contentReader = null;
    ZipObjectReader contentLenReader = null;

    PreparedStatement insertNode = null;
    ResultSet tableMetaData = null;

    // switch table name to lower case
    if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL)) {
      tableName = tableName.toLowerCase();
    }

    try {
      contentReader = new ZipObjectReader(PrivilegedFileHelper.zipInputStream(contentFile));

      while (!contentReader.getNextEntry().getName().equals(tableName))
        ;

      contentLenReader = new ZipObjectReader(PrivilegedFileHelper.zipInputStream(contentLenFile));

      while (!contentLenReader.getNextEntry().getName().equals(tableName))
        ;

      // get information about source table
      int sourceColumnCount = contentReader.readInt();

      List<Integer> columnType = new ArrayList<Integer>();
      List<String> columnName = new ArrayList<String>();

      for (int i = 0; i < sourceColumnCount; i++) {
        columnType.add(contentReader.readInt());
        columnName.add(contentReader.readString());
      }

      int targetColumnCount = sourceColumnCount;

      // construct statement
      StringBuilder names = new StringBuilder();
      StringBuilder parameters = new StringBuilder();
      for (int i = 0; i < targetColumnCount; i++) {
        names.append(columnName.get(i)).append(i == targetColumnCount - 1 ? "" : ",");
        parameters.append("?").append(i == targetColumnCount - 1 ? "" : ",");
      }

      int batchSize = 0;
      insertNode = jdbcConn.prepareStatement("INSERT INTO " + tableName + " (" + names + ") VALUES(" + parameters + ")");

      // set data
      outer: while (true) {
        for (int i = 0, targetIndex = 0; i < columnType.size(); i++, targetIndex++) {
          InputStream stream;
          long len;

          try {
            len = contentLenReader.readLong();
          } catch (EOFException e) {
            if (i == 0) {
              // content length file is empty check content file
              try {
                contentReader.readByte();
              } catch (EOFException e1) {
                break outer;
              }
            }

            throw new IOException("Content length file is empty but content still present", e);
          }
          stream = len == -1 ? null : spoolInputStream(contentReader, len);

          // set
          if (stream != null) {
            if (columnType.get(i) == Types.INTEGER || columnType.get(i) == Types.BIGINT || columnType.get(i) == Types.SMALLINT || columnType.get(i) == Types.TINYINT) {
              ByteArrayInputStream ba = (ByteArrayInputStream) stream;
              byte[] readBuffer = new byte[ba.available()];
              ba.read(readBuffer);

              String value = new String(readBuffer, Constants.DEFAULT_ENCODING);
              int intValue = Integer.parseInt(value);
              insertNode.setLong(targetIndex + 1, intValue);
              if ((dialect.startsWith(DBConstants.DB_DIALECT_ORACLE) || dialect.startsWith(DBConstants.DB_DIALECT_PGSQL)) && ID_COLUMNS_NAME.contains(columnName.get(i).toUpperCase())) {
                maxId = Math.max(intValue, maxId);
              }
            } else if (columnType.get(i) == Types.BIT) {
              ByteArrayInputStream ba = (ByteArrayInputStream) stream;
              byte[] readBuffer = new byte[ba.available()];
              ba.read(readBuffer);

              String value = new String(readBuffer);
              if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL)) {
                insertNode.setBoolean(targetIndex + 1, value.equalsIgnoreCase("t"));
              } else {
                insertNode.setBoolean(targetIndex + 1, value.equals("1"));
              }
            } else if (columnType.get(i) == Types.BOOLEAN) {
              ByteArrayInputStream ba = (ByteArrayInputStream) stream;
              byte[] readBuffer = new byte[ba.available()];
              ba.read(readBuffer);

              String value = new String(readBuffer);
              insertNode.setBoolean(targetIndex + 1, value.equalsIgnoreCase("true"));
            } else if (columnType.get(i) == Types.VARBINARY || columnType.get(i) == Types.LONGVARBINARY || columnType.get(i) == Types.BLOB || columnType.get(i) == Types.BINARY
                || columnType.get(i) == Types.OTHER) {
              insertNode.setBinaryStream(targetIndex + 1, stream, (int) len);
            } else {
              byte[] readBuffer = new byte[(int) len];
              stream.read(readBuffer);

              insertNode.setString(targetIndex + 1, new String(readBuffer, Constants.DEFAULT_ENCODING));
            }
          } else {
            insertNode.setNull(targetIndex + 1, columnType.get(i));
          }
        }

        // add statement to batch
        insertNode.addBatch();

        if (++batchSize == MAXIMUM_BATCH_SIZE) {
          insertNode.executeBatch();

          commitBatch();

          batchSize = 0;
        }
      }

      if (batchSize != 0) {
        insertNode.executeBatch();

        commitBatch();
      }

      return maxId;

    } finally {
      if (contentReader != null) {
        contentReader.close();
      }

      if (contentLenReader != null) {
        contentLenReader.close();
      }

      if (insertNode != null) {
        insertNode.close();
      }

      // delete all temporary files
      for (File file : spoolFileList) {
        if (!PrivilegedFileHelper.delete(file)) {
          file.deleteOnExit();
        }
      }

      if (tableMetaData != null) {
        tableMetaData.close();
      }
    }
  }

  /**
   * Committing changes from batch.
   */
  protected void commitBatch() throws SQLException {
    // commit every batch for sybase
    if (dialect.startsWith(DBConstants.DB_DIALECT_SYBASE)) {
      jdbcConn.commit();
    }
  }

  /**
   * Spool input stream.
   */
  private InputStream spoolInputStream(ObjectReader in, long contentLen) throws IOException {
    byte[] buffer = new byte[0];
    byte[] tmpBuff;
    long readLen = 0;
    File sf = null;
    OutputStream sfout = null;

    try {
      while (true) {
        int needToRead = contentLen - readLen > 2048 ? 2048 : (int) (contentLen - readLen);
        tmpBuff = new byte[needToRead];

        if (needToRead == 0) {
          break;
        }

        in.readFully(tmpBuff);

        if (sfout != null) {
          sfout.write(tmpBuff);
        } else if (readLen + needToRead > maxBufferSize) {
          sf = PrivilegedFileHelper.createTempFile("idmvd", null, tempDir);
          sfout = PrivilegedFileHelper.fileOutputStream(sf);

          sfout.write(buffer);
          sfout.write(tmpBuff);
          buffer = null;
        } else {
          // reallocate new buffer and spool old buffer contents
          byte[] newBuffer = new byte[(int) (readLen + needToRead)];
          System.arraycopy(buffer, 0, newBuffer, 0, (int) readLen);
          System.arraycopy(tmpBuff, 0, newBuffer, (int) readLen, needToRead);
          buffer = newBuffer;
        }

        readLen += needToRead;
      }

      if (buffer != null) {
        return new ByteArrayInputStream(buffer);
      } else {
        return PrivilegedFileHelper.fileInputStream(sf);
      }
    } finally {
      if (sfout != null) {
        sfout.close();
      }

      if (sf != null) {
        spoolFileList.add(sf);
      }
    }
  }

  private void executeSQLFile(String sql, Statement stmt, boolean throwIfError) throws Exception {
    String[] sqlDropStatements = sql.split(";");
    for (String sqlDropStatement : sqlDropStatements) {
      sqlDropStatement = sqlDropStatement.trim();
      if (sqlDropStatement.isEmpty()) {
        continue;
      }
      try {
        stmt.executeUpdate(sqlDropStatement);
      } catch (Exception e) {
        LOG.warn("Can't execute SQL '{}'. This can be minor in case the object doesn't exist. Original cause: {} ", sqlDropStatement, e.getMessage());
        if (throwIfError) {
          throw e;
        } else {
          rollback();
        }
      }
    }
  }

  private static HibernateService getHibernateService(PortalContainer portalContainer) {
    return (HibernateService) portalContainer.getComponentInstanceOfType(HibernateService.class);
  }
}
