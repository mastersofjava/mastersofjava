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
			stompClient.subscribe('/user/queue/feedback', function(messageOutput) {
				console.log("user feedback")
				showOutput(messageOutput.body);
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
		    var time = $assignmentClock.attr('data-time');
		    var timeleft = $assignmentClock.attr('data-time-left');
	    	startClock(time, timeleft);
	    }
	}
	
	function startClock(time, timeleft){
	    var solutiontime = time;
	    var elapsed = solutiontime - timeleft;
	    /* Need initial run as interval hasn't yet occured... */
	    var initialOffset = '440';
	    $circle.css('stroke-dashoffset', initialOffset - (initialOffset / solutiontime));

	    interval = setInterval(function () {
	        if (elapsed === solutiontime) {
	            clearInterval(interval);
	            return;
	        } else {
	            renderTime(elapsed, solutiontime);
	        }
	        elapsed++;
	    }, 1000);		
	}

	function renderTime(elapsed, solutiontime) {
		var initialOffset = '440';
        var remaining = solutiontime - elapsed - 1;
        if (timerActive && remaining >= 0) {
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
	

