package org.exoplatform.extension.synchronization.service.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
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
  protected Set<String> selectedResources = null;
  protected Map<String, String> selectedOptions = null;

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
  public Map<String, String> filterOptions(Map<String, String> resources) {
    Map<String, String> filteredOptions = new HashMap<String, String>();
    for (String optionPath : resources.keySet()) {
      if (optionPath.contains(getParentPath())) {
        filteredOptions.put(optionPath, resources.get(optionPath));
      }
    }
    selectedOptions = filteredOptions;
    return filteredOptions;
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
      if (conn.getResponseCode() != 200) {
        getLogger().error("Synchronization operation error, HTTP error code from target server : " + conn.getResponseCode());
        return false;
      }
    } catch (Exception e) {
      getLogger().error("Error while synchronizing the content", e);
      return false;
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
   * @param filters
   *          passed to GateIN Management SPI
   * @return archive file exported from GateIN Management Controller call
   */
  protected File getExportedFileFromOperation(String path, String... filters) {
    ManagedRequest request = null;
    if (filters != null && filters.length > 0) {
      Map<String, List<String>> attributes = new HashMap<String, List<String>>();
      attributes.put("filter", Arrays.asList(filters));
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
        // Delete file, not used if an error occurs
        if (tmpFile != null) {
          tmpFile.delete();
        }
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
      if (tempFile != null && tempFile.exists()) {
        tempFile.delete();
      }
    }
    tempFiles.clear();
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
    List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    Set<Map.Entry<String, String>> optionsEntrySet = options.entrySet();
    for (Map.Entry<String, String> entry : optionsEntrySet) {
      if (entry.getValue().equals("true")) {
        parameters.add(new BasicNameValuePair("filter", entry.getKey()));
      } else {
        parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }
    }
    return URLEncodedUtils.format(parameters, null);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  protected Log getLogger() {
    return log;
  }
}