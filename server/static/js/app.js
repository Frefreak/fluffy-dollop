$(document).ready(function() {
  $("#regButton").on('click', function (event) {
    event.preventDefault();
    var inputs = $("#regform input");
    var d = {"username": inputs[0].value, "password": inputs[1].value};
    $.ajax({
      type: "POST",
      url:  "/register",
      contentType: "application/json",
      data: JSON.stringify(d),
      complete: function (res) {
        var resJ = res["responseJSON"];
        //$("#prompt").removeAttr("hidden");
        $("#prompt").slideDown('fast');
        if (resJ.token == "") {
          $("#prompt")[0].className = "alert alert-danger";
          $("#prompt button")[0].nextSibling.textContent = resJ.message;
        } else {
          $("#prompt")[0].className = "alert alert-success";
          $("#prompt button")[0].nextSibling.textContent = "Register successfully!";
          window.location.replace("/tokens/" + resJ.token)
        }
      }
    });
    $(".closeModal").click();
    inputs.each(function (index, element) {
      element.value = "";
    });
  });
  
  $("#loginButton").on('click', function (event) {
    event.preventDefault();
    var inputs = $("#loginform input");
    var d = {"username": inputs[0].value, "password": inputs[1].value};
    $.ajax({
      type: "POST",
      url:  "/login",
      contentType: "application/json",
      data: JSON.stringify(d),
      complete: function (res) {
        var resJ = res["responseJSON"];
        $("#prompt").slideDown('fast');
        //$("#prompt").removeAttr("hidden");
        if (resJ.token == "") {
          $("#prompt")[0].className = "alert alert-danger";
          $("#prompt button")[0].nextSibling.textContent = resJ.message;
        } else {
          $("#prompt")[0].className = "alert alert-success";
          $("#prompt button")[0].nextSibling.textContent = "Login successfully!";
          window.location.replace("/tokens/" + resJ.token)
        }
      }
    });
    $(".closeModal").click();
    inputs.each(function (index, element) {
      element.value = "";
    });
  });

  $("#closeAlertButton").click(function (event) {
    event.preventDefault();
    $("#prompt").slideUp('fast');
  });

  $('#post').on('click', function(event) {
    event.preventDefault();
    var data = $('#posttext').val();
    var pathname = window.location.pathname.split('/');
    var token = pathname.pop();
    if (token == "")
      token = pathname.pop();
    if (data != "") {
      var d = {"token": token, "data": data};
      $.ajax({
        type: "POST",
        url: "/post",
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(d),
        complete: function (res) {
          var resJ = res.responseJSON;
          if (resJ.code == 200)
            alert("post successfully");
          else
            alert(resJ.code + ": " + resJ.msg);
        }
      });
    }
  });

  $('#sync').on('click', function(event) {
    event.preventDefault();
    var syncSocket = new WebSocket(websocket_url("/sync"));
    syncSocket.onopen = function(event) {
      var pathname = window.location.pathname.split('/');
      var token = pathname.pop();
      if (token == "")
        token = pathname.pop();
      syncSocket.send(JSON.stringify({"token": token}));
      syncSocket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        if (json.msg == "" && json.code == 200)
          alert("sync successfully");
        else {
          syncSocket.send(JSON.stringify({"msgid": json.msgid, "status": "ok"}));
          $('#synctext').append($('<li>'+json.msg+'</li>'))
        }
      }
    };
  });
});

function websocket_url(s) {
    var l = window.location;
    return ((l.protocol === "https:") ? "wss://" : "ws://") + l.host + s;
}
