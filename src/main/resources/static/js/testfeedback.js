function init() {
	connect();
	connectTaskTime();
}


function connect() {

	var socket = new SockJS('/feedback');
	var stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		console.log('Connected to rankings');
		stompClient.subscribe('/queue/testfeedback', function(messageOutput) {
			refresh(JSON.parse(messageOutput.body));
		});
	});
}
function disconnect() {

	if (stompClient != null) {
		stompClient.disconnect();
	}

}

function refresh(testfeedback){
	console.log("Refreshing");
	var id = testfeedback.team + '-' + testfeedback.test;
	var elem = $('#' + id);
	if (testfeedback.success) {
		elem.css( "border", "3px solid green" );	
	} else {
		elem.css( "border", "3px solid red" );
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
			p.innerHTML = message.remainingTime;
		});

	});
}	
