$(document).ready(function () {
    registerSearch();
    registerTemplate();
    room.join();
});

function registerSearch() {
    $("#searchBtn").on("click", function (ev) {
        ev.preventDefault();
        $.get("search", {q: $("#q").val()}, function (data) {
            $("#resultsBlock").html(Mustache.render(template, data));
        });
    });

    $("#streamBtn").on("click", function (ev) {
        ev.preventDefault();
        var q = $("#q");
        room.send("SUBSCRIBE:" + q.val());
        q.val("");
    });

    $("#alertSubbed").hide();
}

function registerTemplate() {
    template = $("#template").html();
    Mustache.parse(template);
}

if (!window.WebSocket) {
    alert("WebSocket not supported by this browser");
}
var room = {
    join: function() {
        var location = "ws://localhost:8080/stream";
        this._ws=new WebSocket(location);
        this._ws.onmessage=this._onmessage;
        this._ws.onbeforeunload = this._onbeforeunload;
        this._ws.onclose=this._onclose;
    },
    send: function(msg){
        this._ws.send(msg);
    },
    _onmessage: function(m) {
        if (m.data){
            var json = $.parseJSON(m.data);
            if(json["msg"] && json["msg"] === "SUBSCRIBED"){
                var alert = $('#alertSubbed');
                alert.show();
                $("#subbedTerm").text(json['term']);
            }else {
                console.log(json);
                $("#resultsBlock").append(Mustache.render(template, json));
            }
        }
    },
    _onclose: function(m) {
        this._ws=null;
    },
    _onbeforeunload: function () {
        this.send("END:");
    }
};
