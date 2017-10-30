function init() {
	connectFeedback();
	connectControl();
	initializeAssignmentClock();
}

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
  var $assignmentClock = $('#assignment-clock');
  var $circle = $('.circle_animation', $assignmentClock);
  // var time = $assignmentClock.attr('data-time');
  var time = 40;
  var initialOffset = '440';
  // var timeleft = $assignmentClock.attr('data-time-left');
  var timeleft = 30;
  var finished = ($('#content').attr('finished') == 'true');
  if (finished) {
    timeleft = $('#content').attr('submittime');
  }
  var t = time - timeleft;
  // make sure it is rendered at least once in case this team has finished
  renderTime(t);

  function renderTime(i) {
    var remaining = time - i - 1;
    if (remaining >= 0) {
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

      if (remaining === 0) {
        rejectRemainingTeams();
      }
    }

  }

  var interval = setInterval(function() {
    if (finished || t === time) {
      clearInterval(interval);
      return;
    } else {
      renderTime(t);
    }

    t++;
  }, 1000);
}

function rejectRemainingTeams() {
  $('tr').not('.table-success').not(':contains("Team")').addClass('table-danger');
}

