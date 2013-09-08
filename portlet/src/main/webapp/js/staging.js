(function($) {
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
        $('.mode-sync-options').show();
        $('.button-synchronize').css('display', 'block');
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

    selectedResourcesCategories.each(function() {
      location.href = '/portal/rest/managed-components' + $(this).attr("id") + '.zip';
    });
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
		var host = $('#inputHost');
		var port = $('#inputPort');
		var username = $('#inputUsername');
		var password = $('#inputPassword');
		var ssl = $('#inputSSL');

		var hostValue = host.attr("value");
		var portValue = port.attr("value");
		var usernameValue = username.attr("value");
		var passwordValue = password.attr("value");
		var isSSLValue = (ssl.attr("checked")? 'true' : '');

    // Validate target server form
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
			return;
		}

		// Launch synchronization...
		$('#resultMessage').html("Proceeding...");
		$('#resultMessage').jzLoad("StagingExtensionController.synchronize()", {
			"host" : hostValue,
			"port" : portValue,
			"username" : usernameValue,
			"password" : passwordValue,
			"isSSLString" : isSSLValue
		});
	});


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

    // Select all sub checkboxes
    $(objCheckbox).parent().find('.resource-checkbox').each(function() {
      this.checked = checked;
    });

    // Show resources list
    if(checked) {
      $(objCheckbox).parent().find('.dataTables_wrapper').show();
    } else {
      $(objCheckbox).parent().find('.dataTables_wrapper').hide();
    }

    // Reload right panel with selected items
    $('#selectedResourcesForm').jzLoad(
        "StagingExtensionController.selectResources()", {
          "path" : checkboxId,
          "checked" : checked
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

	$('.staging .left-column .resource-checkbox').on("change",	function() {
    // TODO update right panel
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

  // Init datatables in the left tree (use jquery datatable plugin)
	$(document).ready(function() {
		$.fn.DataTable = jQuery.fn.dataTable;
		$.fn.dataTable = jQuery.fn.dataTable;
		$.fn.dataTableSettings = jQuery.fn.dataTable.settings;
		$.fn.dataTableExt = jQuery.fn.dataTable.ext;

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
			$('.staging .right-column .option')
					.each(
							function() {
								$(this)
										.on(
												"change",
												function() {
													var name = $(this).attr(
															"name");
													var value = $(this).attr(
															"value");
													if ($(this).attr("type")
															&& $(this).attr(
																	"type") == 'checkbox') {
														var checkboxId = $(this)
																.attr("id");
														var checked = $(this)
																.attr('checked')
																&& ($(this)
																		.attr(
																				'checked') == 'checked');
														value = (checked ? 'true'
																: '');
													}
													$('#resultMessage')
															.jzLoad(
																	"StagingExtensionController.selectOption()",
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
									"name" : "/content/sites/EXPORT/query",
									"value" : sql
								});
						$('#sitesContentSQLResult').html("Processing...");
						$('#sitesContentSQLResult').jzLoad(
								"StagingExtensionController.executeSQL()", {
									"sql" : sql
								});
					});
			$('.sites-content-sql input[type=text]').on(
					"blur",
					function() {
						var sql = $('.sites-content-sql input[type=text]').attr(
								"value");
						$('#resultMessage').jzLoad(
								"StagingExtensionController.selectOption()", {
									"name" : "/content/sites/EXPORT/query",
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