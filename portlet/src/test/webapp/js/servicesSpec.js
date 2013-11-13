describe('StagingService test suite', function() {
  beforeEach(module('stagingApp'));


  describe('StagingService tests', function() {
    var options = {
      '/organization/users_EXPORT_filter/option1': true,
      '/organization/users_EXPORT_filter/option2': false,
      '/organization/users_EXPORT_option3': 'value3',
      '/organization/users_IMPORT_filter/option4': 'ok',
      '/organization/groups_IMPORT_filter/option5': '25'
    };

    it('should return query options for EXPORT and without namespace', inject(
      function(StagingService) {
        var selectedResourcesCategoryPath = '/organization/users';
        var type = "EXPORT";
        var addNamespace = false;

        var optionsQuery = StagingService.getOptionsAsQueryString(options, selectedResourcesCategoryPath, type, addNamespace);


        expect(optionsQuery).toEqual('filter=option1:true&filter=option2:false&option3=value3');
    }));

    it('should return query options for IMPORT and with namespace', inject(
      function(StagingService) {
        var selectedResourcesCategoryPath = '/organization/users';
        var type = "IMPORT";
        var addNamespace = true;

        var optionsQuery = StagingService.getOptionsAsQueryString(options, selectedResourcesCategoryPath, type, addNamespace);


        expect(optionsQuery).toEqual('staging-option:filter=option4:ok');
      }));
  });
});
