var clock = null;

$(document).ready(function () {
    connectFeedback();
    connectControl();
    initializeAssignmentClock();
    initSubmissions();
    initSolutions();
});

function initSolutions() {
    $('span[data-solution]').click(function(e) {
        e.preventDefault()
        var uuid = $(this).attr('data-assignment');
        var assignmentName = $("#assignment-name").text();
        $.getJSON('/feedback/solution/' + uuid, function (data) {
            console.log(data);
            $tabs = createTabs(data);
            $("h5.modal-title").text("Solution for assignment " + assignmentName);
            $('#show-submission-modal .modal-body')
                .empty().append($tabs);
            $('#show-submission-modal textarea').each(function (idx) {
                var cm = CodeMirror.fromTextArea(this, {
                    lineNumbers: true,
                    mode: "text/x-java",
                    matchBrackets: true,
                    readOnly: true,
                    autofocus: true,
                });

                $tabs.find('a[id="' + data.files[idx].uuid + '"]').on('shown.bs.tab',
                    function (e) {
                        console.log('shown.bs.tab', e);
                        cm.refresh();
                    });

                $('#show-submission-modal').on('shown.bs.modal', function (e) {
                    console.log('shown.bs.modal', e);
                    cm.refresh();
                });

                var $wrapper = $(cm.getWrapperElement());
                $wrapper.resizable({
                    resize: function () {
                        cm.setSize($(this).width(), $(this).height());
                        cm.refresh();
                    }
                });

                var pos = $('#show-submission-modal .modal-body').position();
                var height = window.innerHeight - pos.top - 280;
                $wrapper.css('height', height + 'px');
                cm.refresh();
            });
            $('#show-submission-modal').modal('show');
        });
    })
}

function initSubmissions() {
    $('tr[data-team]').click(function (e) {
        e.preventDefault()
        var uuid = $(this).attr('data-team');
        var assignment = $(this).attr('data-assignment');
        var teamName = $(this).find("td:first").text();
        var assignmentName = $("#assignment-name").text();
        $.getJSON('/feedback/solution/'+ assignment +'/team/' + uuid, function (data) {
            console.log(data);
            $tabs = createTabs(data);
            $("h5.modal-title").text("Submission of team " + teamName + " for assignment " + assignmentName);
            $('#show-submission-modal .modal-body')
                .empty().append($tabs);
            $('#show-submission-modal textarea').each(function (idx) {
                var cm = CodeMirror.fromTextArea(this, {
                    lineNumbers: true,
                    mode: "text/x-java",
                    matchBrackets: true,
                    readOnly: true,
                    autofocus: true,
                });

                $tabs.find('a[id="' + data.files[idx].uuid + '"]').on('shown.bs.tab',
                    function (e) {
                        console.log('shown.bs.tab', e);
                        cm.refresh();
                    });
                
                $('#show-submission-modal').on('shown.bs.modal', function (e) {
                    console.log('shown.bs.modal', e);
                    cm.refresh();
                });

                var $wrapper = $(cm.getWrapperElement());
                $wrapper.resizable({
                    resize: function () {
                        cm.setSize($(this).width(), $(this).height());
                        cm.refresh();
                    }
                });

                var pos = $('#show-submission-modal .modal-body').position();
                var height = window.innerHeight - pos.top - 280;
                $wrapper.css('height', height + 'px');
                cm.refresh();
            });
            $('#show-submission-modal').modal('show');
        });
    });
}

function createTabs(data) {
    var $div = $('<div>');
    var $ul = $('<ul class="nav nav-tabs" role="tablist">');
    var $panes = $('<div class="tab-content">');
    $div.append($ul).append($panes);
    data.files.forEach(function (f, idx) {
        $ul.append('<li class="nav-item"><a class="nav-link' + (idx == 0 ? " active" : "") + '" id="' + f.uuid + '" href="#tab-' + idx + '" data-toggle="tab" role="tab"><span>' + f.filename + '</span></a></li>');
        $panes.append('<div class="tab-pane fade' + (idx === 0 ? ' show active' : '' + '') + '" role="tabpanel" id="tab-'+idx+'"><div class="tab-pane-content"><textarea>' + f.content + '</textarea></div></div>');
    });
    return $div;
}

function connectFeedback() {

    var socket = new WebSocket(
    		((window.location.protocol === "https:") ? "wss://" : "ws://") 
    		+ window.location.hostname 
    		+ ':' + window.location.port 
    		+ '/feedback/websocket');
    var stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/queue/feedbackpage',
            function (data) {
                var msg = JSON.parse(data.body);
                console.log("received", msg);
                if (handlers.hasOwnProperty(msg.messageType)) {
                    handlers[msg.messageType](msg);
                }
            });
    });

    var handlers = {};
    handlers['SUBMIT'] = function (msg) {
        process(msg);
    };
    handlers['TEST'] = function (msg) {
        process(msg);
    };
    handlers['TEAM_STARTED_TESTING'] = function (msg) {
        startTesting(msg);
    };
}

function connectControl() {
    var socket = new WebSocket(
    		((window.location.protocol === "https:") ? "wss://" : "ws://") 
    		+ window.location.hostname 
    		+ ':' + window.location.port 
    		+ '/control/websocket');
    var stompClientControl = Stomp.over(socket);
    stompClientControl.debug = null;
    stompClientControl.connect({}, function (frame) {
        console.log('Connected to /control');
        console.log('Subscribe to /queue/start');
        stompClientControl.subscribe('/queue/start', function (messageOutput) {
            console.log("/queue/start")
            window.location.reload();
        });
        stompClientControl.subscribe('/queue/stop', function (messageOutput) {
            console.log("/queue/stop")
            if (clock) {
                clock.stop();
            }
        });
        console.log('Subscribe to /control/queue/time');
        stompClientControl.subscribe('/queue/time', function (taskTimeMessage) {
            var message = JSON.parse(taskTimeMessage.body);
            if (clock) {
                clock.sync(message.remainingTime, message.totalTime);
            }
        });

    });
}

function startTesting(msg) {
    var uuid = msg.uuid;
    var $team = $('tr[data-team=' + uuid + ']');

    $team.removeClass('table-danger table-success');
    $team.addClass('table-info')
    setTimeout(function () {
        $team.removeClass('table-info')
    }, 500);
    $('span', $team)
        .removeClass('fa-check fa-times fas');

}

function process(message) {
    var team = message.uuid;
    var test = message.test;
    var submit = message.messageType === 'SUBMIT';
    var id = team + '-' + test;
    var $team = $('tr[data-team=' + team + ']');
    var $test = $('span[data-test=' + id + ' ]');
    console.log($team, $test);
    if (message.success) {
        if (submit) {
            $team.removeClass('table-danger')
            $team.addClass('table-success')
        }
        $test.removeClass('fas fa-times');
        $test.addClass('fas fa-check');
        $test.css('color', 'green');
    } else {
        if (submit) {
            $team.removeClass('table-success')
            $team.addClass('table-danger')
        }
        $test.removeClass('fas fa-check');
        $test.addClass('fas fa-times');
        $test.css('color', 'red');
    }
}

function initializeAssignmentClock() {
    clock = new Clock('440');
    clock.start();
}
