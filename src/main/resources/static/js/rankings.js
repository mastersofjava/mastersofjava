var stompClient = null;
var timerActive = true;
var stompControlClient = null;

var $circle = null;
var $assignmentClock = null;

$(document).ready(function(){
	connect();
	connectControl();
	initializeAssignmentClock();
})

function connect() {

	var socket = new SockJS('/rankings');
	stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		console.log('Connected to rankings');
		console.log('Subscribe to /rankings/queue/rankings');
		stompClient.subscribe('/queue/rankings', function(messageOutput) {
			refresh();
		});
	});
}

function connectControl() {
	var socket = new SockJS('/control');
	stompControlClient = Stomp.over(socket);
	stompControlClient.debug = null;
	stompControlClient.connect({}, function(frame) {
		console.log('Connected to control');
		console.log('Subscribe to /control/queue/time');
		stompControlClient.subscribe('/queue/time', function(taskTimeMessage) {
			var message = JSON.parse(taskTimeMessage.body);
			runClock10Sec(message.totalTime, message.remainingTime, 10);
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
    $assignmentClock = $('#assignment-clock');
    $circle = $('.circle_animation', $assignmentClock);
    var running = $assignmentClock.attr('running');
    if (running == 'true') {
	    var solutiontime = $assignmentClock.attr('data-time');
		var timeleft = $assignmentClock.attr('data-time-left');
		var elapsed = solutiontime - timeleft;
	    // run once
		var remainder = elapsed % 10;
		if (remainder > 1) {
			var period = 10 - remainder;
			console.log(period);
			// subtract 1 for delay
			runClock10Sec(solutiontime, timeleft - 1, period - 1);				
		}
    }
}

function runClock10Sec(solutiontime, timeleft, period){
	var i = 0;
	var elapsed = solutiontime - timeleft;
    var clock = setInterval(function () {
        if (i === period) {
            clearInterval(clock);
            i = 0;
            return;
        } else {
            renderTime(elapsed, solutiontime);
        }
        elapsed++;
        i++
    }, 1000);		
}

function renderTime(elapsed, solutiontime) {
	var initialOffset = '440';
    var remaining = solutiontime - elapsed - 1;
    if (remaining >= 0) {
      var minutes = Math.floor(remaining / 60);
      var seconds = ("0" + remaining % 60).slice(-2);
      $('h2', $assignmentClock).text(minutes + ":" + seconds);
      $circle.css('stroke-dashoffset', initialOffset - ((elapsed + 1) * (initialOffset / solutiontime)));
      var fraction = elapsed / solutiontime;
      if (fraction > 0.5) {
        if (fraction > 0.8) {
          $circle.css('stroke', 'red');
        } else {
          $circle.css('stroke', 'orange');
        }
      }
    }
}
