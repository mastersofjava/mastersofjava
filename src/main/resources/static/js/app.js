var stomp = null;
var editors = [];
var timerActive = true;

$(document).ready(function() {
	connectFeedback();
	connectControl();
	connectButtons();
	initializeAssignmentClock();
	initializeCodeMirrors();
});

function connectFeedback() {
	var socket = new SockJS('/submit');
	var stompTestFeedbackClient = Stomp.over(socket);
	stompTestFeedbackClient.debug = null;
	stompTestFeedbackClient.connect({}, function(frame) {
		stompTestFeedbackClient.subscribe('/user/queue/feedback',
				function(msg) {
					var message = JSON.parse(msg.body);
					if (!message.submit) {
						appendOutput(message.text);
						updateOutputHeaderColor(message.success);
					} else {
						updateAlertContainerWithScore(message);
					}
				});
		stompTestFeedbackClient.subscribe('/user/queue/compilefeedback',
				function(msg) {
					var message = JSON.parse(msg.body);
					if (!message.submit) {
						appendOutput(message.text);
						updateOutputHeaderColor(message.success);
					}
				});
	});
}

function connectControl() {
	var socket = new SockJS('/control');
	stomp = Stomp.over(socket);
	stomp.debug = null;
	stomp.connect({}, function(frame) {
		$('#status').append('<span>Connected</span>');
		stomp.subscribe('/queue/start', function(msg) {
			window.location.reload();
		});
		stomp.subscribe("/queue/stop", function(msg) {
			disable();
		})
	});
}

function connectButtons() {
	$('#compile').click(function(e) {
		compile();
		e.preventDefault();
	});
	$('#test').click(function(e) {
		$('#test-modal').modal('hide');
		test();
		e.preventDefault();
	});
	$('#submit').click(function(e) {
		$('#btn-open-submit').attr('disabled', 'disabled');
		$('#confirm-submit-modal').modal('hide');
		timerActive = false;
		submit();
		e.preventDefault();
	});
}

function initializeCodeMirrors() {
	$('textarea[data-cm]').each(
			function(idx) {
				var type = $(this).attr('data-cm-file-type');
				var cm = CodeMirror.fromTextArea(this, {
					lineNumbers : true,
					mode : type === 'TASK' ? 'text/plain' : "text/x-java",
					matchBrackets : true,
					readOnly : $(this).attr('data-cm-readonly') === 'true'
				});
				editors.push({
					'cm' : cm,
					'readonly' : cm.isReadOnly(),
					'filename' : $(this).attr('data-cm-filename'),
					'textarea' : this
				});

				$('a[id="' + $(this).attr('data-cm') + '"]').on('shown.bs.tab',
						function(e) {
							cm.refresh();
						});

				var $wrapper = $(cm.getWrapperElement());
				$wrapper.resizable({
					resize : function() {
						cm.setSize($(this).width(), $(this).height());
						cm.refresh();
					}
				});

				var pos = $('#tabs .tab-content').position();
				var height = window.innerHeight - pos.top - 80;
				$wrapper.css('height', height + 'px');
				cm.refresh();
			});
}

function initializeAssignmentClock() {
	var $assignmentClock = $('#assignment-clock');
	var $circle = $('.circle_animation', $assignmentClock);
	var time = $assignmentClock.attr('data-time');
	var initialOffset = '440';
	var timeleft = $assignmentClock.attr('data-time-left');
	var finished = ($('#content').attr('finished') == 'true');
	if (finished) {
		timeleft = $('#content').attr('submittime');	
	}
	var t = time - timeleft;
	// make sure it is rendered at least once in case this team has finished
	renderTime(t);

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

function resetOutput() {
	$('#output').removeClass('failure', 'success');
	$('#output-content').empty();
}

function updateOutputHeaderColor(success) {
	var $output = $('#output');
	if (success && !$output.hasClass('failure')) {
		$output.removeClass('failure');
		$output.addClass('success');
	} else {
		$output.removeClass('success');
		$output.addClass('failure');
	}
}

function updateAlertContainerWithScore(message) {
	console.log(message);
	if (message.success == true) {
		$('#alert-container')
				.empty()
				.append(
						'<div class="alert alert-success p-4" role="alert"><h4 class="alert-heading">Assignment Tests Successful</h4><h1>:-)</h1>'
								+ '<p>your score is</p><strong>'
								+ message.score + '</strong></div>');
	} else {
		$('#alert-container')
				.empty()
				.append(
						'<div class="alert alert-danger p-4" role="alert"><h4 class="alert-heading">Assignment Tests Failed</h4><h1>:-(</h1></div>');
	}
}

function appendOutput(txt) {
	$('#output-content').append('<pre>' + escape(txt) + '</pre>');
	$('#content').tab('show');
}

function escape(txt) {
	var htmlEscapes = {
		'&' : '&amp;',
		'<' : '&lt;',
		'>' : '&gt;',
		'"' : '&quot;',
		"'" : '&#x27;',
		'/' : '&#x2F;'
	};

	// Regex containing the keys listed immediately above.
	var htmlEscaper = /[&<>"'\/]/g;

	// Escape a string for HTML interpolation.
	return ('' + txt).replace(htmlEscaper, function(match) {
		return htmlEscapes[match];
	});
}

function compile() {
	resetOutput();
	stomp.send("/app/submit/compile", {}, JSON.stringify({
		'sources' : getContent()
	}));
}

function test() {
	resetOutput();
	var tests = $("#test-modal input:checkbox:checked").map(function() {
		return $(this).val();
	}).get();

	stomp.send("/app/submit/test", {}, JSON.stringify({
		'sources' : getContent(),
		'tests' : tests
	}));
}

function disable() {
	console.log("disable");
	$('#compile').attr('disabled', 'disabled');
	$('#test').attr('disabled', 'disabled');
	$('#show-tests').attr('disabled', 'disabled');
	$('#btn-open-submit').attr('disabled', 'disabled');
	$.each(editors, function(idx, val) {
		if (!val.readonly) {
			val.cm.setOption("readOnly", true);
		}
	});
}

function getContent() {
	var editables = [];
	$.each(editors, function(idx, val) {
		if (!val.readonly) {
			var file = {
				filename : val.filename,
				content : val.cm.getValue()
			};
			editables.push(file);
		}
	});
	return editables;
}

function submit() {
	disable();
	resetOutput();
	stomp.send("/app/submit/submit", {}, JSON.stringify({
		'sources' : getContent()
	}));
	showSubmitDetails();
}

function showSubmitDetails() {
	$('#alert-container')
			.empty()
			.append(
					'<div class="alert alert-info p-4" role="alert"><h4 class="alert-heading">Assignment Submitted</h4><p>Well done! You have submitted the assignment for final review. '
							+ 'Chill out and wait a few seconds until the results are displayed.</p></div>');
}
