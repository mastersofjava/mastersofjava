
$(document).ready(function () {
    connect();
    initializeAssignmentClock();

    $('[data-toggle="popover"]').popover();
});


function connect() {
    var socket = new WebSocket(
        ((window.location.protocol === "https:") ? "wss://" : "ws://")
        + window.location.hostname
        + ':' + window.location.port
        + '/control/websocket');
    window.stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        console.log('Connected to control');
        console.log('Subscribe to /user/queue/controlfeedback');
        stompClient.subscribe('/user/queue/controlfeedback', function (msg) {
            console.log("/user/queue/controlfeedback ");
            showAlert(msg.body);
            if (msg.body.indexOf('reload')!=0) {
                reloadPage();
            }
        });
        stompClient.subscribe('/queue/controlfeedback', function(msg){
            console.log("/queue/controlfeedback ");
            console.log(msg);
            var m = JSON.parse(msg.body);
            showAlert('['+m.assignment +'] ' + m.cause);
        });
        console.log('Subscribe to /control/queue/time');
        stompClient.subscribe('/queue/time', function (taskTimeMessage) {
            console.log("/queue/time");
            var message = JSON.parse(taskTimeMessage.body);
            if (clock) {
                clock.sync(message.remainingTime, message.totalTime);
            }
        });
        console.log('subscribe to /control/queue/start');
        stompClient.subscribe('/queue/start', function (msg) {
            console.log("/queue/start");
            reloadPage();
        });
        console.log('subscribe to /control/queue/stop');
        stompClient.subscribe('/queue/stop', function (msg) {
            console.log("/queue/stop");
            if (clock) {
                clock.stop();
            }
        });
    });
}

function reloadPage() {
    window.setTimeout(function () {
        window.location.reload();
    }, 1000);
}

function startTask() {
    $('#alert').empty();
    var taskname = $("input[name='assignment']:checked").val();
    if (!taskname) {
        showAlert("Select an assignment first, before starting");
        return;
    }
    console.log('taskname ' + taskname);
    stompClient.send("/app/control/starttask", {}, JSON.stringify({
        'taskName': taskname
    }));

    var tasktime = $("input[name='assignment']:checked").attr('time');

    console.log('tasktime ' + tasktime);
    $assignmentClock = $('#assignment-clock');
    $assignmentClock.attr('data-time', tasktime);
    $assignmentClock.attr('data-time-left', tasktime);
}

function stopTask() {
    $('#alert').empty();
    var taskname = $("input[name='assignment']:checked").val();
    if (!taskname) {
        showAlert("No assignment active");
        return;
    }
    stompClient.send("/app/control/stoptask", {}, {});
}

function clearAssignments() {
    $('#alert').empty();
    showAlert("competition has been restarted");
    stompClient.send("/app/control/clearCurrentAssignment", {}, {});
}

function scanAssignments() {
    stompClient.send("/app/control/scanAssignments", {}, {});
}

function showOutput(messageOutput) {
    var response = $('#response');
    response.empty();
    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(messageOutput));
    response.append(p);
}

function showAlert(txt) {
    var alert = $('#alert');
    alert.empty();
    alert.append('<div class="alert alert-danger alert-dismissible fade show" role="alert">' +
        '<span>'+txt+'</span>' +
        '<button type="button" class="close" data-dismiss="alert" aria-label="Close"> <span aria-hidden="true">&times;</span></button>' +
        '</div>');
}

function initializeAssignmentClock() {
    window.clock = new Clock('440');
    clock.start();
}

