
var $circle = null;
var $assignmentClock = null;

$(document).ready(function(){
	connectFeedback();
	connectControl();
	initializeAssignmentClock();
})
function connectFeedback() {

	var socket = new SockJS('/feedback');
	var stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		stompClient.subscribe('/queue/feedbackpage', function(messageOutput) {
		  var message = JSON.parse(messageOutput.body)
			process(message);
		});
	});
}

function connectControl() {
	var socket = new SockJS('/control');
	var stompClientControl = Stomp.over(socket);
	stompClientControl.debug = null;
	stompClientControl.connect({}, function(frame) {
		console.log('Connected to /control');
		console.log('Subscribe to /queue/start');
		stompClientControl.subscribe('/queue/start', function(messageOutput) {
			console.log("/queue/start")
			window.location.reload();
		});
		console.log('Subscribe to /control/queue/time');
		stompClientControl.subscribe('/queue/time', function(taskTimeMessage) {
			var message = JSON.parse(taskTimeMessage.body);
			runClock10Sec(message.totalTime, message.remainingTime, 10);
		});

	});
}

function process(message){
  var team = message.team;
  var test = message.test;
	var id = team + '-' + test;

	var testTd = $('#' + id);
  var teamTd = $('td').filter(function () { return $(this).text() === team });
	var row = teamTd.closest($('tr'));
	if (message.success) {
	  if (message.submit) {
      row.removeClass('table-danger')
      row.addClass('table-success')
    }
	  testTd.removeClass('fa fa-close');
	  testTd.addClass('fa fa-check');
	  testTd.css('color', 'green');
	} else {
	  if (message.submit) {
      row.removeClass('table-success')
      row.addClass('table-danger')
    }
    testTd.removeClass('fa fa-check');
    testTd.addClass('fa fa-close');
    testTd.css('color', 'red');
	}
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
      if (remaining === 0) {
          rejectRemainingTeams();
      }
    }
}

function rejectRemainingTeams() {
  $('tr').not('.table-success').not(':contains("Team")').addClass('table-danger');
}

