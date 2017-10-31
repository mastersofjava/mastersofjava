var stompClient = null;
var timerActive = true;
var interval = null;

var $circle = null;
var $assignmentClock = null;

$(document).ready(function () {
	connect();
	initializeAssignmentClock();
});

	
	function connect() {
		var socket = new SockJS('/control');
		stompClient = Stomp.over(socket);
		stompClient.debug = null;
		stompClient.connect({}, function(frame) {
			console.log('Connected to control');
			console.log('Subscribe to /user/queue/controlfeedback');
			stompClient.subscribe('/user/queue/controlfeedback', function(messageOutput) {
				console.log("controlfeedback")
				showOutput(messageOutput.body);
			});
			console.log('Subscribe to /control/queue/time');
			stompClient.subscribe('/queue/time', function(taskTimeMessage) {
				var message = JSON.parse(taskTimeMessage.body);
				runClock10Sec(message.totalTime, message.remainingTime, 10);
			});
		});
	}
	
	function startTask() {
		var taskname = $("input[name='assignment']:checked").val();
		console.log(taskname);
		stompClient.send("/app/control/starttask", {}, JSON.stringify({
			'taskName' : taskname
		}));
		
		var tasktime = $("input[name='assignment']:checked").attr('time');
		$assignmentClock = $('#assignment-clock');
	    $assignmentClock.attr('data-time', tasktime);
	    $assignmentClock.attr('data-time-left', tasktime);
	    startClock(tasktime, tasktime);
	    runClock10Sec(tasktime, tasktime, 10)
	}

	function stopTask() {
		var taskname = $("input[name='assignment']:checked").val();
		console.log(taskname);
		stompClient.send("/app/control/stoptask", {}, JSON.stringify({
			'taskName' : taskname
		}));
		// stop the clock
		clearInterval(interval);
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
	

