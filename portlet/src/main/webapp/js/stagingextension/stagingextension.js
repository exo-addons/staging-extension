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
		var importFile = document.getElementById('filteToImport');
		var importMessageSpan = document.getElementById('importMessage');
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
				importMessageSpan.className = "progressBar";
				importMessageSpan.innerHTML = "Proceeding ...";
		    },
			success: function(data){
		        importMessageSpan.className = "success";
		        importMessageSpan.innerHTML = data;
			},
			error: function (xhr, ajaxOptions, thrownError) {
				importMessageSpan.className = "error";
				importMessageSpan.innerHTML = xhr.responseText;
			}
        }
        );
  	});
})($);