define( "stagingControllers", [ "SHARED/jquery", "SHARED/juzu-ajax" ], function ( $ )
{

  var stagingCtrl = function($scope, $http, stagingService) {
    var stagingContainer = $('#staging');

    $scope.mode = "export";

    $scope.changeMode = function(mode) {
      $scope.mode = mode;
      $scope.readyToImport = false;
      if(mode == 'synchronize') {
        $scope.syncServersMessage = "";
        $scope.loadServers();
      }
    };

    /**********************************************************************/
    /*                              MODES                                 */
    /**********************************************************************/

    $scope.selectedServer = "";
    $scope.newServer = "";

    $scope.syncServersMessage = "";
    $scope.syncServersMessageClass = "alert-info";

    // function which set the result message with the given style
    $scope.setSyncServerMessage = function(text, type) {
      $scope.syncServersMessageClass = "alert-" + type;
      $scope.syncServersMessage = text;
    }

    /** Load synchronization servers **/
    $scope.loadServers = function() {
      $http.get(stagingContainer.jzURL('StagingExtensionController.getSynchonizationServers')).success(function (data) {
        $scope.servers = data.synchronizationServers;
      });
    };

    $scope.displayServerForm = function() {
      $scope.hostClass = "";
      $scope.portClass = "";
      $scope.usernameClass = "";
      $scope.passwordClass = "";

      $scope.isNewServerFormDisplayed = true;
      $scope.selectedServer = "";
    };

    $scope.saveServer = function(server) {
      if(!$scope.validateSyncServerForm($scope.newServer, true)) {
        return;
      }

      $scope.setSyncServerMessage("Saving new server ...", "info");
      $http.post(stagingContainer.jzURL('StagingExtensionController.addSynchonizationServer') + '&name='+server.name+'&host='+server.host+'&port='+server.port+'&username='+server.username+'&password='+server.password+'&ssl='+(server.ssl? 'true' : 'false')).success(function (data) {
          $scope.setSyncServerMessage("Server saved !", "success");
          $scope.loadServers();
        }).error(function (data) {
          $scope.setSyncServerMessage("Error while saving the server", "error");
        });
    };

    $scope.deleteServer = function(id) {
      $scope.syncServersMessageClass = "alert-info";
      $scope.syncServersMessage = "Deleting server ...";
      $http.post(stagingContainer.jzURL('StagingExtensionController.removeSynchonizationServer') + '&id='+id).success(function (data) {
        $scope.syncServersMessageClass = "alert-success";
        $scope.syncServersMessage = "Server deleted !";
        $scope.loadServers();
      }).error(function (data) {
        $scope.syncServersMessageClass = "alert-error";
        $scope.syncServersMessage = "Error while deleting the server";
      });
    };

    $scope.validateSyncServerForm = function(server, validateServerName) {
      $scope.hostClass = server.host ? "" : "error";
      $scope.portClass = server.port ? "" : "error";
      $scope.usernameClass = server.username ? "" : "error";
      $scope.passwordClass = server.password ? "" : "error";
      if(validateServerName) {
        $scope.serverNameClass = server.name ? "" : "error";
      }

      return server.host && server.port && server.username && server.password && (!validateServerName || server.name);
    };

    /**********************************************************************/
    /*                            CATEGORIES                              */
    /**********************************************************************/

    // categories tree (categories + subcategories)
    $scope.categories = [];

    //
    $scope.categoriesModel = [];

    // options
    $scope.optionsModel = [];
    // options default values
    $scope.optionsModel['/organization/user_EXPORT_filter/with-membership'] = true;
    $scope.optionsModel['/organization/group_EXPORT_filter/with-membership'] = true;
    $scope.optionsModel['/content/sites_EXPORT_filter/taxonomy'] = true;
    $scope.optionsModel['/content/sites_IMPORT_uuidBehavior'] = "NEW";

    //
    $scope.resources = [];
    //
    $scope.loadingResources = [];

    $scope.loadingCategoriesTree = true;
    $http.get(stagingContainer.jzURL('StagingExtensionController.getCategories')).success(function (data) {
      $scope.categories = data.categories;
      $scope.loadingCategoriesTree = false;
    });

    $scope.onToggleCategorySelection = function(selectedCategory) {
      if($scope.categoriesModel[selectedCategory]) {
        $scope.loadingResources[selectedCategory] = true;
        $http.get(stagingContainer.jzURL('StagingExtensionController.getResourcesOfCategory') + '&path=' + selectedCategory).success(function (data) {
          $scope.resources[selectedCategory] = [];
          for(var i=0; i<data.resources.length; i++) {
            $scope.resources[selectedCategory].push({
              "path": data.resources[i].path,
              "text": data.resources[i].text,
              "selected": false
            });
          }
          $scope.loadingResources[selectedCategory] = false;
        });
      } else {
        delete $scope.resources[selectedCategory];
      }
    };

    $scope.unselectCategory = function(selectedCategory) {
      $scope.categoriesModel[selectedCategory] = false;
      $scope.onToggleCategorySelection(selectedCategory);
    };


    /**********************************************************************/
    /*                 ACTIONS (export/import/synchronize)                */
    /**********************************************************************/

    $scope.resultMessage = "";
    $scope.resultMessageClass = "alert-info";

    $scope.readyToImport = false;

    // function which set the result message with the given style
    $scope.setResultMessage = function(text, type) {
      $scope.resultMessageClass = "alert-" + type;
      $scope.resultMessage = text;
    }

    // function which lists the selected categories
    // TODO see if it can be improved with a better binding
    $scope.getSelectedCategories = function() {
      var selectedCategories = [];
      for(var category in $scope.categoriesModel) {
        if($scope.categoriesModel[category]) {
          selectedCategories.push(category);
        }
      }
      return selectedCategories;
    };

    // import action
    $scope.prepareImportResources = function() {
      if(!$scope.importFile) {
        $scope.setResultMessage("No file selected", "error");
        return;
      }

      var form = new FormData();
      form.append('file', $scope.importFile);

      $scope.setResultMessage("Analyzing import file ...", "info");
      $http({
          url: stagingContainer.jzURL("StagingExtensionController.prepareImportResources"),
          data : form,
          method : 'POST',
          headers : {'Content-Type':undefined},
          transformRequest: function(data) { return data; }
        }).success(function (dataList) {
          $scope.setResultMessage("", "info");

          $scope.readyToImport = true;

          // reset all selected categories
          $scope.categoriesModel = [];
          
    	  var datas = dataList.split(',');
    	  for (index in datas) {
    		  var data = datas[index];
          	  if(data == '') {
          		  continue;
          	  }
	          // select category
	          $scope.categoriesModel[data] = true;
	          
	          // expand/unexpand first level resources categories
	          for(var i=0; i<$scope.categories.length; i++) {
	            // exception : /gadget and /registry are not really under /application
	            if($scope.categories[i].path == "/application") {
	              $scope.categories[i].expanded = (data == "/gadget" || data == "/registry");
	            } else {
	              $scope.categories[i].expanded = (data.indexOf($scope.categories[i].path) == 0);
	            }
	          }
	          $scope.onToggleCategorySelection(data);
          }
        }).error(function (data) {
          $scope.setResultMessage("Prepare Import failed. " + data, "error");
          $scope.readyToImport = false;
        });
    }

    // import action
    $scope.importResources = function() {
      if(!$scope.importFile) {
        $scope.setResultMessage("No file selected", "error");
        return;
      }

      var selectedCategories = $scope.getSelectedCategories();
      var nbOfSelectedCategories = selectedCategories.length;

      if(nbOfSelectedCategories == 0) {
        $scope.setResultMessage("No resource category selected", "error");
        return;
      }

      var form = new FormData();
      form.append('file', $scope.importFile);

      var queryParams = "";
      for(index in selectedCategories) {
    	  selectedCategory = selectedCategories[index];

	      if(queryParams != "") {
		        queryParams = queryParams + "&";
		  }
	      // options
	      queryParams += stagingService.getOptionsAsQueryString($scope.optionsModel, selectedCategory, "IMPORT", true);
	      if(queryParams != "") {
	        queryParams = "&" + queryParams;
	      }
	      // resource category
	      queryParams += "&staging:resourceCategory=" + selectedCategory;
      }

      console.log(queryParams);

      $scope.setResultMessage("Importing ...", "info");
      $http({
          url: stagingContainer.jzURL("StagingExtensionController.importResources") + queryParams,
          data : form,
          method : 'POST',
          headers : {'Content-Type':undefined},
          transformRequest: function(data) { return data; }
        }).success(function (data) {
          $scope.setResultMessage(data, "success");
        }).error(function (data) {
          $scope.setResultMessage("Import failed. " + data, "error");
        });
    };

    $scope.setFile = function(element) {
        $scope.$apply(function($scope) {
            $scope.importFile = element.files[0];
        });
    };

    // export action
    $scope.exportResources = function() {
      var selectedResources = [];
      for(categoryResources in $scope.resources) {
        var selectedCategoryResources = $scope.resources[categoryResources].filter(function(element) { return element.selected; });
        for(var i=0; i<selectedCategoryResources.length; i++) {
          selectedResources.push(selectedCategoryResources[i]);
        }
      }

      if(selectedResources.length == 0) {
        $scope.setResultMessage("No resources selected", "error");
        return;
      }

      // Request parameters

      // resource categories
      var selectedCategories = $scope.getSelectedCategories();
      var paramsResourceCategories = "";
      for(var i=0; i<selectedCategories.length; i++) {
        paramsResourceCategories += "&resourceCategories=" + selectedCategories[i];
      }

      // resources
      var paramsResources = "";
      for(var i=0; i<selectedResources.length; i++) {
        paramsResources += "&resources=" + selectedResources[i].path;
      }

      // options
      var paramsOptions = "";
      for(optionName in $scope.optionsModel) {
        paramsOptions += "&options=" + optionName + ":" + $scope.optionsModel[optionName];
      }

      // Launch synchronization...
      $scope.setResultMessage("Proceeding...", "info");
	  $.fileDownload(stagingContainer.jzURL('StagingExtensionController.export')+paramsResourceCategories + paramsResources + paramsOptions , {
		 successCallback: function (url) {
		   $scope.setResultMessage("Successfully exported.", "success");
		 },
		 failCallback: function (html, url) {
		   $scope.setResultMessage(html, "error");
		 }
	  });
	  // FIXME this have to be deleted, once 'successCallback' and 'failCallback' will work
	  $scope.setResultMessage("", "");

    };

    // synchronize action
    $scope.synchronizeResources = function() {
      var targetServer;
      if($scope.selectedServer) {
        targetServer = $scope.selectedServer;
      } else if($scope.servers.length == 0 || $scope.isNewServerFormDisplayed) {
        if(!$scope.validateSyncServerForm($scope.newServer, false)) {
          return;
        }
        targetServer = $scope.newServer;
      } else {
        $scope.setResultMessage("No destination server selected...", "error");
        return;
      }

      var selectedResources = [];
      for(categoryResources in $scope.resources) {
        var selectedCategoryResources = $scope.resources[categoryResources].filter(function(element) { return element.selected; });
        for(var i=0; i<selectedCategoryResources.length; i++) {
          selectedResources.push(selectedCategoryResources[i]);
        }
      }

      if(selectedResources.length == 0) {
        $scope.setResultMessage("No resources selected", "error");
        return;
      }

      // Request parameters

      // target server
      var paramsTargetServer = "host=" + targetServer.host;
      paramsTargetServer += "&port=" + targetServer.port;
      paramsTargetServer += "&username=" + targetServer.username;
      paramsTargetServer += "&password=" + targetServer.password;
      paramsTargetServer += "&isSSLString=" + targetServer.isSSLString;

      // resource categories
      var selectedCategories = $scope.getSelectedCategories();
      var paramsResourceCategories = "";
      for(var i=0; i<selectedCategories.length; i++) {
        paramsResourceCategories += "&resourceCategories=" + selectedCategories[i];
      }

      // resources
      var paramsResources = "";
      for(var i=0; i<selectedResources.length; i++) {
        paramsResources += "&resources=" + selectedResources[i].path;
      }

      // options
      var paramsOptions = "";
      for(optionName in $scope.optionsModel) {
        paramsOptions += "&options=" + optionName + ":" + $scope.optionsModel[optionName];
      }

      // Launch synchronization...
      $scope.setResultMessage("Proceeding...", "info");

      $http({
          method: 'POST',
          url: stagingContainer.jzURL('StagingExtensionController.synchronize'),
          data: paramsTargetServer + paramsResourceCategories + paramsResources + paramsOptions,
          headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).success(function (data) {
          $scope.setResultMessage(data, "success");
        }).error(function (data) {
          $scope.setResultMessage(data, "error");
        });
    };


    $scope.validateQuery = function() {
      var sql = $scope.optionsModel["/content/sites_EXPORT_filter/query"];
      if(sql == null || sql == "") {
        $scope.validateQueryResultMessageClass = "alert-error";
        $scope.validateQueryResultMessage = "Error : Empty query";
      } else {

        var selectedSites = $scope.resources["/content/sites"].filter(function(element) { return element.selected && element.path.indexOf("/content/sites") == 0; });
        var paramsSites = "";
        for(var i=0; i<selectedSites.length; i++) {
          paramsSites += "&sites=" + selectedSites[i].path;
        }

        $http({
          method: 'POST',
          url: stagingContainer.jzURL('StagingExtensionController.executeSQL'),
          data: 'sql=' + sql + paramsSites
          ,
          headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).success(function (data) {
            $scope.validateQueryResultMessageClass = "alert-info";
            $scope.validateQueryResultMessage = data;
          }).error(function (data) {
            $scope.validateQueryResultMessageClass = "alert-error";
            $scope.validateQueryResultMessage = data;
          });
      }
    };

  };

  return stagingCtrl;
});