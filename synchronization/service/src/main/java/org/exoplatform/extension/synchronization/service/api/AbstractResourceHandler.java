package org.exoplatform.extension.synchronization.service.api;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.util.IOUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.operation.OperationNames;

public abstract class AbstractResourceHandler implements ResourceHandler {

  protected static final String MANAGED_COMPONENT_REST_URI = "/rest/private/managed-components";
  protected static final String OPERATION_IMPORT_PREFIX = "IMPORT";
  protected static final String OPERATION_EXPORT_PREFIX = "EXPORT";
  protected Set<String> selectedResources = null;
  protected Map<String, String> selectedImportOptions = new HashMap<String, String>();
  protected Map<String, String> selectedExportOptions = new HashMap<String, String>();

  private List<File> tempFiles = new ArrayList<File>();

  /**
   * GateIN Management Controller
   */
  private ManagementController managementController = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> filterSubResources(Set<String> resources) {
    Set<String> filteredSelectedResources = new HashSet<String>();
    for (String resourcePath : resources) {
      if (resourcePath.contains(getParentPath())) {
        filteredSelectedResources.add(resourcePath);
      }
    }
    selectedResources = filteredSelectedResources;
    return filteredSelectedResources;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void filterOptions(Map<String, String> options, boolean allFilter) {
    selectedImportOptions.clear();
    selectedExportOptions.clear();
    for (String optionPath : options.keySet()) {
      if (optionPath.contains(getParentPath())) {
        String optionKey = optionPath.replace(getParentPath(), "");
        if (optionKey.startsWith("/")) {
          optionKey = optionKey.substring(1);
        }
        String[] optionKeys = optionKey.split("/", 2);
        if (optionKeys.length != 2) {
          throw new RuntimeException("Option '" + optionKey + "' is not valid.");
        }
        String optionValue;
        if (allFilter) {
          optionValue = "filter";
        } else {
          optionValue = options.get(optionPath);
        }
        if (optionKeys[0].equals(OPERATION_IMPORT_PREFIX)) {
          optionKey = optionKeys[1];
          selectedImportOptions.put(optionKey, optionValue);
        } else if (optionKeys[0].equals(OPERATION_EXPORT_PREFIX)) {
          optionKey = optionKeys[1];
          selectedExportOptions.put(optionKey, optionValue);
        } else {
          throw new RuntimeException("Option '" + optionKey + "' is not valid.");
        }
      }
    }
  }

  protected ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }

  protected boolean synhronizeData(File inputFile, boolean isSSL, String host, String port, String uri, String username, String password, Map<String, String> options) {
    FileInputStream inputFileStream = null;
    try {
      String targetServerURL = getServerURL(isSSL, host, port, uri, options);
      URL url = new URL(targetServerURL);

      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("PUT");
      String passString = username + ":" + password;
      String basicAuth = "Basic " + new String(Base64.encodeBase64(passString.getBytes()));
      conn.setRequestProperty("Authorization", basicAuth);
      conn.setUseCaches(false);
      conn.setRequestProperty("Connection", "Keep-Alive");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/zip");

      inputFileStream = new FileInputStream(inputFile);
      conn.setRequestProperty("Content-Length", String.valueOf(inputFileStream.available()));
      IOUtils.copy(inputFileStream, conn.getOutputStream());
      inputFileStream.close();
      inputFileStream = null;
      if (conn.getResponseCode() != 200) {
        throw new IllegalStateException("Synchronization operation error, HTTP error code from target server : " + conn.getResponseCode());
      }
    } catch (Exception e) {
      getLogger().error("Error while synchronizing the content", e);
      throw new RuntimeException(e);
    } finally {
      if (inputFileStream != null) {
        try {
          inputFileStream.close();
        } catch (IOException e) {
          // Nothing here
        }
      }
      clearTempFiles();
    }
    return true;
  }

  /**
   * Call GateIN Management Controller to export selected resource using options
   * passed in filters
   * 
   * @param path
   *          managed path
   * @param selectedOptions
   *          passed to GateIN Management SPI
   * @return archive file exported from GateIN Management Controller call
   */
  protected File getExportedFileFromOperation(String path, Map<String, String> selectedOptions) {
    ManagedRequest request = null;
    if (!selectedOptions.isEmpty()) {
      Map<String, List<String>> attributes = extractAttributes(selectedOptions);
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), attributes, ContentType.ZIP);
    } else {
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), ContentType.ZIP);
    }
    FileOutputStream outputStream = null;
    File tmpFile = null;
    try {
      // Call GateIN Management SPI
      ManagedResponse response = getManagementController().execute(request);

      // Create temp file
      tmpFile = File.createTempFile("exo", "-extension-generator");
      tmpFile.deleteOnExit();
      outputStream = new FileOutputStream(tmpFile);
      tempFiles.add(tmpFile);

      // Create temp file
      response.writeResult(outputStream);
      outputStream.flush();
      outputStream.close();

      return tmpFile;
    } catch (Exception e) {
      if (outputStream != null) {
        try {
          outputStream.flush();
          outputStream.close();
        } catch (IOException ioExp) {
          // nothing to do
        }
        // Delete temp file in case of error
        deleteFile(tmpFile);
      }
      throw new RuntimeException("Error while handling Response from GateIN Management, export operation", e);
    }
  }

  /**
   * Delete temp files created by GateIN management operations
   * 
   */
  protected void clearTempFiles() {
    for (File tempFile : tempFiles) {
      deleteFile(tempFile);
    }
    tempFiles.clear();
    deleteTempFilesStartingWith("gatein-export");
  }

  private String getServerURL(boolean isSSL, String host, String port, String uri, Map<String, String> options) {
    String targetServerURL = "http";
    if (isSSL) {
      targetServerURL += "s";
    }
    targetServerURL += "://" + host + ":" + port + MANAGED_COMPONENT_REST_URI;
    if (!uri.startsWith("/")) {
      targetServerURL += "/";
    }
    targetServerURL += uri;
    String optionsString = encodeURLParameters(options);
    if (!options.isEmpty()) {
      targetServerURL += "?" + optionsString;
    }
    return targetServerURL;
  }

  private String encodeURLParameters(Map<String, String> options) {
    Map<String, List<String>> attributes = extractAttributes(options);
    List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    Iterator<Entry<String, List<String>>> entryIterator = attributes.entrySet().iterator();
    while (entryIterator.hasNext()) {
      Map.Entry<String, List<String>> entry = entryIterator.next();
      String parameterName = entry.getKey();
      List<String> parameterValues = entry.getValue();
      for (String parameterValue : parameterValues) {
        parameters.add(new BasicNameValuePair(parameterName, parameterValue));
      }
    }
    return URLEncodedUtils.format(parameters, null);
  }

  private Map<String, List<String>> extractAttributes(Map<String, String> selectedOptions) {
    Map<String, List<String>> attributes = new HashMap<String, List<String>>();
    Set<Map.Entry<String, String>> optionsEntrySet = selectedOptions.entrySet();
    for (Map.Entry<String, String> entry : optionsEntrySet) {
      if (entry.getValue().equals("filter")) {
        List<String> filters = attributes.get("filter");
        if (filters == null) {
          filters = new ArrayList<String>();
          attributes.put("filter", filters);
        }
        filters.add(entry.getKey());
      } else {
        List<String> parameterValues = attributes.get(entry.getKey());
        if (parameterValues == null) {
          parameterValues = new ArrayList<String>();
          attributes.put(entry.getKey(), parameterValues);
        }
        parameterValues.add(entry.getValue());
      }
    }
    return attributes;
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  protected Log getLogger() {
    return log;
  }

  protected void deleteTempFilesStartingWith(String prefix) {
    String tempDirPath = System.getProperty("java.io.tmpdir");
    File file = new File(tempDirPath);
    log.info("Delete files '" + prefix + "*' under " + tempDirPath);
    File[] listFiles = file.listFiles((FileFilter) new PrefixFileFilter(prefix));
    for (File tempFile : listFiles) {
      deleteFile(tempFile);
    }
  }

  protected void deleteFile(File tempFile) {
    if (tempFile != null && tempFile.exists() && !tempFile.isDirectory()) {
      try {
        tempFile.delete();
      } catch (Exception exception) {
        // Cannot delete file, plan to delete it when JVM stops.
        tempFile.deleteOnExit();
      }
    }
  }

}