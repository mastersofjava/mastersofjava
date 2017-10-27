var stompClient = null;
var timerActive = true;

$(document).ready(function(){
	connect();
	initializeAssignmentClock();
})

function connect() {

	var socket = new SockJS('/rankings');
	stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		console.log('Connected to rankings');
		stompClient.subscribe('/queue/rankings', function(messageOutput) {
			refresh();
		});
	});
}

function disconnect() {
	if (stompClient != null) {
		stompClient.disconnect();
	}
}

function refresh(){
	console.log("Refreshing");
	$('#table').load(document.URL +  ' #table');
}

function initializeAssignmentClock() {

    var $assignmentClock = $('#assignment-clock');
    var $circle = $('.circle_animation', $assignmentClock);
    var time = $assignmentClock.attr('data-time');
    var initialOffset = '943';  //(2*pi*r)
    var t = time - $assignmentClock.attr('data-time-left');

    /* Need initial run as interval hasn't yet occured... */
    $circle.css('stroke-dashoffset', initialOffset - (initialOffset / time));

    function renderTime(i) {
        var remaining = time - i - 1;
        if (timerActive && remaining >= 0) {
            var minutes = Math.floor(remaining / 60);
            var seconds = ("0" + remaining % 60).slice(-2);

            $('h2', $assignmentClock).text(minutes + ":" + seconds);
            $circle.css('stroke-dashoffset', initialOffset - ((i + 1) * (initialOffset / time)));

            var fraction = i / time;
            if (fraction > 0.5) {
                if (fraction > 0.8) {
                    $circle.css('stroke', 'red');
                } else {
                    $circle.css('stroke', 'orange');
                }
            }
        }
    }

    var interval = setInterval(function () {
        if (t === time) {
            clearInterval(interval);
            return;
        } else {
            renderTime(t);
        }


        t++;
    }, 1000);
}