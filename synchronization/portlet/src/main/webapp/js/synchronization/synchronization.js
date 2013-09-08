(function($) {
	$('#synchronization-portlet .leftColumn .list-checkbox').on(
			"change",
			function() {
				var checkboxId = $(this).attr("id");
				var checked = $(this).attr('checked')
						&& ($(this).attr('checked') == 'checked');
				if (!checked) {
					checked = false;
				}

				var childrenChecked = false;
				var allChildrenChecked = true;
				$(this).closest('li').find('.list-checkbox').each(function() {
					childrenChecked = childrenChecked || this.checked;
					allChildrenChecked = allChildrenChecked && this.checked;
				});

				$(this).closest('ul').closest('li').children('.list-checkbox')
						.each(
								function() {
									this.checked = allChildrenChecked;
									this.indeterminate = childrenChecked
											&& !allChildrenChecked;
								});
				// Select all sub checkboxes
				$(this).parent().find('.list-checkbox').each(function() {
					this.checked = checked;
				});
				$('#exportImportForm').jzLoad(
						"SynchronizationController.selectResources()", {
							"path" : checkboxId,
							"checked" : checked
						});
			});
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

	$('#button-synchronize').on("click", function() {
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
		$('#sync_message').html("Proceeding...");
		$('#sync_message').jzLoad("SynchronizationController.synchronize()", {
			"host" : hostValue,
			"port" : portValue,
			"username" : usernameValue,
			"password" : passwordValue,
			"isSSLString" : isSSLValue
		});
	});

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

		if ($('#synchronization-portlet .rightColumn .option')) {
			$('#synchronization-portlet .rightColumn .option')
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
													$('#sync_message')
															.jzLoad(
																	"SynchronizationController.selectOption()",
																	{
																		"name" : name,
																		"value" : value
																	});
												});
							});
		}

		if ($('.SitesContentSQL button')) {
			$('.SitesContentSQL button').on(
					"click",
					function() {
						var sql = $('.SitesContentSQL input[type=text]').attr(
								"value");
						$('#sync_message').jzLoad(
								"SynchronizationController.selectOption()", {
									"name" : "/content/sites/EXPORT/query",
									"value" : sql
								});
						$('#SitesContentSQLResult').html("Processing...");
						$('#SitesContentSQLResult').jzLoad(
								"SynchronizationController.executeSQL()", {
									"sql" : sql
								});
					});
		}
	};
})($);