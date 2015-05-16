var register_handler, envelope_handler, update_filter, qv, gadget, gadget_init, poll_visualizations,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

QVSController = (function(_super) {
  __extends(QVSController, _super);

  function QVSController(callback) {
    var gadgets;
    gadgets = ["QVSController"];
    QVSController.__super__.constructor.call(this, callback, "QVSController", gadgets);
  }

  QVSController.prototype.init = function() {
    var env;
    env = new Envelope("enter");
    return env.publish();
  };

  QVSController.prototype.openAppCallback = function(envelope, message) {
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

  return QVSController;

})(MultiGadget);


gadget_init = function(key, host, path) {
  gadget = new QVSController(function(envelope, message, gadget) {
    return envelope_handler(envelope);
  });
  console.log("QVSController: ", gadget);
  // init_ui();
  gadget.connect();
  // var qv = QV.fromKey("939916599", null, null, "http://localhost:8080/", "QVS/");
  qv = new QV.Visualizer(host, path);
  poll_visualizations();
  return qv;
};

update_filter = function(filter, value, reload) {
  var env = new Envelope("QVS_filter_update", void 0, {filter: filter, value: value, reload: reload});
  env.publish();
};

poll_visualizations = function() {
  var env = new Envelope("QVS_poll_visualizations", void 0, {});
  env.publish();
};

envelope_handler = function(env) {
  if (env.event.startsWith("QVS_vis_register")) {
    register_handler(env.message);
  }
};

var visualizations = {};
var filters = {};

register_handler = function(m) {
  var v = visualizations[m.key];
  if (v) {
    return;
  }
  QV.getFilters(m.key, qv, null, "controller", true, function(database, filter, user, values, select) {
    visualizations[m.key] = {database: database, filter:filter, user:user, values:values};
    select.on("change", function() {
      update_filter(filter, this.value, true);
    });
  });
};
