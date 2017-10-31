var stompClient = null;
var stompControlClient = null;
var clock = null;

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
			if( clock ) {
                clock.sync(message.remainingTime,message.totalTime);
			}
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
	clock = new Clock('943');
	clock.start();
}