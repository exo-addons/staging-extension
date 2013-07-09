(function($) {
	$('#extension-genrator-portlet .list-checkbox').on(
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

//				if (checked) {
					// add Selected class to current label
//					$(this).parent().find('label').each(function() {
//						this.className = this.className + ' SelectedNode';
//					});
//				} else {
//					if (!childrenChecked) {
//						$(this).parent().parent().parent().children('label')
//								.each(
//										function() {
//											this.className = this.className
//													.replace(' SelectedNode',
//															'');
//										});
//					}
//					$(this).parent().find('label').each(
//							function() {
//								this.className = this.className.replace(
//										' SelectedNode', '');
//							});
//				}

				// Select all sub checkboxes
				$(this).parent().find('.list-checkbox').each(function() {
					this.checked = checked;
				});
				$('#exportImportForm').jzLoad(
						"ExtensionGeneratorController.selectResources()", {
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

	window.paintTables = function() {
		$('.grid_datatable').each(function() {
			$(this).dataTable({
				bSort : false,
				"aoColumns" : [ {
					"asSorting" : [ "asc" ]
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
	};
})($);