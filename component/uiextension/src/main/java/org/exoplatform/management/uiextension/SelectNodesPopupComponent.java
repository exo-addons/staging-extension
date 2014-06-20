package org.exoplatform.management.uiextension;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.management.uiextension.coparaison.NodeComparaison;
import org.exoplatform.management.uiextension.coparaison.NodeComparaisonState;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfigs(
  { @ComponentConfig(
    type = UIGrid.class,
    id = "uiSelectedNodesGrid",
    template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectedNodesGrid.gtmpl"), @ComponentConfig(
    type = UIGrid.class,
    id = "selectNodesGrid",
    template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectNodesGrid.gtmpl"), @ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/webui/component/explorer/popup/staging/SelectContents.gtmpl",
    events =
      { @EventConfig(
        listeners = SelectNodesPopupComponent.FilterActionListener.class), @EventConfig(
        listeners = SelectNodesPopupComponent.SelectActionListener.class), @EventConfig(
        listeners = SelectNodesPopupComponent.SelectAllActionListener.class), @EventConfig(
        listeners = SelectNodesPopupComponent.DeleteAllActionListener.class), @EventConfig(
        listeners = PushContentPopupComponent.DeleteActionListener.class) }) })
public class SelectNodesPopupComponent extends UIForm implements UIPopupComponent {
  private static final String CONTENT_STATE_FIELD_NAME = "contentState";
  private static final String FILTER_CONTENT_FIELD_NAME = "filter";
  private static final String PUBLICATION_DATE_FIELD_NAME = "modifiedAfter";
  private static final String PUBLISHED_CONTENT_ONLY_FIELD_NAME = "publishedContentOnly";

  public static String[] COMPARAISON_BEAN_FIELD =
    { "title", "path", "lastModifierUserName", "sourceModificationDate", "targetModificationDate", "stateLocalized" };

  public static String[] COMPARAISON_BEAN_ACTION =
    { "Select" };

  public static String[] SELECTED_COMPARAISON_BEAN_FIELD =
    { "title", "path", "stateLocalized" };

  public static String[] SELECTED_COMPARAISON_BEAN_ACTION =
    { "Delete" };

  private UIGrid nodesGrid;
  private UIGrid selectedNodesGrid;
  protected UIFormStringInput filterField;
  protected UIFormSelectBox stateCheckBoxInput;
  protected UICheckBoxInput publishedCheckBoxInput;
  private PushContentPopupComponent pushContentPopupComponent;

  String stateString = null;
  Calendar modifiedDateFilter = null;
  String filterString = null;
  boolean publishedContentOnly = false;

  private List<NodeComparaison> comparaisons;
  private List<NodeComparaison> filteredComparaison;

  public SelectNodesPopupComponent() throws Exception {
    ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();

    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(resourceBundle.getString("PushContent.state.all"), ""));
    options.add(new SelectItemOption<String>(NodeComparaisonState.MODIFIED_ON_SOURCE.getLabel(resourceBundle), NodeComparaisonState.MODIFIED_ON_SOURCE.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.MODIFIED_ON_TARGET.getLabel(resourceBundle), NodeComparaisonState.MODIFIED_ON_TARGET.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.NOT_FOUND_ON_SOURCE.getLabel(resourceBundle), NodeComparaisonState.NOT_FOUND_ON_SOURCE.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.NOT_FOUND_ON_TARGET.getLabel(resourceBundle), NodeComparaisonState.NOT_FOUND_ON_TARGET.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.SAME.getLabel(resourceBundle), NodeComparaisonState.SAME.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.UNKNOWN.getLabel(resourceBundle), NodeComparaisonState.UNKNOWN.getKey()));

    stateCheckBoxInput = new UIFormSelectBox(CONTENT_STATE_FIELD_NAME, CONTENT_STATE_FIELD_NAME, options);
    addUIFormInput(stateCheckBoxInput);

    publishedCheckBoxInput = new UICheckBoxInput(PUBLISHED_CONTENT_ONLY_FIELD_NAME, PUBLISHED_CONTENT_ONLY_FIELD_NAME, false);
    publishedCheckBoxInput.setLabel("publishedContentOnly");
    addUIFormInput(publishedCheckBoxInput);

    filterField = new UIFormStringInput(FILTER_CONTENT_FIELD_NAME, FILTER_CONTENT_FIELD_NAME, "");
    addUIFormInput(filterField);

    addUIFormInput(new UIFormDateTimeInput(PUBLICATION_DATE_FIELD_NAME, PUBLICATION_DATE_FIELD_NAME, null, true));

    nodesGrid = addChild(UIGrid.class, "selectNodesGrid", "selectNodesGrid");
    nodesGrid.configure("path", COMPARAISON_BEAN_FIELD, COMPARAISON_BEAN_ACTION);

    selectedNodesGrid = addChild(UIGrid.class, "uiSelectedNodesGrid", "uiSelectedNodesGrid");
    selectedNodesGrid.configure("path", SELECTED_COMPARAISON_BEAN_FIELD, SELECTED_COMPARAISON_BEAN_ACTION);
  }

  public void setComparaisons(List<NodeComparaison> comparaisons) {
    this.comparaisons = comparaisons;
    filteredComparaison = new ArrayList<NodeComparaison>(comparaisons);
    computeComparaisons();
  }

  public void computeComparaisons() {
    List<NodeComparaison> alreadySelectedNodes = pushContentPopupComponent.getSelectedNodes();
    filteredComparaison.clear();

    for (NodeComparaison nodeComparaison : comparaisons) {
      if (!alreadySelectedNodes.contains(nodeComparaison) && checkFilter(nodeComparaison)) {
        filteredComparaison.add(nodeComparaison);
      }
    }
    nodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, filteredComparaison), 5));
    refreshSelectedNodesGrid();
  }

  public static class SelectActionListener extends EventListener<SelectNodesPopupComponent> {
    @Override
    public void execute(Event<SelectNodesPopupComponent> event) throws Exception {
      SelectNodesPopupComponent selectNodesPopupComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesPopupComponent.getPushContentPopupComponent();

      String path = event.getRequestContext().getRequestParameter(OBJECTID);

      Iterator<NodeComparaison> comparaisons = selectNodesPopupComponent.getComparaisons().iterator();
      NodeComparaison selectedNodeComparaison = null;
      while (selectedNodeComparaison == null && comparaisons.hasNext()) {
        NodeComparaison tmpNodeComparaison = comparaisons.next();
        if (path.equals(tmpNodeComparaison.getPath())) {
          selectedNodeComparaison = tmpNodeComparaison;
          break;
        }
      }
      pushContentPopupComponent.addSelection(selectedNodeComparaison);
      selectNodesPopupComponent.computeComparaisons();

      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getSelectedNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getSelectedNodesGrid());
    }
  }

  public static class FilterActionListener extends EventListener<SelectNodesPopupComponent> {
    @Override
    public void execute(Event<SelectNodesPopupComponent> event) throws Exception {
      SelectNodesPopupComponent selectNodesPopupComponent = event.getSource();

      selectNodesPopupComponent.filterString = selectNodesPopupComponent.getUIStringInput(FILTER_CONTENT_FIELD_NAME).getValue();
      selectNodesPopupComponent.modifiedDateFilter = selectNodesPopupComponent.getUIFormDateTimeInput(PUBLICATION_DATE_FIELD_NAME).getCalendar();
      selectNodesPopupComponent.stateString = selectNodesPopupComponent.getUIFormSelectBox(CONTENT_STATE_FIELD_NAME).getValue();
      selectNodesPopupComponent.publishedContentOnly = selectNodesPopupComponent.getUICheckBoxInput(PUBLISHED_CONTENT_ONLY_FIELD_NAME).getValue();

      selectNodesPopupComponent.computeComparaisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getNodesGrid());
    }
  }

  public static class DeleteAllActionListener extends EventListener<SelectNodesPopupComponent> {
    @Override
    public void execute(Event<SelectNodesPopupComponent> event) throws Exception {
      SelectNodesPopupComponent selectNodesPopupComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesPopupComponent.getPushContentPopupComponent();
      pushContentPopupComponent.getSelectedNodes().clear();
      pushContentPopupComponent.getSelectedNodesGrid().getUIPageIterator()
          .setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, pushContentPopupComponent.getDefaultSelection()), 5));
      selectNodesPopupComponent.computeComparaisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getSelectedNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getSelectedNodesGrid());
    }
  }

  public static class SelectAllActionListener extends EventListener<SelectNodesPopupComponent> {
    @Override
    public void execute(Event<SelectNodesPopupComponent> event) throws Exception {
      SelectNodesPopupComponent selectNodesPopupComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesPopupComponent.getPushContentPopupComponent();
      List<NodeComparaison> comparaisons = selectNodesPopupComponent.getFilteredComparaison();
      for (NodeComparaison nodeComparaison : comparaisons) {
        pushContentPopupComponent.addSelection(nodeComparaison);
      }
      selectNodesPopupComponent.computeComparaisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getSelectedNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getSelectedNodesGrid());
    }
  }

  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }

  public UIGrid getSelectedNodesGrid() {
    return selectedNodesGrid;
  }

  public UIGrid getNodesGrid() {
    return nodesGrid;
  }

  public List<NodeComparaison> getComparaisons() {
    return comparaisons;
  }

  public List<NodeComparaison> getFilteredComparaison() {
    return filteredComparaison;
  }

  public void setPushContentPopupComponent(PushContentPopupComponent pushContentPopupComponent) {
    this.pushContentPopupComponent = pushContentPopupComponent;
    refreshSelectedNodesGrid();
  }

  public void refreshSelectedNodesGrid() {
    this.selectedNodesGrid.getUIPageIterator().setPageList(pushContentPopupComponent.getSelectedNodesGrid().getUIPageIterator().getPageList());
  }

  public PushContentPopupComponent getPushContentPopupComponent() {
    return pushContentPopupComponent;
  }

  private boolean checkFilter(NodeComparaison nodeComparaison) {
    boolean isMatch = filterString == null || filterString.isEmpty() || nodeComparaison.getPath().toLowerCase().contains(filterString.toLowerCase())
        || nodeComparaison.getTitle().toLowerCase().contains(filterString.toLowerCase()) || nodeComparaison.getStateLocalized().contains(filterString);

    if (isMatch) {
      isMatch = modifiedDateFilter == null || nodeComparaison.getSourceModificationDateCalendar() == null || modifiedDateFilter.before(nodeComparaison.getSourceModificationDateCalendar());
    }

    if (isMatch) {
      isMatch = stateString == null || stateString.isEmpty() || nodeComparaison.getState().getKey().equals(stateString);
    }

    if (isMatch) {
      isMatch = !publishedContentOnly || nodeComparaison.isPublishedOnSource();
    }
    return isMatch;
  }

  public boolean isDefaultEntry(String path) {
    for (NodeComparaison comparaison : pushContentPopupComponent.getDefaultSelection()) {
      if (path.equals(comparaison.getPath())) {
        return true;
      }
    }
    return false;
  }

}