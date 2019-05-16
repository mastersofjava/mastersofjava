var stompClient = null;
var stompControlClient = null;
var clock = null;

$(document).ready(function(){
	connect();
	connectControl();
	initializeAssignmentClock();
	initPopovers();
})

function connect() {

	var socket = new WebSocket(
    		((window.location.protocol === "https:") ? "wss://" : "ws://") 
    		+ window.location.hostname 
    		+ ':' + window.location.port 
    		+ '/rankings/websocket');
	stompClient = Stomp.over(socket);
	stompClient.debug = null;
	stompClient.connect({}, function(frame) {
		console.log('Connected to rankings');
		console.log('Subscribe to /rankings/queue/rankings');
		stompClient.subscribe('/queue/rankings', function(messageOutput) {
            window.location.reload();
		});
	});
}

function connectControl() {
	var socket = new WebSocket(
    		((window.location.protocol === "https:") ? "wss://" : "ws://") 
    		+ window.location.hostname 
    		+ ':' + window.location.port 
    		+ '/control/websocket');
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
        console.log('subscribe to /control/queue/start');
        stompControlClient.subscribe('/queue/start', function (msg) {
            window.location.reload();
        });
        console.log('subscribe to /control/queue/stop');
        stompControlClient.subscribe('/queue/stop', function (msg) {
            if( clock ) {
                clock.stop();
            }
        });
	});
}

function disconnect() {
	if (stompClient != null) {
		stompClient.disconnect();
	}
}

function initializeAssignmentClock() {
	clock = new Clock('943');
	clock.start();
}

function initPopovers() {
    $('[data-toggle="popover"]').popover();
    function html( json ) {
        var val = JSON.parse(json);
        var txt = '<table>';
        $.each(val.scores,function() {
            txt += '<tr><td>'+this.name+':</td><td>'+this.score+'</td></tr>';
        });
        txt += '</table>';
        return txt;
    }

    $('[data-score-popup]').each(function(){
        var $popup = $(this);
        $popup.popover({
            container: 'body',
            content: html($popup.attr('data-score-popup')),
            html: true,
            placement: 'top',
            title: 'Individual Assignment Scores',
            trigger: 'hover'
        });
    })



}