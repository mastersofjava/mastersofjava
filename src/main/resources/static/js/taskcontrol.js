var stompClient = null;
var clock = null;

$(document).ready(function () {
    connect();
    initializeAssignmentClock();
});


function connect() {
    var socket = new WebSocket(
        ((window.location.protocol === "https:") ? "wss://" : "ws://")
        + window.location.hostname
        + ':' + window.location.port
        + '/control/websocket');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        console.log('Connected to control');
        console.log('Subscribe to /user/queue/controlfeedback');
        stompClient.subscribe('/user/queue/controlfeedback', function (msg) {
            showOutput(msg.body);
        });
        stompClient.subscribe('/queue/controlfeedback', function(msg){
            let m = JSON.parse(msg.body);
            showAlert('['+m.assignment +'] ' + m.cause);
        });
        console.log('Subscribe to /control/queue/time');
        stompClient.subscribe('/queue/time', function (taskTimeMessage) {
            var message = JSON.parse(taskTimeMessage.body);
            if (clock) {
                clock.sync(message.remainingTime, message.totalTime);
            }
        });
        console.log('subscribe to /control/queue/start');
        stompClient.subscribe('/queue/start', function (msg) {
            window.setTimeout(function () {
                window.location.reload();
            }, 1000);
        });
        console.log('subscribe to /control/queue/stop');
        stompClient.subscribe('/queue/stop', function (msg) {
            if (clock) {
                clock.stop();
            }
        });
    });
}

function startTask() {
    var taskname = $("input[name='assignment']:checked").val();
    if (!taskname) {
        showOutput("Select a task to start");
        return;
    }
    console.log(taskname);
    stompClient.send("/app/control/starttask", {}, JSON.stringify({
        'taskName': taskname
    }));

    var tasktime = $("input[name='assignment']:checked").attr('time');
    $assignmentClock = $('#assignment-clock');
    $assignmentClock.attr('data-time', tasktime);
    $assignmentClock.attr('data-time-left', tasktime);
}

function stopTask() {
    stompClient.send("/app/control/stoptask", {}, {});
}

function clearAssignment() {
    stompClient.send("/app/control/clearCurrentAssignment", {}, {});
}

function scanAssignments() {
    stompClient.send("/app/control/scanAssignments", {}, {});
}

function showOutput(messageOutput) {
    let response = $('#response');
    response.empty();
    let p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(messageOutput));
    response.append(p);
}

function showAlert(txt) {
    let alert = $('#alert');
    alert.empty();
    alert.append('<div class="alert alert-danger alert-dismissible fade show" role="alert">' +
        '<span>'+txt+'</span>' +
        '<button type="button" class="close" data-dismiss="alert" aria-label="Close"> <span aria-hidden="true">&times;</span></button>' +
        '</div>');
}

function initializeAssignmentClock() {
    clock = new Clock('440');
    clock.start();
}

