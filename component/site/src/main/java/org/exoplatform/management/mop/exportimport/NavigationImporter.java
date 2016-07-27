package org.exoplatform.management.mop.exportimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.NavigationFragment;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.importer.NavigationFragmentImporter;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

public class NavigationImporter {

  /** . */
  private final Locale portalLocale;

  /** . */
  private final PageNavigation src;

  /** . */
  private final NavigationService service;

  /** . */
  private final ImportMode mode;

  /** . */
  private final DescriptionService descriptionService;

  public NavigationImporter(Locale portalLocale, ImportMode mode, PageNavigation src, NavigationService service, DescriptionService descriptionService) {
    this.portalLocale = portalLocale;
    this.mode = mode;
    this.src = src;
    this.service = service;
    this.descriptionService = descriptionService;
  }

  public void perform() {

    //
    SiteKey key = new SiteKey(src.getOwnerType(), src.getOwnerId());

    //
    NavigationContext dst = service.loadNavigation(key);

    //
    switch (mode) {
    case CONSERVE:
      if (dst == null) {
        dst = new NavigationContext(key, new NavigationState(src.getPriority()));
        service.saveNavigation(dst);
      } else {
        dst = null;
      }
      break;
    case INSERT:
      if (dst == null) {
        dst = new NavigationContext(key, new NavigationState(src.getPriority()));
        service.saveNavigation(dst);
      }
      break;
    case MERGE:
      dst = new NavigationContext(key, new NavigationState(src.getPriority()));
      service.saveNavigation(dst);
      break;
    case OVERWRITE:
      if (dst != null) {
        service.destroyNavigation(dst);
      }

      dst = new NavigationContext(key, new NavigationState(src.getPriority()));
      service.saveNavigation(dst);
      break;
    default:
      throw new AssertionError();
    }

    //
    if (dst != null) {
      ArrayList<NavigationFragment> fragments = src.getFragments();
      if (fragments != null && fragments.size() > 0) {
        for (NavigationFragment fragment : fragments) {
          String parentURI = fragment.getParentURI();

          // Find something better than that for building the path
          List<String> path;
          if (parentURI != null) {
            path = new ArrayList<String>();
            String[] names = Utils.split("/", parentURI);
            for (String name : names) {
              if (name.length() > 0) {
                path.add(name);
              }
            }
          } else {
            path = Collections.emptyList();
          }

          //
          NavigationFragmentImporter fragmentImporter = new NavigationFragmentImporter(path.toArray(new String[path.size()]), service, dst.getKey(), portalLocale, descriptionService, fragment, mode.config);

          //
          fragmentImporter.perform();
          ExoContainer container = ExoContainerContext.getCurrentContainer();
          CacheService cacheService = (CacheService) container.getComponentInstanceOfType(CacheService.class);
          ExoCache cache = cacheService.getCacheInstance(NavigationService.class.getSimpleName());
          cache.clearCache();
        }
      }
    }
  }
}
