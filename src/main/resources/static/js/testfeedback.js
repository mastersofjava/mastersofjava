var clock = null;

$(document).ready(function () {
    connectFeedback();
    connectControl();
    initializeAssignmentClock();
});

function connectFeedback() {

    var socket = new SockJS('/feedback');
    var stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/queue/feedbackpage', function (messageOutput) {
            var message = JSON.parse(messageOutput.body)
            process(message);
        });
    });
}

function connectControl() {
    var socket = new SockJS('/control');
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

function process(message) {
    var team = message.team;
    var test = message.test;
    var submit = message.messageType === 'SUBMIT';
    var id = team + '-' + test;

    var testTd = $('#' + id);
    var teamTd = $('td').filter(function () {
        return $(this).text() === team
    });
    var row = teamTd.closest($('tr'));
    if (message.success) {
        if (submit) {
            row.removeClass('table-danger')
            row.addClass('table-success')
        }
        testTd.removeClass('fa fa-close');
        testTd.addClass('fa fa-check');
        testTd.css('color', 'green');
    } else {
        if (submit) {
            row.removeClass('table-success')
            row.addClass('table-danger')
        }
        testTd.removeClass('fa fa-check');
        testTd.addClass('fa fa-close');
        testTd.css('color', 'red');
    }
}

function initializeAssignmentClock() {
    clock = new Clock('440');
    clock.start();
}
