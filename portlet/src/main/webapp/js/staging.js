(function($) {
  $.fn.DataTable = jQuery.fn.dataTable;
  $.fn.dataTable = jQuery.fn.dataTable;
  $.fn.dataTableSettings = jQuery.fn.dataTable.settings;
  $.fn.dataTableExt = jQuery.fn.dataTable.ext;


  /** Mode selection **/
	$('input[name=mode]').on("change", function() {
    if(this.checked) {
      $('#resultMessage').empty();

      var mode = $(this).val();
      if(mode == 'export') {
        $('.button-import').hide();
        $('.button-synchronize').hide();
        $('.mode-import-options').hide();
        $('.mode-sync-options').hide();
        $('.button-export').css('display', 'block');
      } else if(mode == 'import') {
        $('.button-export').hide();
        $('.button-synchronize').hide();
        $('.mode-sync-options').hide();
        $('.mode-import-options').show();
        $('.button-import').css('display', 'block');
      } else {
        $('.button-export').hide();
        $('.button-import').hide();
        $('.mode-import-options').hide();
        $('.mode-sync-options').show(0, reloadServers);
        $('.button-synchronize').css('display', 'block');
      }
    }
	});

  // Store the list of the synchronization servers
  var syncServersData;

  /** Reload synchronization servers **/
  reloadServers = function() {
    var syncServers = $('#syncServers');
    var syncServersTable = syncServers.find('#syncServersTable');
    var syncServersForm = $('#syncServersForm');

    var syncServersMessage = $('#syncServersMessage');
    syncServersMessage.hide();

    $.ajax({
        url : syncServers.jzURL('StagingExtensionController.getSynchonizationServers'),
        type: 'GET',
        dataType: 'json',
        cache: false,
        processData: false,
        contentType: false,
        success: function(data){
          syncServersData = data.synchronizationServers;

          var tblBody = syncServersTable.find('tbody');
          tblBody.empty();

          if(data.synchronizationServers.length > 0) {
            $.each(data.synchronizationServers, function(index, element) {
              var tr = document.createElement('tr');

              var tdRadiobox = document.createElement('td');
              var radio = document.createElement('input');
              radio.setAttribute('type', 'radio');
              radio.setAttribute('id', element.id);
              radio.setAttribute('name', 'syncServer');
              tdRadiobox.appendChild(radio);
              tr.appendChild(tdRadiobox);

              var tdName = document.createElement('td');
              tdName.appendChild(document.createTextNode(element.name));
              tr.appendChild(tdName);

              var tdHost = document.createElement('td');
              tdHost.appendChild(document.createTextNode(element.host));
              tr.appendChild(tdHost);

              var tdPort = document.createElement('td');
              tdPort.appendChild(document.createTextNode(element.port));
              tr.appendChild(tdPort);

              var tdUsername = document.createElement('td');
              tdUsername.appendChild(document.createTextNode(element.username));
              tr.appendChild(tdUsername);

              var tdPassword = document.createElement('td');
              tdPassword.appendChild(document.createTextNode(element.password));
              tr.appendChild(tdPassword);

              var tdSSL = document.createElement('td');
              var imgSSL = document.createElement('span');              
              if(element.ssl == 'true') {
                imgSSL.setAttribute('class', 'uiIconTick');
              } else {
                imgSSL.setAttribute('class', 'uiIconClose');
              }
              imgSSL.setAttribute('disabled', 'disabled');
              tdSSL.appendChild(imgSSL);
              tr.appendChild(tdSSL);

              var tdAction = document.createElement('td');
              var trash = document.createElement('span');
              trash.setAttribute('id', 'deleteSyncServer_'+element.id);
              trash.setAttribute('class', 'uiIconDelete delete-sync-server-action');
              tdAction.appendChild(trash);
              tr.appendChild(tdAction);

              tblBody.append(tr);
            });

            tblBody.find('input[name=syncServer]').on('click', function() {
              syncServersForm.hide();
            });

            /** Delete a synchronization server **/
            $(".delete-sync-server-action").on("click", function() {
              var actionId = $(this).attr('id');
              var serverId = actionId.substring(actionId.indexOf('_')+1);

              syncServersMessage.removeClass("alert-success alert-error").addClass("alert-info");
              syncServersMessage.html("Deleting server ...");
              syncServersMessage.show();

              $.ajax({
                url : syncServers.jzURL('StagingExtensionController.removeSynchonizationServer'),
                type: 'POST',
                data: 'id='+serverId,
                cache: false,
                success: function() {
                  reloadServers();
                  syncServersMessage.removeClass("alert-info alert-error").addClass("alert-success");
                  syncServersMessage.html("Server deleted !");
                  syncServersMessage.show();
                },
                error: function() {
                  syncServersMessage.removeClass("alert-success alert-info").addClass("alert-error");
                  syncServersMessage.html("Error while deleting the server");
                  syncServersMessage.show();
                }
              });
            });

            syncServersForm.css('display', 'none');
            syncServers.css('display', 'block');
          } else {
            syncServers.css('display', 'none');
            syncServersForm.css('display', 'block');
          }

        },
        error: function(xhr, ajaxOptions, thrownError) {
        }
    });
  };

  /** Click on New Server button **/
  $(".button-newserver").on("click", function() {
    var syncServers = $('#syncServers');
    syncServers.find('input[name=syncServer]').attr('checked', false);
    var syncServersForm = $('#syncServersForm');
    syncServersForm.show();
  });

  /** Click on Save As button **/
  $(".button-saveserver").on("click", function() {
    var syncServersForm = $('#syncServersForm')
    var host = syncServersForm.find('#inputHost');
    var port = syncServersForm.find('#inputPort');
    var username = syncServersForm.find('#inputUsername');
    var password = syncServersForm.find('#inputPassword');
    var ssl = syncServersForm.find('#inputSSL');
    var newSaveName = syncServersForm.find('#newSaveName');

    // validate form
    if(validateSyncServerForm(host, port, username, password, ssl)) {
      var serverName = newSaveName.val().trim();
      if(serverName) {
        newSaveName.closest('.control-group').removeClass('error');

        var syncServersMessage = $('#syncServersMessage');
        syncServersMessage.removeClass("alert-success alert-error").addClass("alert-info");
        syncServersMessage.html("Saving new server ...");
        syncServersMessage.show();

        $.ajax({
          url : syncServersForm.jzURL('StagingExtensionController.addSynchonizationServer'),
          type: 'POST',
          data: 'name='+serverName+'&host='+host.val().trim()+'&port='+port.val().trim()+'&username='+username.val().trim()+'&password='+password.val().trim()+'&ssl='+(ssl.attr('checked')? 'true' : 'false'),
          cache: false,
          success: function() {
            reloadServers();
            syncServersMessage.removeClass("alert-info alert-error").addClass("alert-success");
            syncServersMessage.html("Server saved !");
            syncServersMessage.show();
          },
          error: function() {
            syncServersMessage.removeClass("alert-success alert-info").addClass("alert-error");
            syncServersMessage.html("Error while saving the server");
            syncServersMessage.show();
          }
        });
      } else {
        newSaveName.closest('.control-group').addClass('error');
        newSaveName.focus();
      }
    }
  });

	/** Click on Export button **/
  $(".button-export").on("click", function() {
    var selectedResourcesCategories = $('.staging .left-column .resource-category-checkbox:checked');
    if(selectedResourcesCategories.length > 1) {
      alert('Only one resource category can be exported at a time');
      return;
    }

    var selectedResourcesCategoryPath = "";
    selectedResourcesCategories.each(function() {
      selectedResourcesCategoryPath = $(this).attr("id");
    });

    var options = "";

    // find export options
    $("input.option").each(function() {
      if(this.name.indexOf(selectedResourcesCategoryPath +"_EXPORT_") === 0) {
        if(options === "") {
          options = "?";
        } else {
          options += "&";
        }
        var optionFullName = this.name.substring((selectedResourcesCategoryPath +"_EXPORT_").length, this.name.length);
        var fieldValue = "";
        if(this.type === "checkbox") {
          fieldValue = this.checked;
        } else {
          fieldValue = this.value;
        }

        var indexSlash = optionFullName.indexOf("/");
        if(indexSlash > 0) {
          var optionName = optionFullName.substring(0, indexSlash);
          var optionValue = optionFullName.substring(indexSlash + 1, optionFullName.length) + ":" + fieldValue;
           options += optionName + "=" + optionValue;
        } else {
          options += optionFullName + "=" + this.value;
        }
      }
    });


    location.href = "/portal/rest/managed-components" + selectedResourcesCategoryPath + ".zip" + options;
  });

  $(".button-import").on("click", function() {
    var importFile = $("#filteToImport")[0];
    var resultMessage = $('#resultMessage');
    var form = new FormData();
    form.append('file', importFile.files[0]);

    $.ajax({
        url : resultMessage.jzURL('StagingExtensionController.importResources'),
        type: 'POST',
        data : form,
        cache: false,
        processData: false,
        contentType: false,
        beforeSend: function() {
          resultMessage.removeClass("alert-success alert-error").addClass("alert-info");
          resultMessage.html("Importing ...");
        },
        success: function(data){
          resultMessage.removeClass("alert-info alert-error").addClass("alert-success");
          resultMessage.html(data);
        },
        error: function(xhr, ajaxOptions, thrownError) {
          resultMessage.removeClass("alert-success alert-info").addClass("alert-error");
          resultMessage.html(xhr.responseText);
        }
    });
  });

  /** Click on Synchronize button **/
	$('.button-synchronize').on("click", function() {
	  var hostValue;
    var portValue;
    var usernameValue;
    var passwordValue;
    var isSSLValue;

    var selectedSyncServer = $('input[name=syncServer]:checked');
    // if a server has been selected in the list...
    if(selectedSyncServer.length == 1) {
      $.each(syncServersData, function(index, server) {
        if(selectedSyncServer.attr('id') == server.id) {
          hostValue = server.host;
          portValue = server.port;
          usernameValue = server.username;
          passwordValue = server.password;
          isSSLValue = server.ssl;
          return false;
        }
      });
    } else {
      var host = $('#inputHost');
      var port = $('#inputPort');
      var username = $('#inputUsername');
      var password = $('#inputPassword');
      var ssl = $('#inputSSL');

      $('#newSaveName').closest('.control-group').removeClass('error');
      if(validateSyncServerForm(host, port, username, password, ssl)) {
        hostValue = host.attr("value");
        portValue = port.attr("value");
        usernameValue = username.attr("value");
        passwordValue = password.attr("value");
        isSSLValue = (ssl.attr("checked")? 'true' : 'false');
      } else {
        return false;
      }
    }

		// Launch synchronization...
		var resultMessage = $('#resultMessage');
		resultMessage.html("Proceeding...");
		resultMessage.removeClass("alert-success alert-error").addClass("alert-info");
		resultMessage.jzLoad("StagingExtensionController.synchronize()", {
        "host" : hostValue,
        "port" : portValue,
        "username" : usernameValue,
        "password" : passwordValue,
        "isSSLString" : isSSLValue
      }, function (response, status, xhr) {
        if(status == "success") {
          resultMessage.removeClass("alert-info alert-error").addClass("alert-success");
        } else if(status == "error") {
          resultMessage.removeClass("alert-success alert-info").addClass("alert-error");
        }
        resultMessage.html(xhr.responseText);
      });
	});

  /** Validate sync server form **/
  var validateSyncServerForm = function(host, port, username, password, ssl) {
    hostValue = host.attr("value").trim();
    portValue = port.attr("value").trim();
    usernameValue = username.attr("value").trim();
    passwordValue = password.attr("value").trim();
    isSSLValue = (ssl.attr("checked")? 'true' : 'false');

    var firstErrorField = null;
    if(!hostValue) {
      host.closest('.control-group').addClass('error');
      if(firstErrorField == null) {
        firstErrorField = host;
      }
    } else {
      host.closest('.control-group').removeClass('error');
    }
    if(!portValue) {
      port.closest('.control-group').addClass('error');
      if(firstErrorField == null) {
        firstErrorField = port;
      }
    } else {
      port.closest('.control-group').removeClass('error');
    }
    if(!usernameValue) {
      username.closest('.control-group').addClass('error');
      if(firstErrorField == null) {
        firstErrorField = username;
      }
    } else {
      username.closest('.control-group').removeClass('error');
    }
    if(!passwordValue) {
      password.closest('.control-group').addClass('error');
      if(firstErrorField == null) {
        firstErrorField = password;
      }
    } else {
      password.closest('.control-group').removeClass('error');
    }
    if (!hostValue || !portValue || !usernameValue || !passwordValue) {
      firstErrorField.focus();
      $('#syncServersForm').show();
      return;
    }

    return true;
  }


  /** Open/Close Resource Categories **/
  $('.resource-category > label').on("click", function() {
    $(this).siblings('ul').toggle();
    $(this).closest('li').toggleClass('expanded');
  });

  /** Function which toggles a resource category **/
  toggleResourceCategorySelection = function(objCheckbox) {
    var checkboxId = $(objCheckbox).attr("id");
    var checked = $(objCheckbox).attr('checked')
        && ($(objCheckbox).attr('checked') == 'checked');
    if (!checked) {
      checked = false;
    }

    // Server update
    $.ajax({
      url : $(objCheckbox).jzURL('StagingExtensionController.selectResourcesCategory'),
      type: 'POST',
      dataType: 'json',
      data: { "path" : checkboxId, "checked" : checked },
      success: function(data){

        // manage resources
        var divResourcesTable = $(objCheckbox).parent().find('.divResourcesTable')
        if(divResourcesTable) {
          if(checked) {
            // fetch data
            $.ajax({
                url : divResourcesTable.jzURL('StagingExtensionController.getResourcesOfCategory'),
                type: 'GET',
                dataType: 'json',
                data: { path : checkboxId },
                cache: false,
                contentType: false,
                success: function(data){
                  divResourcesTable.empty();

                  var table = document.createElement('table');
                  table.setAttribute('id', 'table_'+checkboxId);
                  table.setAttribute('class', 'display dataTable tree_datatable');

                  var thead = document.createElement('thead');
                  table.appendChild(thead);
                  var tbody = document.createElement('tbody');
                  table.appendChild(tbody);

                  $.each(data.resources, function(index, element) {
                    var tr = document.createElement('tr');

                    var tdCheckbox = document.createElement('td');
                    var checkbox = document.createElement('input');
                    checkbox.setAttribute('type', 'checkbox');
                    checkbox.setAttribute('id', element.path);
                    checkbox.setAttribute('name', 'selectedResources');
                    checkbox.setAttribute('class', 'resource-checkbox');
                    checkbox.setAttribute('checked', 'checked');
                    checkbox.setAttribute('value', element.path);
                    tdCheckbox.appendChild(checkbox);
                    var label = document.createElement('label');
                    label.setAttribute('title', element.text);
                    label.setAttribute('class', 'leaf');
                    label.setAttribute('for', element.path);
                    label.innerHTML = element.text;
                    tdCheckbox.appendChild(label);
                    tr.appendChild(tdCheckbox);

                    tbody.appendChild(tr);
                  });

                  divResourcesTable.append(table);

                  var oTable = $(table).dataTable({
                    "aoColumns" : [ {
                      "bSortable" : false
                    } ],
                    "bPaginate" : false,
                    "bInfo" : false,
                    "oLanguage" : {
                      "sSearch" : "Search:",
                      "oSearch" : "bSmart"
                    },
                    "sScrollY" : 245
                  });

                  divResourcesTable.show();

                  var childrenChecked = false;
                  var allChildrenChecked = true;
                  $(objCheckbox).parent().find('.resource-checkbox').each(function() {
                    childrenChecked = childrenChecked || this.checked;
                    allChildrenChecked = allChildrenChecked && this.checked;
                  });

                  // Check parent category if all subcategories are checked
                  $(objCheckbox).closest('ul').closest('li').children('.resource-category-checkbox')
                      .each(
                          function() {
                            this.checked = allChildrenChecked;
                            this.indeterminate = childrenChecked
                                && !allChildrenChecked;
                          });

                  // Reload right panel with selected items
                  $('#selectedResourcesForm').jzLoad(
                      "StagingExtensionController.selectResources()");

                  divResourcesTable.find('.resource-checkbox').on("change",	function() {
                    var checkboxId = $(this).attr("id");
                    var checked = $(this).attr('checked')
                        && ($(this).attr('checked') == 'checked');
                    if (!checked) {
                      checked = false;
                    }

                    $('#selectedResourcesForm').jzLoad(
                        "StagingExtensionController.selectResources()", {
                          "path" : checkboxId,
                          "checked" : checked
                        });
                  });
                },
                error: function(xhr, ajaxOptions, thrownError) {
                }
            });
          } else {
            divResourcesTable.hide();
          }
        }
      }
    });
  };

  /** Select/unselect a resources category in the left tree **/
	$('.staging .left-column .resource-category-checkbox').on(
			"change",
			function() {
        toggleResourceCategorySelection(this);
	    });

  /** Unselect a resources category with the "remove" link **/
	unselectResourceCategory = function(resourcePath) {
        var resourceCategoryCheckbox = $("input[id='" + resourcePath + "']")[0];
        resourceCategoryCheckbox.checked = false;
        toggleResourceCategorySelection(resourceCategoryCheckbox);
			};

  // Init datatables in the left tree (use jquery datatable plugin)
	$(document).ready(function() {
		$('.tree_datatable').each(function() {
			$(this).dataTable({
				"aoColumns" : [ {
					"bSortable" : false
				} ],
				"bPaginate" : false,
				"bInfo" : false,
				"oLanguage" : {
					"sSearch" : "Search:",
					"oSearch" : "bSmart"
				},
				"sScrollY" : 245
			});
		});
	});

  // Init datatables in the right panel (use jquery datatable plugin)
	window.initSynchronizationForm = function() {
		$('.grid_datatable').each(function() {
			$(this).dataTable({
				bSort : false,
				"aoColumns" : [ {
					"bSortable" : false
				} ],
				"bFilter" : false,
				"bPaginate" : false,
				"bInfo" : false,
				"sScrollY" : 252,
				"oLanguage" : {
					"sSearch" : "",
					"oSearch" : false
				}
			});
		});

		if ($('.staging .right-column .option')) {
			$('.staging .right-column .option').each(function() {
			  $(this).on("change", function() {
          var name = $(this).attr("name");
          var value = $(this).attr("value");
          if ($(this).attr("type")&& $(this).attr("type") == 'checkbox') {
            var checkboxId = $(this).attr("id");
            var checked = $(this).attr('checked')	&& ($(this).attr('checked') == 'checked');
            value = (checked ? 'true'	: '');
          }
          $('#resultMessage')
              .jzLoad("StagingExtensionController.selectOption()",
                  {
                    "name" : name,
                    "value" : value
                  });
        });
			});
		}

		if ($('.sites-content-sql button')) {
			$('.sites-content-sql button').on(
					"click",
					function() {
						var sql = $('.sites-content-sql input[type=text]').attr(
								"value");
						$('#resultMessage').jzLoad(
								"StagingExtensionController.selectOption()", {
									"name" : "/content/sites_EXPORT_query",
									"value" : sql
								});
						$('#sitesContentSQLResult').html("Processing...");
						$('#sitesContentSQLResult').jzLoad(
								"StagingExtensionController.executeSQL()", {
									"sql" : sql
								});
					});
		}
	};
})($);