$(document).ready(function () {
    registerSearch();
    registerTemplate();
});

function registerSearch() {
    $("#search").submit(function (ev) {
        ev.preventDefault();
        $.get($(this).attr('action'), {q: $("#q").val(), max: 1}, function (data) {
            $("#resultsBlock").html(Mustache.render(template, data));
        });
    });
}

function registerTemplate() {
    template = $("#template").html();
    Mustache.parse(template);
}
