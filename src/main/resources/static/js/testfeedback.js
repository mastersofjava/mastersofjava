function init() {
	connect();
	connectTaskTime();
	connectControl();
}


function connect() {

	var socket = new SockJS('/feedback');
	var stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		console.log('Connected to rankings');
		stompClient.subscribe('/queue/testfeedback', function(messageOutput) {
			process(JSON.parse(messageOutput.body));
		});
	});
}

function connectControl() {

	var socket = new SockJS('/control');
	var stompClientControl = Stomp.over(socket);
	stompClientControl.debug = null;
	stompClientControl.connect({}, function(frame) {
		console.log('Connected to /control/queue/start');
		stompClientControl.subscribe('/queue/start', function(messageOutput) {
			console.log("/queue/start")
			window.location.reload();
		});

	});
}

function process(testfeedback){
	var id = testfeedback.team + '-' + testfeedback.test;
	var elem = $('#' + id);
	if (testfeedback.success) {
		elem.text('V');
		elem.css( "background-color", "green" );	
	} else {
		elem.text('X');
		elem.css( "background-color", "red" );
	}
}

function connectTaskTime() {

	var socket = new SockJS('/control');
	var stompClientTaskTime = Stomp.over(socket);
	stompClientTaskTime.debug = null;
	stompClientTaskTime.connect({}, function(frame) {

		console.log('Connected to /control/queue/time');
		stompClientTaskTime.subscribe('/queue/time', function(taskTimeMessage) {
			var message = JSON.parse(taskTimeMessage.body);
			var p = document.getElementById('tasktime');
			var date = new Date(null);
			date.setSeconds(message.remainingTime); // specify value for SECONDS here
			var result = date.toISOString().substr(11, 8);
			p.innerHTML = result;
		});

	});
}	
