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

function startTesting(msg) {
    var uuid = msg.uuid;
    var $team = $('tr[data-team='+uuid+']');

    $team.removeClass('table-danger table-success');
    $team.addClass('table-info')
    setTimeout(function(){
        $team.removeClass('table-info')
    },500);
    $('span',$team )
        .removeClass('fa-check fa-times fas');

}

function process(message) {
    var team = message.uuid;
    var test = message.test;
    var submit = message.messageType === 'SUBMIT';
    var id = team + '-' + test;
    var $team = $('tr[data-team='+team+']');
    var $test = $('span[data-test=' + id +' ]');
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
