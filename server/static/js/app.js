var token = null;
$("#regButton").click(function (event) {
  event.preventDefault();
  var inputs = $("#regform input");
  var d = {"username": inputs[0].value, "password": inputs[1].value};
  $.ajax({
    type: "POST",
    url:  "register",
    contentType: "application/json",
    data: JSON.stringify(d),
    complete: function (res) {
      var resJ = res["responseJSON"];
      $("#prompt").removeAttr("hidden");
      if (resJ["token"] == "") {
        $("#prompt")[0].className = "alert alert-danger";
        $("#prompt button")[0].nextSibling.textContent = resJ["message"];
      } else {
        $("#prompt")[0].className = "alert alert-success";
        $("#prompt button")[0].nextSibling.textContent = "Register successfully!";
        token = resJ["token"];
      }
    }
  });
  $(".closeModal").click();
  inputs.each(function (index, element) {
    element.value = "";
  });
});

$("#loginButton").click(function (event) {
  event.preventDefault();
  var inputs = $("#loginform input");
  var d = {"username": inputs[0].value, "password": inputs[1].value};
  $.ajax({
    type: "POST",
    url:  "login",
    contentType: "application/json",
    data: JSON.stringify(d),
    complete: function (res) {
      var resJ = res["responseJSON"];
      $("#prompt").removeAttr("hidden");
      if (resJ["token"] == "") {
        $("#prompt")[0].className = "alert alert-danger";
        $("#prompt button")[0].nextSibling.textContent = respJ["message"];
      } else {
        $("#prompt")[0].className = "alert alert-success";
        $("#prompt button")[0].nextSibling.textContent = "Login successfully!";
        token = resJ["token"];
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
  $("#prompt").prop("hidden", true);
});
