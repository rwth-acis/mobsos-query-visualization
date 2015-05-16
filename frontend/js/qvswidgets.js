Gadget = (function() {
  function Gadget(callback) {
    this.callback = callback;
    gadgets.openapp.connect(this.callback);
    window.onunload = function() {
      return gadgets.openapp.disconnect();
    };
  }

  Gadget.prototype.publish = function(envelope) {
    return envelope.publish();
  };

  Gadget.prototype.connect = function() {
    var envelope;
    envelope = new Envelope("enter");
    return envelope.publish();
  };

  Gadget.prototype.disconnect = function() {
    var envelope;
    envelope = new Envelope("exit");
    return envelope.publish();
  };

  return Gadget;

})();

Envelope = (function() {
  function Envelope(event, type, message, uri, date, sharing, sender, viewer) {
    this.event = event;
    this.type = type !== null ? type : "namespaced-properties";
    this.message = message;
    this.uri = uri;
    this.date = date;
    this.sharing = sharing;
    this.sender = sender;
    this.viewer = viewer;
  }

  Envelope.prototype.publish = function() {
    return gadgets.openapp.publish(this);
  };

  return Envelope;

})();
