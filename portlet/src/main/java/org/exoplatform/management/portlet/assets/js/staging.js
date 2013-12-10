// HACK : redefine jz js functions
// TODO : find a way to use juzu-ajax js
$.fn.jz = function() {
  return this.closest(".jz");
};
$.fn.jzURL = function(mid) {
  return this.
    jz().
    children().
    filter(function() { return $(this).data("method-id") == mid; }).
    map(function() { return $(this).data("url"); })[0];
};
var re = /^(.*)\(\)$/;
$.fn.jzLoad = function(url, data, complete) {
  var match = re.exec(url);
  if (match !== null) {
    var repl = this.jzURL(match[1]);
    url = repl || url;
  }
  if (typeof data === "function") {
    complete = data;
    data = null;
  }
  return this.load(url, data, complete);
};
