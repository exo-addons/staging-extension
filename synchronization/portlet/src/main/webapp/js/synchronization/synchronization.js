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

		if($('#synchronization-portlet .rightColumn .option')) {
			$('#synchronization-portlet .rightColumn .option').each(function() {
				$(this).on("change", function() {
					var name = $(this).attr("name");
					var value = $(this).attr("value");
					if($(this).attr("type") && $(this).attr("type") == 'checkbox') {
						var checkboxId = $(this).attr("id");
						var checked = $(this).attr('checked')
								&& ($(this).attr('checked') == 'checked');
						value = (checked ? 'true':'');
					}
					$('#sync_message').jzLoad("SynchronizationController.selectOption()", {
								"name" : name,
								"value" : value
							});
				});
			});
		}

		$('#button-synchronize').on("click", function() {
			var host = $('#inputHost').attr("value");
			var port = $('#inputPort').attr("value");
			var username = $('#inputUsername').attr("value");
			var password = $('#inputPassword').attr("value");
			var isSSLString = $('#inputSSL').attr("checked");
			isSSLString = (isSSLString ? 'true':'');
			$('#sync_message').jzLoad("SynchronizationController.synchronize()", {
						"host" : host,
						"port" : port,
						"username" : username,
						"password" : password,
						"isSSLString" : isSSLString
					});
		});
	};
})($);