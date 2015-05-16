var envelope_handler, register,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

QVSViewer = (function(_super) {
  __extends(QVSViewer, _super);

  function QVSViewer(callback) {
    var gadgets;
    gadgets = ["QVSController"];
    QVSViewer.__super__.constructor.call(this, callback, "QVSViewer", gadgets);
  }

  QVSViewer.prototype.init = function() {
    var env;
    env = new Envelope("enter");
    return env.publish();
  };

  QVSViewer.prototype.openAppCallback = function(envelope, message) {
    if (/QVSController.xml$/.test(envelope.sender) || /QVSViewer.xml$/.test(envelope.sender)) {
      switch (envelope.event) {
        case "error":
          return console.log("Received error message from other gadget but the type is unknown: " + envelope.message["http://purl.org/dc/terms/type"]);
        case "enter":
          return console.log("OpenApp: entered", envelope, message);
        case "exit":
          return console.log("OpenApp: exited");
        default:
          return console.log("Received unknown event message from gadget: " + envelope.sender + " Message: " + envelope.event);
      }
    } else {
      return console.log("Received (ViewerGadget) unknown event message from unknown sender: " + envelope.sender + " Event: " + envelope.event + " Message: " + envelope.message);
    }
  };

  return QVSViewer;

})(MultiGadget);

var filters = {};
var query_key = null;

window.gadget_init = function(key, host, path) {
  var gadget;
  query_key = key;
  gadget = new QVSViewer(function(envelope, message, gadget) {
    return envelope_handler(envelope);
  });
  console.log("QVSViewer: ", gadget);
  // init_ui();
  gadget.connect();
  var qv = QV.fromKey(key, "viewer", "none", host, path);
  qv.addVisualization(key, function(event) {
    if (event.filter) {
      filters[event.filter.key] = event.filter.value;
    }
    var filterArray = [];
    for (var x in filters) {
      filterArray.push(filters[x]);
    }
    if (event.reload === true) {
      var container = document.getElementById("viewer");
      qv.fromKey(key,container,filterArray);
    }
  });
  register(key);
  return qv;
};

register = function(key) {
  var env = new Envelope("QVS_vis_register", void 0, {key: key});
  env.publish();
};

envelope_handler = function(env) {
  if (env.event.startsWith("QVS_filter_update")) {
    return filter_handler(env.message);
  } else if (env.event.startsWith("QVS_poll_visualizations")) {
    register(query_key);
  }
};

var filter_handler = function(message) {
  qv.trigger({filter: {key: message.filter, value:message.value}, reload: message.reload});
};

