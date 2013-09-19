package org.exoplatform.management.service.integration;

import org.chromattic.spi.jcr.SessionLifeCycle;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.lang.reflect.Method;

/**
 * Integrates Chromattic with the GateIn JCR server.
 */
public class CurrentRepositoryLifeCycle implements SessionLifeCycle {
  /** . */
  private final String containerName = "portal";

  private Repository getCurrentRepository() throws RepositoryException
  {
    try
    {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();

      // Get top container
      Class<?> eXoContainerContextClass = cl.loadClass("org.exoplatform.container.ExoContainerContext");
      Method getTopContainerMethod = eXoContainerContextClass.getMethod("getTopContainer");
      Object topContainer = getTopContainerMethod.invoke(null);

      //
      if (topContainer == null)
      {
        throw new RepositoryException("Could not locate the top container");
      }

      //
      Method getPortalContainerMethod = topContainer.getClass().getMethod("getPortalContainer", String.class);
      Object container = getPortalContainerMethod.invoke(topContainer, containerName);

      //
      if (container == null)
      {
        throw new RepositoryException("Could not obtain the " + containerName + " portal container");
      }

      //
      Method getComponentInstanceOfTypeMethod = container.getClass().getMethod("getComponentInstanceOfType", Class.class);
      Class<?> repositoryServiceClass = Thread.currentThread().getContextClassLoader().loadClass("org.exoplatform.services.jcr.RepositoryService");
      Object repositoryService = getComponentInstanceOfTypeMethod.invoke(container, repositoryServiceClass);

      //
      if (repositoryService == null)
      {
        throw new RepositoryException("Could not obtain the repository service");
      }

      //
      Method getDefaultRepositoryMethod = repositoryService.getClass().getMethod("getCurrentRepository");
      return (Repository)getDefaultRepositoryMethod.invoke(repositoryService);
    }
    catch (Exception e)
    {
      throw new RepositoryException("Could not obtain repository", e);
    }
  }

  public Session login() throws RepositoryException
  {
    Repository repo = getCurrentRepository();
    return repo.login();
  }

  public Session login(String workspace) throws RepositoryException
  {
    Repository repo = getCurrentRepository();
    return repo.login(workspace);
  }

  public Session login(Credentials credentials, String workspace) throws RepositoryException
  {
    Repository repo = getCurrentRepository();
    return repo.login(credentials, workspace);
  }

  public Session login(Credentials credentials) throws RepositoryException
  {
    Repository repo = getCurrentRepository();
    return repo.login(credentials);
  }

  public void save(Session session) throws RepositoryException
  {
    session.save();
  }

  public void close(Session session)
  {
    session.logout();
  }
}
