var require = eXo.require, requirejs = eXo.require,define = eXo.define;

require( ["stagingServices"], function ( stagingService )
{
  var stagingApp = angular.module('stagingApp', []);
  //stagingApp.controller('stagingCtrl', stagingControllers);
  stagingApp.service('stagingService', stagingService);
});