(function($) {
	$('.Clickable').on("click", function() {
		$('#exportImportForm').jzLoad("StagingExtensionController.displayForm()", {
			"path" : $(this).attr("title")
		});
		$('.SelectedNode').removeClass('SelectedNode');
		$(this).addClass('SelectedNode');
	});
	$("#button-download").live("click", function() {
		location.href = $(this).attr("alt");
	});
    $("#importButton").live("click", function() {
    	var importFile = $("#filteToImport")[0];
		var importMessageSpan = $('#importMessage');
		var form = new FormData();
		form.append('file', importFile.files[0]);
		var actionURL = $("#importForm").attr("action");
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
				importMessageSpan.html("Proceeding ...");
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
  	});
})($);