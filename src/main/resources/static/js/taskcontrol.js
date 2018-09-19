var stompClient = null;
var clock = null;

$(document).ready(function () {
    connect();
    initializeAssignmentClock();
});


function connect() {
    var socket = new SockJS('/control');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        console.log('Connected to control');
        console.log('Subscribe to /user/queue/controlfeedback');
        stompClient.subscribe('/user/queue/controlfeedback', function (messageOutput) {
            showOutput(messageOutput.body);
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
            window.location.reload();
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

function cloneAssignmentsRepo() {
    var repo = $("input[name='repo']:checked").val();
    stompClient.send("/app/control/cloneAssignmentsRepo", {}, repo);
}

function showOutput(messageOutput) {
    var response = document.getElementById('response');
    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(messageOutput));
    response.appendChild(p);
}

function initializeAssignmentClock() {
    clock = new Clock('440');
    clock.start();
}

