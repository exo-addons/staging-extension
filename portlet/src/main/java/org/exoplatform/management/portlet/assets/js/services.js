define( "stagingServices", [], function ( )
{

var stagingService = function() {

  var service = {};

  service.getOptionsAsQueryString = function(options, selectedResourcesCategoryPath, type, addNamespace) {
    var optionsQuery = "";
    for(optionName in options) {
      if(optionName.indexOf(selectedResourcesCategoryPath + "_" + type + "_") === 0) {
        if(optionsQuery !== "") {
          optionsQuery += "&";
        }
        var optionFullName = optionName.replace((selectedResourcesCategoryPath + "_" + type + "_"), (selectedResourcesCategoryPath + "_"));
        var fieldValue = options[optionName];
        
        var indexSlash = optionFullName.indexOf("/");
        if(indexSlash > 0) {
          var optionName = optionFullName.substring(0, indexSlash);
          var optionValue = optionFullName.substring(indexSlash + 1, optionFullName.length) + ":" + fieldValue;
          if(addNamespace) {
            optionsQuery += "staging-option:"
          }
          optionsQuery += optionName + "=" + optionValue;
        } else {
          if(addNamespace) {
            optionsQuery += "staging-option:"
          }
          optionsQuery += optionFullName + "=" + fieldValue;
        }
      }
    }

    return optionsQuery;
  };

  return service;

};

  return stagingService;

});