function StagingCtrl($scope, $http) {
  var stagingContainer = $('#staging');

  $scope.mode = "export";
  $scope.syncServersMessage = "";
  $scope.syncServersMessageClass = "alert-info";
  $scope.resultMessage = "";
  $scope.resultMessageClass = "alert-info";

  $scope.changeMode = function(mode) {
    $scope.mode = mode;
    if(mode === 'synchronize') {
      $scope.syncServersMessage = "";
      $scope.loadServers();
    }
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

    $scope.syncServersMessageClass = "alert-info";
    $scope.syncServersMessage = "Saving new server ...";
    $http.post(stagingContainer.jzURL('StagingExtensionController.addSynchonizationServer') + '&name='+server.name+'&host='+server.host+'&port='+server.port+'&username='+server.username+'&password='+server.password+'&ssl='+(server.ssl? 'true' : 'false')).success(function (data) {
      $scope.syncServersMessageClass = "alert-success";
      $scope.syncServersMessage = "Server saved !";
      $scope.loadServers();
    }).error(function (data) {
      $scope.syncServersMessageClass = "alert-error";
      $scope.syncServersMessage = "Error while saving the server";
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
  //
  $scope.resources = [];

  $http.get(stagingContainer.jzURL('StagingExtensionController.getCategories')).success(function (data) {
    $scope.categories = data.categories;
  });

  $scope.toggleCategorySelection = function(selectedCategory) {
    $http.post(stagingContainer.jzURL('StagingExtensionController.selectResourcesCategory') + '&path=' + selectedCategory + '&checked=' + $scope.categoriesModel[selectedCategory]).success(function (data) {
      if($scope.categoriesModel[selectedCategory]) {
        $http.get(stagingContainer.jzURL('StagingExtensionController.getResourcesOfCategory') + '&path=' + selectedCategory).success(function (data) {
          $scope.resources[selectedCategory] = [];
          for(var i=0; i<data.resources.length; i++) {
            $scope.resources[selectedCategory].push({
              "path": data.resources[i].path,
              "text": data.resources[i].text,
              "selected": true
            });
          }
        });
      } else {
        delete $scope.resources[selectedCategory];
      }
    });
  };

  $scope.unselectCategory = function(selectedCategory) {
    $scope.categoriesModel[selectedCategory] = false;
    $scope.toggleCategorySelection(selectedCategory);
  };

  // Temporary method used to update the server state
  $scope.selectResource = function(resource) {
    $http.post(stagingContainer.jzURL("StagingExtensionController.selectResources") + "&path=" + resource.path + "&checked=" + resource.selected);
  };


  /**********************************************************************/
  /*                 ACTIONS (export/import/synchronize)                */
  /**********************************************************************/

  // function which lists the selected categories
  // TODO see if it can be improved with a better binding
  $scope.getSelectedCategories = function() {
    var selectedCategories = [];
    for(var category in $scope.categoriesModel) {
      if($scope.categoriesModel[category] === true) {
        selectedCategories.push(category);
      }
    }
    return selectedCategories;
  };

  // export action
  $scope.exportResources = function() {
    var selectedCategories = $scope.getSelectedCategories();
    var nbOfSelectedCategories = selectedCategories.length;

    if(nbOfSelectedCategories === 0) {
      alert('No resource category selected');
      return;
    } else if(nbOfSelectedCategories > 1) {
      alert('Only one resource category can be exported at a time');
      return;
    }

    location.href = '/portal/rest/managed-components' + selectedCategories[0] + '.zip';
  };

  // import action
  $scope.importResources = function() {
    if(!$scope.importFile) {
      $scope.resultMessageClass = "alert-error";
      $scope.resultMessage = "No file selected";
      return;
    }

    var selectedCategories = $scope.getSelectedCategories();
    var nbOfSelectedCategories = selectedCategories.length;

    if(nbOfSelectedCategories === 0) {
      $scope.resultMessageClass = "alert-error";
      $scope.resultMessage = "No resource category selected";
      return;
    } else if(nbOfSelectedCategories > 1) {
      $scope.resultMessageClass = "alert-error";
      $scope.resultMessage = "Only one resource category can be imported at a time";
      return;
    }

    var form = new FormData();
    form.append('file', $scope.importFile);

    $scope.resultMessageClass = "alert-info";
    $scope.resultMessage = "Importing ...";
    $http({
      url: stagingContainer.jzURL("StagingExtensionController.importResources"),
      data : form,
      method : 'POST',
      headers : {'Content-Type':false},
      transformRequest: function(data) { return data; }
    }).success(function (data) {
      $scope.resultMessageClass = "alert-success";
      $scope.resultMessage = data;
    }).error(function (data) {
      $scope.resultMessageClass = "alert-error";
      $scope.resultMessage = "Import failed. " + data;
    });
  };

  $scope.setFile = function(element) {
      $scope.$apply(function($scope) {
          $scope.importFile = element.files[0];
      });
  };

  // synchronize action
  $scope.synchronizeResources = function() {
    var targetServer;
    if($scope.selectedServer) {
      targetServer = $scope.selectedServer;
    } else if($scope.servers.length === 0 || $scope.isNewServerFormDisplayed) {
      if(!$scope.validateSyncServerForm($scope.newServer, false)) {
        return;
      }
      targetServer = $scope.newServer;
    } else {
      $scope.resultMessageClass = "alert-error";
      $scope.resultMessage = "No destination server selected...";
      return;
    }

    var selectedResources = [];
    for(categoryResources in $scope.resources) {
      var selectedCategoryResources = $scope.resources[categoryResources].filter(function(element) { return element.selected === true; });
      for(var i=0; i<selectedCategoryResources.length; i++) {
        selectedResources.push(selectedCategoryResources[i]);
      }
    }

    if(selectedResources.length === 0) {
      $scope.resultMessageClass = "alert-error";
      $scope.resultMessage = "No resources selected";
      return;
    }

		// Launch synchronization...
    $scope.resultMessageClass = "alert-info";
    $scope.resultMessage = "Proceeding...";

    $http.get(stagingContainer.jzURL('StagingExtensionController.synchronize') + '&host=' + targetServer.host + '&port=' + targetServer.port + '&username=' + targetServer.username + '&password=' + targetServer.password + '&isSSLString=' + targetServer.isSSLString).success(function (data) {
        $scope.resultMessageClass = "alert-success";
        $scope.resultMessage = data;
      }).error(function (data) {
        $scope.resultMessageClass = "alert-error";
        $scope.resultMessage = data;
      });
  };

}


// HACK : redefine jz js functions
// TODO : find a way to use juzu-ajax js
$.fn.jz = function() {
  return this.closest(".jz");
};
$.fn.jzURL = function(mid) {
  return this.
    jz().
    children().
    filter(function() { return $(this).data("method-id") == mid; }).
    map(function() { return $(this).data("url"); })[0];
};
var re = /^(.*)\(\)$/;
$.fn.jzLoad = function(url, data, complete) {
  var match = re.exec(url);
  if (match !== null) {
    var repl = this.jzURL(match[1]);
    url = repl || url;
  }
  if (typeof data === "function") {
    complete = data;
    data = null;
  }
  return this.load(url, data, complete);
};


var stagingApp = angular.module('stagingApp',[]);
stagingApp.controller('StagingCtrl', StagingCtrl);