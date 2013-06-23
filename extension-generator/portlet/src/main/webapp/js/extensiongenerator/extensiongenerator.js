(function($) {
	$('#extension-genrator-portlet .list-checkbox').on("change", function() {
		var checkboxId = $(this).attr("id");
		var checked = $(this).attr('checked') && ($(this).attr('checked') == 'checked');
		if(!checked) {
			checked = false;
		}
		var childrenChecked = false;
		var allChildrenChecked = true;
		$(this).parent().parent().find('.list-checkbox').each(function() {
			childrenChecked = childrenChecked || this.checked;
			allChildrenChecked = allChildrenChecked && this.checked;
		});
		$(this).parent().parent().parent().children('.list-checkbox').each(function() {
			this.checked = allChildrenChecked;
			this.indeterminate = childrenChecked && !allChildrenChecked;
		});
		if(checked) {
			// add Selected class to current label
			$(this).parent().find('label').each(function() {
				this.className = this.className + ' SelectedNode';
			});
		} else {
			if(!childrenChecked) {
				$(this).parent().parent().parent().children('label').each(function() {
					this.className = this.className.replace(' SelectedNode','');
				});
			}
			$(this).parent().find('label').each(function() {
				this.className = this.className.replace(' SelectedNode','');
			});
		}
		// Select all sub checkboxes
		$(this).parent().find('.list-checkbox').each(function() {
            this.checked = checked;                        
        });
		$('#exportImportForm').jzLoad("ExtensionGeneratorController.selectResources()", {
			"path" : checkboxId,
			"checked" : checked
		});
	});
	$("#extension-genrator-portlet #button-download").live("click", function() {
		var actionURL = $(this).attr("action");
		$.ajax(
        {
            url : actionURL,
            type: 'POST',
            data :  form,
            cache: false,
            processData: false,
			contentType: false,
			beforeSend: function(){
				importMessageSpan.attr("class", "progressBar");
				importMessageSpan.html("processing ...");
		    },
			success: function(data){
		        importMessageSpan.attr("class", "success");
		        importMessageSpan.html(data);
			},
			error: function (xhr, ajaxOptions, thrownError) {
				importMessageSpan.attr("class", "error");
				importMessageSpan.html(xhr.responseText);
			}
        }
        );
		location.href = $(this).attr("alt");
  	});
})($);