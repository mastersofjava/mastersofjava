function init() {
	connectFeedback();
	connectControl();
}


function connectFeedback() {

	var socket = new SockJS('/feedback');
	var stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		console.log('Connected to /feedback');
		console.log('Subscribe to /queue/feedbackpage')
		stompClient.subscribe('/queue/feedbackpage', function(messageOutput) {
			process(JSON.parse(messageOutput.body));
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

function process(testfeedback){
	var id = testfeedback.team + '-' + testfeedback.test;
	var elem = $('#' + id);
	$('#' + testfeedback.team).removeClass('table-active').addClass('table-primary');
	if (testfeedback.success) {
		elem.text('V');
		elem.removeClass('table-active');
		elem.removeClass('table-danger');
		elem.addClass('table-success');
	} else {
		elem.text('X');
		elem.removeClass('table-active');
		elem.removeClass('table-success');
		elem.addClass('table-danger');
	}
}

