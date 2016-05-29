var contexts = [
{ "title": "Send selection text",
  "contexts": ["selection"],
  "onclick": postContent("selectionText")
},
{ "title": "Send this link",
  "contexts": ["link"],
  "onclick": postContent("linkUrl")
},
{ "title": "Send image link",
  "contexts": ["image"],
  "onclick": postContent("srcUrl")
}
];

function postContent(key) {
  return function(info, tab) {
    var msg = info[key];
    postMsg(msg);
  }
}

function postMsg(msg) {
  chrome.storage.sync.get('token', function(items) {
    if (chrome.runtime.lastError)
      alert('something unexpected happened');
    else if (!items.token) {
      alert('You must login first');
    } else {
      var tok = items.token;
      if (msg != "")
        ajaxPost(msg, tok);
    }
  });
}

function ajaxPost(msg, token) {
  var d = {"token": token, "data": msg};

  chrome.storage.sync.get('server', function(items) {
    var postSocket = new WebSocket(items.server + '/post');
    postSocket.onopen = function(event) {
      postSocket.send(JSON.stringify(d));
      postSocket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        if (json.msg == "" && json.code == 200)
          alert("post successfully");
        else
          alert(json.code + ": " + json.msg);
      }
    }
  });
}

function initAll() {
  for (i in contexts)
    chrome.contextMenus.create(contexts[i]);
  loadConfs();
}

function loadConfs() {
  var sto = chrome.storage.sync;
  sto.set({'server': 'ws://104.207.144.233:4564'});
}

initAll();
