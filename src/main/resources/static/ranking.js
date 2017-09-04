var stompClient = null;
function connect() {

	var socket = new SockJS('/rankings');
	stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		console.log('Connected to rankings');
		stompClient.subscribe('/rankings', function(messageOutput) {
			refresh();
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
