$('#loginButton').on('click', function(event) {
  event.preventDefault();
  var username = $('#loginform').find('#signinusername').val();
  var password = $('#loginform').find('#signinpassword').val();
  ajaxLogin(username, password);

});

function ajaxLogin(username, password) {
  var d = {"username": username, "password": password, "deviceName": "chrome"};

  chrome.storage.sync.get('server', function(items) {
    var loginSocket = new WebSocket(items.server + '/login');
    loginSocket.onopen = function(event) {
      loginSocket.send(JSON.stringify(d));
      loginSocket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        if (json.msg == "" && json.code == 200) {
          alert("login successfully");
          chrome.storage.sync.set({'token': json.token});
          window.close();
        } else {
          alert(json.code + ": " + json.msg);
          window.close();
        }
      }
    }
  });
}

$('#regButton').on('click', function(event) {
  event.preventDefault();
  var username = $('#regform').find('#signupusername').val();
  var password = $('#regform').find('#signuppassword').val();
  ajaxRegister(username, password);

});

function ajaxRegister(username, password) {
  var d = {"username": username, "password": password, "deviceName": "chrome"};

  chrome.storage.sync.get('server', function(items) {
    var regSocket = new WebSocket(items.server + '/register');
    regSocket.onopen = function(event) {
      regSocket.send(JSON.stringify(d));
      regSocket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        if (json.msg == "" && json.code == 200) {
          alert("Register successfully");
          ajaxLogin(username, password);
        } else {
          alert(json.code + ": " + json.msg);
          window.close();
        }
      }
    }
  });
}
