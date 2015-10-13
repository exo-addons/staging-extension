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
import java.util.List;

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
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipReaderImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.hibernate.internal.SessionFactoryImpl;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class IDMRestore {
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

  public IDMRestore(File storageDir, Connection jdbcConn) throws Exception {
    this.jdbcConn = jdbcConn;
    this.storageDir = storageDir;
    this.dialect = DialectDetecter.detect(jdbcConn.getMetaData());
    this.dbCleanerInAutoCommit = dialect.startsWith(DialectConstants.DB_DIALECT_SYBASE);
  }

  public static void restore(PortalContainer portalContainer, File backupDirFile) throws Exception {
    // using existing DataSource to get a JDBC Connection.
    SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) getHibernateService(portalContainer).getSessionFactory();
    Connection jdbcConn = sessionFactoryImpl.getConnectionProvider().getConnection();

    IDMRestore restorer = new IDMRestore(backupDirFile, jdbcConn);
    try {
      restorer.clean();
      restorer.restore();
      restorer.applyConstraints();
      restorer.commit();
    } catch (Exception e) {
      restorer.rollback();
      throw e;
    }
  }

  public void clean() throws Exception {
    LOG.info("Start to clean IDM tables");

    String sql = null;

    Statement stmt = jdbcConn.createStatement();
    if (dialect.startsWith(DBConstants.DB_DIALECT_HSQLDB)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.hsqldb.drop.sql");
      sql += IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.hsqldb.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_MSSQL)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mssql.drop.sql");
      sql += IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mssql.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_MYSQL)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mysql.drop.sql");
      sql += IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.mysql.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_ORACLE)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.oracle.drop.sql");
      sql += IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.oracle.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.postgresql.drop.sql");
      sql += IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.postgresql.create.sql");
    } else if (dialect.startsWith(DBConstants.DB_DIALECT_SYBASE)) {
      sql = IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.sybase.drop.sql");
      sql += IOUtil.getResourceAsString("idm-db-ddl/picketlink.idm.sybase.create.sql");
    }
    if (!StringUtils.isEmpty(sql)) {
      stmt.execute(sql);
    } else {
      throw new IllegalStateException("SQL to clean database was not found");
    }
  }

  public void applyConstraints() throws Exception {
    LOG.info("Add IDM tables constraints");

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
      stmt.execute(sql);
    } else {
      throw new IllegalStateException("SQL to clean database was not found");
    }
  }

  /**
   * {@inheritDoc}
   */
  public void restore() throws Exception {
    for (String tableName : IDMBackup.TABLE_NAMES) {
      restoreTable(storageDir, jdbcConn, tableName);
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
      jdbcConn.rollback();

      jdbcConn.commit();
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
   */
  private void restoreTable(File storageDir, Connection jdbcConn, String tableName) throws IOException, SQLException {
    ObjectZipReaderImpl contentReader = null;
    ObjectZipReaderImpl contentLenReader = null;

    PreparedStatement insertNode = null;
    ResultSet tableMetaData = null;

    // switch table name to lower case
    if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL)) {
      tableName = tableName.toLowerCase();
    }

    try {
      File contentFile = new File(storageDir, IDMBackup.CONTENT_ZIP_FILE);
      contentReader = new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(contentFile));

      while (!contentReader.getNextEntry().getName().equals(tableName))
        ;

      File contentLenFile = new File(storageDir, IDMBackup.CONTENT_LEN_ZIP_FILE);
      contentLenReader = new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(contentLenFile));

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
              insertNode.setLong(targetIndex + 1, Integer.parseInt(value));
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

  private static HibernateService getHibernateService(PortalContainer portalContainer) {
    return (HibernateService) portalContainer.getComponentInstanceOfType(HibernateService.class);
  }
}
