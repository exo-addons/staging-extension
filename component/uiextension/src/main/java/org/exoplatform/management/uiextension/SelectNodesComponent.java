package org.exoplatform.management.uiextension;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.management.uiextension.comparison.NodeComparison;
import org.exoplatform.management.uiextension.comparison.NodeComparisonState;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
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
@ComponentConfigs({
    @ComponentConfig(type = UIGrid.class, id = "uiSelectedNodesGrid", template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectedNodesGrid.gtmpl"),
    @ComponentConfig(type = UIGrid.class, id = "selectNodesGrid", template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectNodesGrid.gtmpl"),
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/webui/component/explorer/popup/staging/SelectContents.gtmpl", events = {
        @EventConfig(listeners = SelectNodesComponent.FilterActionListener.class), @EventConfig(listeners = SelectNodesComponent.SelectActionListener.class),
        @EventConfig(listeners = SelectNodesComponent.SelectAllActionListener.class), @EventConfig(listeners = SelectNodesComponent.DeleteAllActionListener.class),
        @EventConfig(listeners = SelectNodesComponent.DeleteActionListener.class) }) })
public class SelectNodesComponent extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(SelectNodesComponent.class.getName());

  public static final String CONTENT_STATE_FIELD_NAME = "contentState";
  public static final String FILTER_CONTENT_FIELD_NAME = "filter";
  public static final String PUBLICATION_DATE_FIELD_NAME = "modifiedAfter";
  public static final String PUBLISHED_CONTENT_ONLY_FIELD_NAME = "publishedContentOnly";

  public static String[] COMPARISON_BEAN_FIELD = { "title", "path", "published", "sourceModificationDate", "targetModificationDate", "stateLocalized" };

  public static String[] COMPARISON_BEAN_ACTION = { "Select" };

  public static String[] SELECTED_COMPARISON_BEAN_FIELD = { "title", "path", "actionLocalized" };

  public static String[] SELECTED_COMPARISON_BEAN_ACTION = { "Delete" };

  private UIGrid nodesGrid;
  private UIGrid selectedNodesGrid;
  protected UIFormStringInput filterField;
  protected UIFormSelectBox stateSelectBoxInput;
  protected UICheckBoxInput publishedCheckBoxInput;
  UIFormDateTimeInput fromDateModified = null;
  private PushContentPopupComponent pushContentPopupComponent;

  String stateString = null;
  Calendar modifiedDateFilter = null;
  String filterString = null;
  boolean publishedContentOnly = false;

  private List<NodeComparison> comparisons;
  private List<NodeComparison> filteredComparison;

  public SelectNodesComponent() throws Exception {
    ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();

    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(resourceBundle.getString("PushContent.state.all"), ""));
    options.add(new SelectItemOption<String>(NodeComparisonState.MODIFIED_ON_SOURCE.getLabel(resourceBundle), NodeComparisonState.MODIFIED_ON_SOURCE.getKey()));
    options.add(new SelectItemOption<String>(NodeComparisonState.MODIFIED_ON_TARGET.getLabel(resourceBundle), NodeComparisonState.MODIFIED_ON_TARGET.getKey()));

    options.add(new SelectItemOption<String>(NodeComparisonState.SAME.getLabel(resourceBundle), NodeComparisonState.SAME.getKey()));
    options.add(new SelectItemOption<String>(NodeComparisonState.UNKNOWN.getLabel(resourceBundle), NodeComparisonState.UNKNOWN.getKey()));

    stateSelectBoxInput = new UIFormSelectBox(CONTENT_STATE_FIELD_NAME, CONTENT_STATE_FIELD_NAME, options);
    addUIFormInput(stateSelectBoxInput);
    stateSelectBoxInput.setHTMLAttribute("style", "width:120px;");

    publishedCheckBoxInput = new UICheckBoxInput(PUBLISHED_CONTENT_ONLY_FIELD_NAME, PUBLISHED_CONTENT_ONLY_FIELD_NAME, false);
    publishedCheckBoxInput.setLabel("publishedContentOnly");
    publishedCheckBoxInput.setChecked(true);
    addUIFormInput(publishedCheckBoxInput);

    filterField = new UIFormStringInput(FILTER_CONTENT_FIELD_NAME, FILTER_CONTENT_FIELD_NAME, "");
    addUIFormInput(filterField);
    filterField.setHTMLAttribute("style", "width:90px;");

    fromDateModified = new UIFormDateTimeInput(PUBLICATION_DATE_FIELD_NAME, PUBLICATION_DATE_FIELD_NAME, null, false);
    addUIFormInput(fromDateModified);
    fromDateModified.setHTMLAttribute("style", "width:135px;");

    nodesGrid = addChild(UIGrid.class, "selectNodesGrid", "selectNodesGrid");
    nodesGrid.configure("path", COMPARISON_BEAN_FIELD, COMPARISON_BEAN_ACTION);
    nodesGrid.getUIPageIterator().setId("UISelectNodesGridIterator");

    selectedNodesGrid = addChild(UIGrid.class, "uiSelectedNodesGrid", "uiSelectedNodesGrid");
    selectedNodesGrid.configure("path", SELECTED_COMPARISON_BEAN_FIELD, SELECTED_COMPARISON_BEAN_ACTION);
    selectedNodesGrid.getUIPageIterator().setId("UISelectedNodesGridIterator");
  }

  public void init() throws Exception {
    String fieldJavascriptAction = event("Filter", null).replace("javascript:", "");
    filterField.setHTMLAttribute("onblur", fieldJavascriptAction);
    filterField.setHTMLAttribute("onkeypress", "if(event.keyCode == 13){" + fieldJavascriptAction + ";event.preventDefault();}");
    fromDateModified.setHTMLAttribute("onblur", "var textField = this; setTimeout(function(){ " + fieldJavascriptAction + "}, 200);");
    fromDateModified.setHTMLAttribute("oninput", "var textField = this; setTimeout(function(){ " + fieldJavascriptAction + "}, 200);");
    fromDateModified.setHTMLAttribute("onkeypress", "if(event.keyCode == 13)event.preventDefault()");
    publishedCheckBoxInput.setOnChange("Filter");
    stateSelectBoxInput.setOnChange("Filter");

    this.stateString = pushContentPopupComponent.stateString;
    stateSelectBoxInput.setValue(this.stateString);

    this.modifiedDateFilter = pushContentPopupComponent.modifiedDateFilter;
    fromDateModified.setCalendar(this.modifiedDateFilter);

    this.filterString = pushContentPopupComponent.filterString;
    filterField.setValue(this.filterString);

    this.publishedContentOnly = pushContentPopupComponent.publishedContentOnly;
    publishedCheckBoxInput.setChecked(this.publishedContentOnly);
  }

  public void setComparisons(List<NodeComparison> comparisons) {
    this.comparisons = comparisons;
    filteredComparison = new ArrayList<NodeComparison>(comparisons);
    computeComparisons();
  }

  public void computeComparisons() {
    List<NodeComparison> alreadySelectedNodes = pushContentPopupComponent.getSelectedNodes();
    filteredComparison.clear();

    pushContentPopupComponent.filterString = filterString = filterField.getValue();
    pushContentPopupComponent.modifiedDateFilter = modifiedDateFilter = fromDateModified.getCalendar();
    pushContentPopupComponent.stateString = stateString = stateSelectBoxInput.getValue();
    pushContentPopupComponent.publishedContentOnly = publishedContentOnly = publishedCheckBoxInput.getValue();

    for (NodeComparison nodeComparison : comparisons) {
      if (!alreadySelectedNodes.contains(nodeComparison) && checkFilter(nodeComparison)) {
        filteredComparison.add(nodeComparison);
      }
    }
    int currentPage = nodesGrid.getUIPageIterator().getCurrentPage();
    nodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparison>(new ListAccessImpl<NodeComparison>(NodeComparison.class, filteredComparison), 5));
    updateGridPage(nodesGrid, currentPage);

    currentPage = selectedNodesGrid.getUIPageIterator().getCurrentPage();
    if (pushContentPopupComponent.getSelectedNodes().isEmpty()) {
      selectedNodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparison>(new ListAccessImpl<NodeComparison>(NodeComparison.class, pushContentPopupComponent.getDefaultSelection()), 5));
    } else {
      selectedNodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparison>(new ListAccessImpl<NodeComparison>(NodeComparison.class, pushContentPopupComponent.getSelectedNodes()), 5));
    }
    updateGridPage(selectedNodesGrid, currentPage);
  }

  public static class FilterActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();

      selectNodesComponent.computeComparisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getNodesGrid());
    }
  }

  public static class SelectActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesComponent.getPushContentPopupComponent();

      String path = event.getRequestContext().getRequestParameter(OBJECTID);

      Iterator<NodeComparison> comparisons = selectNodesComponent.getComparisons().iterator();
      NodeComparison selectedNodeComparison = null;
      while (selectedNodeComparison == null && comparisons.hasNext()) {
        NodeComparison tmpNodeComparison = comparisons.next();
        if (path.equals(tmpNodeComparison.getPath())) {
          selectedNodeComparison = tmpNodeComparison;
          break;
        }
      }
      pushContentPopupComponent.addSelection(selectedNodeComparison);
      selectNodesComponent.computeComparisons();

      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent);
    }
  }

  public static class DeleteAllActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();
      selectNodesComponent.getSelectedNodesGrid().getUIPageIterator().setPageList(new LazyPageList<NodeComparison>(new ListAccessImpl<NodeComparison>(NodeComparison.class, selectNodesComponent.getPushContentPopupComponent().getDefaultSelection()), 5));

      selectNodesComponent.getPushContentPopupComponent().getSelectedNodes().clear();
      selectNodesComponent.computeComparisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getSelectedNodesGrid());
    }
  }

  private void updateGridPage(UIGrid grid, int currentPage) {
    try {
      if (currentPage < grid.getUIPageIterator().getAvailablePage()) {
        grid.getUIPageIterator().setCurrentPage(currentPage);
      } else {
        grid.getUIPageIterator().setCurrentPage(grid.getUIPageIterator().getAvailablePage());
      }
    } catch (Exception e) {
      LOG.warn(e.getMessage());
    }
  }

  static public class DeleteActionListener extends EventListener<UIForm> {
    public void execute(Event<UIForm> event) throws Exception {
      UIForm uiForm = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = null;
      if (uiForm instanceof PushContentPopupComponent) {
        pushContentPopupComponent = (PushContentPopupComponent) uiForm;
      } else if (uiForm instanceof SelectNodesComponent) {
        pushContentPopupComponent = ((SelectNodesComponent) uiForm).getPushContentPopupComponent();
      }
      String path = event.getRequestContext().getRequestParameter(OBJECTID);

      try {
        Iterator<NodeComparison> comparisons = pushContentPopupComponent.getSelectedNodes().iterator();
        boolean removed = false;
        while (!removed && comparisons.hasNext()) {
          NodeComparison comparison = comparisons.next();
          if (path.equals(comparison.getPath())) {
            comparisons.remove();
            removed = true;
          }
        }

        if (pushContentPopupComponent.getSelectNodesComponent().isRendered()) {
          pushContentPopupComponent.getSelectNodesComponent().computeComparisons();
          event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getSelectNodesComponent());
        }
      } catch (Exception ex) {
        ApplicationMessage message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        message.setResourceBundle(PushContentPopupComponent.getResourceBundle());
        pushContentPopupComponent.getUIFormInputInfo(PushContentPopupComponent.INFO_FIELD_NAME).setValue(message.getMessage());
        LOG.error("Error while deleting '" + path + "' from selected contents:", ex);
      }
    }
  }

  public static class SelectAllActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesComponent.getPushContentPopupComponent();
      List<NodeComparison> comparisons = selectNodesComponent.getFilteredComparison();
      for (NodeComparison nodeComparison : comparisons) {
        if (nodeComparison.getState().equals("unknown")) {
          continue;
        }
        pushContentPopupComponent.addSelection(nodeComparison);
      }
      selectNodesComponent.computeComparisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getSelectedNodesGrid());
    }
  }

  @Override
  public void activate() {}

  @Override
  public void deActivate() {}

  public UIGrid getSelectedNodesGrid() {
    return selectedNodesGrid;
  }

  public UIGrid getNodesGrid() {
    return nodesGrid;
  }

  public List<NodeComparison> getComparisons() {
    return comparisons;
  }

  public List<NodeComparison> getFilteredComparison() {
    return filteredComparison;
  }

  public void setPushContentPopupComponent(PushContentPopupComponent pushContentPopupComponent) {
    this.pushContentPopupComponent = pushContentPopupComponent;
  }

  public PushContentPopupComponent getPushContentPopupComponent() {
    return pushContentPopupComponent;
  }

  private boolean checkFilter(NodeComparison nodeComparison) {
    boolean isMatch = filterString == null || filterString.isEmpty() || nodeComparison.getTitle().toLowerCase().contains(filterString.toLowerCase());

    if (isMatch) {
      isMatch = modifiedDateFilter == null || nodeComparison.getSourceModificationDateCalendar() == null || modifiedDateFilter.before(nodeComparison.getSourceModificationDateCalendar());
    }

    NodeComparisonState state = nodeComparison.getState();
    if (state != null && state.equals(NodeComparisonState.NOT_FOUND_ON_TARGET)) {
      state = NodeComparisonState.MODIFIED_ON_SOURCE;
    } else if (state != null && state.equals(NodeComparisonState.NOT_FOUND_ON_SOURCE)) {
      state = NodeComparisonState.MODIFIED_ON_TARGET;
    }

    if (isMatch) {
      isMatch = stateString == null || stateString.isEmpty() || state.getKey().equals(stateString);
    }

    if (isMatch) {
      isMatch = !publishedContentOnly || nodeComparison.isPublished();
    }
    return isMatch;
  }

  public boolean isDefaultEntry(String path) {
    for (NodeComparison comparison : pushContentPopupComponent.getDefaultSelection()) {
      if (path.equals(comparison.getPath())) {
        return true;
      }
    }
    return false;
  }

}