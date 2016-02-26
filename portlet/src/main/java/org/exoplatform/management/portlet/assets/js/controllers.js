define( "stagingControllers", [ "SHARED/jquery", "SHARED/juzu-ajax" ], function ( $ )
{
  var stagingCtrl = function($scope, $q, $timeout, $http, stagingService) {
  var stagingContainer = $('#staging');

    var deferred = $q.defer();
	$http.get(stagingContainer.jzURL('StagingExtensionController.getBundle'))
	    .success( function(data) {
	         $scope.i18n = data;
	         deferred.resolve(data);
	    })

    $scope.mode = "export";
    $scope.button_clicked = false;

    $scope.changeMode = function(mode) {
      if($scope.readyToImport) {
    	  $("#fileToImport").attr({ value: '' });
    	  $scope.fileToImportTitle = "";
          $scope.readyToImport = false;
          // reset all selected categories
          $scope.categoriesModel = [];
          // expand/unexpand first level resources categories
          for(var i=0; i<$scope.categories.length; i++) {
        	  $scope.categories[i].expanded = false;
          }
      }
      $scope.mode = mode;
      if(mode == 'import') {
    	  $("#fileToImport").attr({ value: '' });
      } else {
	      if(mode == 'synchronize') {
	        $scope.loadServers();
	      }
      }
      $scope.setResultMessage("", "info");
    };

    /**********************************************************************/
    /*                              MODES                                 */
    /**********************************************************************/

    $scope.selectedServer = "";
    $scope.newServer = "";

    /** Load synchronization servers **/
    $scope.loadServers = function() {
      $http.get(stagingContainer.jzURL('StagingExtensionController.getSynchonizationServers')).success(function (data) {
        $scope.servers = data.synchronizationServers;
      });
    };

    $scope.refreshController = function() {
    	try{
    	   $scope.$digest()
    	} catch(excep){
    		// No need to display errors in console
    	}
    };

    $scope.displayServerForm = function() {
      $scope.hostClass = "";
      $scope.portClass = "";
      $scope.usernameClass = "";
      $scope.passwordClass = "";

      $scope.isNewServerFormDisplayed = true;
      $scope.selectedServer = "";
    };

    $scope.testConnection = function(server) {
      if(!$scope.validateSyncServerForm($scope.newServer, false)) {
        return;
      }

      $scope.setResultMessage($scope.i18n.testingConnection, "info");
      $http.post(stagingContainer.jzURL('StagingExtensionController.testServerConnection') + '&name='+server.name+'&host='+server.host+'&port='+server.port+'&username='+server.username+'&password='+server.password+'&ssl='+(server.ssl? 'true' : 'false')).success(function (data) {
    	  $scope.setResultMessage(data, "success");
          $scope.loadServers();
          $timeout(function(){$scope.setResultMessage("", "info")}, 3000);
        }).error(function (data) {
          $scope.setResultMessage(data, "error");
        });
    };

    $scope.saveServer = function(server) {
      if(!$scope.validateSyncServerForm($scope.newServer, true)) {
        return;
      }

      $scope.setResultMessage($scope.i18n.savingServer, "info");
      $http.post(stagingContainer.jzURL('StagingExtensionController.addSynchonizationServer') + '&name='+server.name+'&host='+server.host+'&port='+server.port+'&username='+server.username+'&password='+server.password+'&ssl='+(server.ssl? 'true' : 'false')).success(function (data) {
    	  $scope.setResultMessage(data, "success");
          $scope.loadServers();
          server.name = '';
          server.host = '';
          server.port = '';
          server.username = '';
          server.password = '';
          server.ssl = false;

          $scope.isNewServerFormDisplayed = false;
          $timeout(function(){$scope.setResultMessage("", "info")}, 3000);
        }).error(function (data) {
          $scope.setResultMessage(data, "error");
        });
    };

    $scope.deleteServer = function(id) {
      $scope.setResultMessage($scope.i18n.deletingServer, "info");
      $http.post(stagingContainer.jzURL('StagingExtensionController.removeSynchonizationServer') + '&id='+id).success(function (data) {
        $scope.setResultMessage(data, "success");
        $scope.loadServers();
      }).error(function (data) {
        $scope.setResultMessage(data, "error");
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
    $scope.optionsModel['/content/sites_EXPORT_filter/taxonomy'] = true;

    //
    $scope.resources = [];
    //
    $scope.loadingResources = [];

    $scope.loadingCategoriesTree = true;
    $http.get(stagingContainer.jzURL('StagingExtensionController.getCategories')).success(function (data) {
      $scope.categories = data.categories;
      $scope.loadingCategoriesTree = false;
    }).error(function (data) {
        $scope.setResultMessage(data, "error");
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
    $scope.resultMessageClassExt = "uiIconInfo";

    $scope.readyToImport = false;
    $scope.fileToImportTitle = "";

    // function which set the result message with the given style
    $scope.setResultMessage = function(text, type) {
      $scope.resultMessageClass = "alert-" + type;
      $scope.resultMessageClassExt = "uiIcon" + type.charAt(0).toUpperCase() + type.slice(1);
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
    	$scope.fileToImportTitle = "";
        $scope.setResultMessage($scope.i18n.noSelectedFile, "error");
        return;
      }

      $(".staging .mode-import-options .uiAction").addClass("resources-loader");

      var form = new FormData();
      form.append('file', $scope.importFile);

      $scope.setResultMessage($scope.i18n.analyzingFile, "info");
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

          // expand/unexpand first level resources categories
          for(var i=0; i<$scope.categories.length; i++) {
        	  $scope.categories[i].expanded = false;
          }

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
            	  if(data == "/gadget" || data == "/registry") {
            	    $scope.categories[i].expanded = true;
            	  }
	            } else {
	              if(data.indexOf($scope.categories[i].path) == 0) {
	            	    $scope.categories[i].expanded = true;
	              }
		          // select category
		          $scope.categoriesModel[$scope.categories[i].path] = (data === $scope.categories[i].path);
	            }
	          }

          	  $scope.onToggleCategorySelection(data);
          }
          $(".staging .mode-import-options .uiAction").removeClass("resources-loader");
        }).error(function (data) {
          $scope.setResultMessage(data, "error");
          $scope.readyToImport = false;
          $(".staging .mode-import-options .uiAction").removeClass("resources-loader");
        });
    }

    // import action
    $scope.importResources = function() {
      if(!$scope.importFile) {
        $scope.setResultMessage($scope.i18n.noSelectedFile, "error");
        return;
      }

      var selectedCategories = $scope.getSelectedCategories();
      var nbOfSelectedCategories = selectedCategories.length;

      if(nbOfSelectedCategories == 0) {
        $scope.setResultMessage($scope.i18n.noSelectedResourceCategory, "error");
        return;
      }

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
	      queryParams += "&staging:resourceCategory=" + encodeURIComponent(selectedCategory);
      }

  	  $scope.button_clicked = true;
      $scope.refreshController();
      try {
        $scope.setResultMessage($scope.i18n.importing, "info");
        $http({
            url: stagingContainer.jzURL("StagingExtensionController.importResources"),
            data: queryParams,
            method : 'POST',
	        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
          }).success(function (data) {
            $scope.setResultMessage(data, "success");
            $scope.button_clicked = false;
            $scope.refreshController();
          }).error(function (data) {
            $scope.setResultMessage(data, "error");
            $scope.button_clicked = false;
            $scope.refreshController();
          });
      } catch(exception) {
          $scope.button_clicked = false;
          $scope.refreshController();
      }
    };

    $scope.setFile = function(element) {
        $scope.$apply(function($scope) {
            $scope.importFile = element.files[0];
            $scope.fileToImportTitle = $scope.importFile.name;
        });
    };

    $scope.fileUploadClick = function(element) {
        $scope.$apply(function($scope) {
            $(".staging input#fileToImport").click();
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
        $scope.setResultMessage($scope.i18n.selectResources, "error");
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
		
	  $scope.button_clicked = true;
	  $scope.refreshController();
	  try {
      // Launch synchronization
        $scope.setResultMessage($scope.i18n.processing, "info");

	    var downloadOptions = {};
        $.fileDownload(stagingContainer.jzURL('StagingExtensionController.export') + paramsResourceCategories + paramsResources + paramsOptions, downloadOptions)
          .done(function () {
            $scope.$apply(function(scope) {
              scope.setResultMessage("", "info");
              scope.button_clicked = false;
              scope.refreshController();
            });
          })
          .fail(function (data) {
            $scope.$apply(function(scope) {
              scope.setResultMessage(data, "error");
	          scope.button_clicked = false;
	          scope.refreshController();
            });
          });
        } catch(exception) {
      	  $scope.button_clicked = false;
	      $scope.refreshController();
        }
	};

	$scope.backup = function() {
		var dirFolder = $scope.optionsModel["/backup/directory"];
		var exportJCR = $scope.optionsModel["/backup/export-jcr"];
		var exportIDM = $scope.optionsModel["/backup/export-idm"];

		var writeStrategy = $scope.optionsModel["/backup/writeStrategy"];
		var displayMessageFor = $scope.optionsModel["/backup/displayMessageFor"];
		var message = $scope.optionsModel["/backup/message"];
		if(!message) {
			message = "";
		}

		if(dirFolder == null || dirFolder == "") {
		  $scope.setResultMessage($scope.i18n.emptyBackupFolder, "error");
		} else {
		  $scope.button_clicked = true;
		  $scope.refreshController();

		  // Launch backup
	      $scope.setResultMessage($scope.i18n.processing, "info");

		  $http({
		        method: 'POST',
		        url: stagingContainer.jzURL('StagingExtensionController.backup'),
		        data: 'backupDirectory=' + encodeURIComponent(dirFolder) + "&exportJCR=" + exportJCR + "&exportIDM=" + exportIDM + "&writeStrategy=" + writeStrategy + "&displayMessageFor=" + displayMessageFor + "&message=" + encodeURIComponent(message),
		        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
		  	}).success(function (data) {
		  		if(!data) {
		    	  $scope.setResultMessage($scope.i18n.errorOccured, "error");
		        }  else if(data.indexOf('<body') >= 0 || data.indexOf('<head') >= 0) {
		          $scope.setResultMessage($scope.i18n.sessionTimeout, "error");
		      	} else {
			      $scope.setResultMessage(data, "success");
		      	}
	      	    $scope.button_clicked = false;
		        $scope.refreshController();
		    }).error(function (data) {
			      $scope.setResultMessage(data, "error");
		      	  $scope.button_clicked = false;
			      $scope.refreshController();
		    });
		}
	};

    $scope.restore = function() {
	  var dirFolder = $scope.optionsModel["/restore/directory"];
      if(dirFolder == null || dirFolder == "") {
        $scope.setResultMessage($scope.i18n.emptyBackupFolder, "error");
      } else {
    	$scope.button_clicked = true;
        $scope.refreshController();

        // Launch restore
	    $scope.setResultMessage($scope.i18n.processing, "info");

        $http({
			method: 'POST',
			url: stagingContainer.jzURL('StagingExtensionController.restore'),
			data: 'backupDirectory=' + encodeURIComponent(dirFolder),
			headers: {'Content-Type': 'application/x-www-form-urlencoded'}
      	}).success(function (data) {
      		if(!data) {
    	      $scope.setResultMessage($scope.i18n.errorOccured, "error");
            } else if(data.indexOf('<body') >= 0 || data.indexOf('<head') >= 0) {
  	          $scope.setResultMessage($scope.i18n.sessionTimeout, "error");
          	} else {
    	      $scope.setResultMessage(data, "success");
          	}
      	    $scope.button_clicked = false;
	        $scope.refreshController();
        }).error(function (data) {
			$scope.setResultMessage(data, "error");
      	    $scope.button_clicked = false;
			$scope.refreshController();
        });
      }
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
        $scope.setResultMessage($scope.i18n.selectDestinationServer, "error");
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
        $scope.setResultMessage($scope.i18n.selectResources, "error");
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

	   $scope.button_clicked = true;
	   $scope.refreshController();
	   try {
	      // Launch synchronization
	      $scope.setResultMessage($scope.i18n.processing, "info");
	
	      $http({
	          method: 'POST',
	          url: stagingContainer.jzURL('StagingExtensionController.synchronize'),
	          data: paramsTargetServer + paramsResourceCategories + paramsResources + paramsOptions,
	          headers: {'Content-Type': 'application/x-www-form-urlencoded'}
	        }).success(function (data) {
				if(data.indexOf('<body') >= 0 || data.indexOf('<head') >= 0) {
		            $scope.setResultMessage($scope.i18n.sessionTimeout, "error");
		            $scope.button_clicked = false;
		            $scope.refreshController();
	        	} else {
		          $scope.setResultMessage(data, "success");
	    	      $scope.button_clicked = false;
	    	      $scope.refreshController();
	    	  }
	        }).error(function (data) {
	          $scope.setResultMessage(data, "error");
    	      $scope.button_clicked = false;
    	      $scope.refreshController();
	        });
      } catch(exception) {
  	    $scope.button_clicked = false;
	    $scope.refreshController();
      }
    };


    $scope.validateQuery = function() {
      var sql = $scope.optionsModel["/content/sites_EXPORT_filter/query"];
      if(sql == null || sql == "") {
        $scope.validateQueryResultMessageClassExt = "uiIconError";
        $scope.validateQueryResultMessageClass = "alert-error";
        $scope.validateQueryResultMessage = $scope.i18n.emptyQuery;
      } else {
        var selectedSites = $scope.resources["/content/sites"].filter(function(element) { return element.selected && element.path.indexOf("/content/sites") == 0; });
        var paramsSites = "";
        for(var i=0; i<selectedSites.length; i++) {
          paramsSites += "&sites=" + selectedSites[i].path;
        }

        $http({
          method: 'POST',
          url: stagingContainer.jzURL('StagingExtensionController.executeSQL'),
          data: 'sql=' + encodeURIComponent(sql) + paramsSites,
          headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).success(function (data) {
			if(data.indexOf('<body') >= 0 || data.indexOf('<head') >= 0) {
	            $scope.validateQueryResultMessage = $scope.i18n.sessionTimeout;
	            $scope.validateQueryResultMessageClassExt = "uiIconError";
	            $scope.validateQueryResultMessageClass = "alert-error";
        	} else {
	            $scope.validateQueryResultMessage = data;
	            $scope.validateQueryResultMessageClass = "alert-info";
	            $scope.validateQueryResultMessageClassExt = "uiIconInfo";
        	}
          }).error(function (data) {
            $scope.validateQueryResultMessage = data;
            $scope.validateQueryResultMessageClass = "alert-error";
            $scope.validateQueryResultMessageClassExt = "uiIconError";
          });
      }
    };

    $('#stagingCtrl').css('visibility', 'visible');
    $(".stagingLoadingBar").remove();
  };
  return stagingCtrl;
});