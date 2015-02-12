package org.exoplatform.management.service.api;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;

public abstract class AbstractResourceHandler implements ResourceHandler {

  protected static final String MANAGED_COMPONENT_REST_URI = "/rest/private/managed-components";

  private Log log = ExoLogger.getLogger(this.getClass());

  protected Log getLogger() {
    return log;
  }

  /**
   * GateIN Management Controller
   */
  private ManagementController managementController = null;

  protected ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }

  /**
   * {@inheritDoc}
   */
  public void synchronize(List<Resource> resources, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception {
    for (Resource resource : resources) {
      synchronize(resource, exportOptions, importOptions, targetServer);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void export(List<Resource> resources, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception {
    for (Resource resource : resources) {
      export(resource, exportFileOS, exportOptions);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void synchronizeResourcesInFilter(List<Resource> resources, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception {
    for (Resource resource : resources) {
      if (getPath().equals(resource.getPath())) {
        synchronize(new Resource(getPath(), getPath(), getPath()), exportOptions, importOptions, targetServer);
      } else {
        String resourcePath = resource.getPath().replace(getPath() + "/", "");

        Map<String, String> exportOptionsTmp = new HashMap<String, String>(exportOptions);
        exportOptionsTmp.put("filter/" + resourcePath, null);

        synchronize(new Resource(getPath(), getPath(), getPath()), exportOptionsTmp, importOptions, targetServer);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void exportResourcesInFilter(List<Resource> resources, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception {
    Map<String, String> exportOptionsTmp = new HashMap<String, String>(exportOptions);
    for (Resource resource : resources) {
      if (getPath().equals(resource.getPath())) {
        export(new Resource(getPath(), getPath(), getPath()), exportFileOS, exportOptions);
      } else {
        String resourcePath = resource.getPath().replace(getPath() + "/", "");
        exportOptionsTmp.put("filter/" + resourcePath, null);
      }
    }
    if (!exportOptionsTmp.isEmpty()) {
      export(new Resource(getPath(), getPath(), getPath()), exportFileOS, exportOptionsTmp);
    }
  }

  /**
   * Sends data (exported zip) to the target server
   * 
   * @param response
   * @param options
   * @param targetServer
   * @return
   */
  protected boolean sendData(File file, Map<String, String> options, TargetServer targetServer) throws Exception {
    FileInputStream fileInputStream = null;
    try {
      getLogger().info("Sending data to server: " + targetServer.getHost());

      String targetServerURL = getServerURL(targetServer, getPath(), options);
      URL url = new URL(targetServerURL);

      fileInputStream = new FileInputStream(file);

      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("PUT");
      String passString = targetServer.getUsername() + ":" + targetServer.getPassword();
      String basicAuth = "Basic " + new String(Base64.encodeBase64(passString.getBytes()));
      conn.setRequestProperty("Authorization", basicAuth);
      conn.setFixedLengthStreamingMode(fileInputStream.available());
      conn.setRequestProperty("Content-Type", "application/zip");
      conn.setDoOutput(true);

      IOUtils.copy(fileInputStream, conn.getOutputStream());
      fileInputStream.close();

      getLogger().info("Content sent to target server: " + targetServer.getHost());
      getLogger().info("Importing content in target server, please wait ...");

      if (conn.getResponseCode() != 200) {
        throw new IllegalStateException("Synchronization operation error, HTTP error code from target server : " + conn.getResponseCode());
      }
      getLogger().info("Import in target server finished successfully.");
    } catch (Exception e) {
      throw new RuntimeException("Error while synchronizing the content", e);
    } finally {
      if (fileInputStream != null) {
        fileInputStream.close();
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
  public ManagedResponse getExportedResourceFromOperation(String path, Map<String, String> selectedOptions) {
    ManagedRequest request = null;
    if (!selectedOptions.isEmpty()) {
      Map<String, List<String>> attributes = extractAttributes(selectedOptions);
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), attributes, ContentType.ZIP);
    } else {
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), ContentType.ZIP);
    }

    try {
      // Call GateIN Management SPI
      return getManagementController().execute(request);
    } catch (Exception e) {
      throw new RuntimeException("Error while handling Response from GateIN Management, export operation", e);
    }
  }

  /**
   * Buil server URL
   * 
   * @param targetServer
   * @param uri
   * @param options
   * @return
   */
  public String getServerURL(TargetServer targetServer, String uri, Map<String, String> options) {
    String targetServerURL = "http";
    if (targetServer.isSsl()) {
      targetServerURL += "s";
    }
    targetServerURL += "://" + targetServer.getHost() + ":" + targetServer.getPort() + MANAGED_COMPONENT_REST_URI;
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

  /**
   * Delete temp files created by GateIN management operations
   * 
   */
  protected void clearTempFiles() {
    deleteTempFilesStartingWith("gatein-export(.*)\\.zip");
    deleteTempFilesStartingWith("data(.*)\\.xml");
  }

  private void export(Resource resource, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws IOException {
    FileOutputStream outputStream = null;
    FileInputStream inputStream = null;
    File tmpFile = null;
    try {
      ManagedResponse managedResponse = getExportedResourceFromOperation(resource.getPath(), exportOptions);
      tmpFile = File.createTempFile("staging", "-export.zip");
      tmpFile.deleteOnExit();

      outputStream = new FileOutputStream(tmpFile);
      managedResponse.writeResult(outputStream, false);

      outputStream.flush();
      outputStream.close();
      outputStream = null;

      getLogger().info("Export operation finished.");

      inputStream = new FileInputStream(tmpFile);

      Utils.copyZipEnries(new ZipInputStream(inputStream), exportFileOS, null);

      inputStream.close();
      inputStream = null;
    } catch (Exception ex) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting resource: " + resource.getPath(), ex);
    } finally {
      if (outputStream != null) {
        outputStream.close();
      }
      if (inputStream != null) {
        inputStream.close();
      }
      if (tmpFile != null) {
        tmpFile.delete();
      }
      clearTempFiles();
    }
  }

  private void synchronize(Resource resource, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws IOException {
    FileOutputStream fileOutputStream = null;
    File tmpFile = null;
    try {
      ManagedResponse managedResponse = getExportedResourceFromOperation(resource.getPath(), exportOptions);

      tmpFile = File.createTempFile(resource.getPath().replace("/", ""), ".zip");
      tmpFile.deleteOnExit();

      fileOutputStream = new FileOutputStream(tmpFile);
      managedResponse.writeResult(fileOutputStream, false);
      fileOutputStream.close();

      getLogger().info("Export operation finished.");

      sendData(tmpFile, importOptions, targetServer);

    } catch (Exception ex) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting resource: " + resource.getPath(), ex);
    } finally {
      if (tmpFile != null) {
        tmpFile.delete();
      }
    }
  }

  private String encodeURLParameters(Map<String, String> options) {
    Map<String, List<String>> attributes = extractAttributes(options);
    List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    Iterator<Entry<String, List<String>>> entryIterator = attributes.entrySet().iterator();
    while (entryIterator.hasNext()) {
      Entry<String, List<String>> entry = entryIterator.next();
      String parameterName = entry.getKey();
      List<String> parameterValues = entry.getValue();
      for (String parameterValue : parameterValues) {
        parameters.add(new BasicNameValuePair(parameterName, parameterValue));
      }
    }
    return URLEncodedUtils.format(parameters, null);
  }

  /**
   * Convert map of option to a map of attributes for the export operation.
   * Example : * filter/with-membership -> true * filter/replace-existing ->
   * true * importMode -> merge is converted to * filter ->
   * {with-membership:true, replace-existing} * importMode -> {merge}
   * 
   * @param selectedOptions
   * @return
   */
  private Map<String, List<String>> extractAttributes(Map<String, String> selectedOptions) {
    Map<String, List<String>> attributes = new HashMap<String, List<String>>();

    Set<Entry<String, String>> optionsEntrySet = selectedOptions.entrySet();
    for (Entry<String, String> option : optionsEntrySet) {
      String optionName;
      String optionValue;
      String[] optionParts = option.getKey().split("/", 2);
      if (optionParts.length == 1) {
        optionName = option.getKey();
        optionValue = option.getValue();
      } else if (optionParts.length == 2) {
        optionName = optionParts[0];
        optionValue = optionParts[1];
        if (option.getValue() != null) {
          optionValue += ":" + option.getValue();
        }
      } else {
        throw new RuntimeException("Option '" + option.getKey() + "' is not valid.");
      }

      List<String> attribute = attributes.get(optionName);
      if (attribute == null) {
        attribute = new ArrayList<String>();
        attributes.put(optionName, attribute);
      }
      attribute.add(optionValue);
    }

    return attributes;
  }

  protected void deleteTempFilesStartingWith(String regex) {
    String tempDirPath = System.getProperty("java.io.tmpdir");
    File file = new File(tempDirPath);

    if (log.isDebugEnabled()) {
      log.debug("Delete files with regex '" + regex + "*' under " + tempDirPath);
    }

    File[] listFiles = file.listFiles((FileFilter) new RegexFileFilter(regex));
    for (File tempFile : listFiles) {
      deleteFile(tempFile);
    }
  }

  protected void deleteFile(File tempFile) {
    if (tempFile != null && tempFile.exists() && !tempFile.isDirectory()) {
      try {
        FileUtils.forceDelete(tempFile);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Unable to delete temp file: " + tempFile.getAbsolutePath() + ". Not blocker.");
        }
        tempFile.deleteOnExit();
      }
    }
  }
}