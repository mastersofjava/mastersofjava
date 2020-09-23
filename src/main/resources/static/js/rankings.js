var stompClient = null;
var stompControlClient = null;
var clock = null;

$(document).ready(function () {
    connect();
    connectControl();
    initializeAssignmentClock();
    initPopovers();
});

function connect() {

    stompClient = new StompJs.Client({
        brokerURL: ((window.location.protocol === "https:") ? "wss://" : "ws://")
            + window.location.hostname
            + ':' + window.location.port
            + "/rankings/websocket",
     //   debug: function (str) {
   //         console.log(str);
     //   },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    stompClient.onConnect = function (frame) {
        console.log('Connected to rankings');
        console.log('Subscribe to /rankings/queue/rankings');
        stompClient.subscribe('/queue/rankings',
            function (msg) {
                window.location.reload();
                msg.ack();
            },
            {ack: 'client'});
    };

    stompClient.activate();
}

function connectControl() {
    stompControlClient = new StompJs.Client({
        brokerURL: ((window.location.protocol === "https:") ? "wss://" : "ws://")
            + window.location.hostname
            + ':' + window.location.port
            + "/control/websocket",
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    stompControlClient.onConnect = function(frame) {
        console.log('Connected to control');
        console.log('Subscribe to /control/queue/time');
        stompControlClient.subscribe('/queue/time',
            function (msg) {
                var message = JSON.parse(msg.body);
                if (clock) {
                    clock.sync(message.remainingTime, message.totalTime);
                }
                msg.ack();
            },
            {ack: 'client'});
        console.log('subscribe to /control/queue/start');
        stompControlClient.subscribe('/queue/start',
            function (msg) {
                window.location.reload();
                msg.ack();
            },
            {ack: 'client'});
        console.log('subscribe to /control/queue/stop');
        stompControlClient.subscribe('/queue/stop',
            function (msg) {
                if (clock) {
                    clock.stop();
                }
                msg.ack();
            },
            {ack: 'client'});
    };

    stompControlClient.activate();
}

function disconnect() {
    if (stompClient != null) {
        stompClient.deactivate();
    }
    if( stompControlClient != null ) {
        stompControlClient.deactivate();
    }
}

function initializeAssignmentClock() {
    clock = new Clock('943');
    clock.start();
}

function initPopovers() {
    $('[data-toggle="popover"]').popover();

    function html(json) {
        var val = JSON.parse(json);
        var txt = '<table>';
        $.each(val.scores, function () {
            txt += '<tr><td>' + this.name + ':</td><td>' + this.score + '</td></tr>';
        });
        txt += '</table>';
        return txt;
    }

    $('[data-score-popup]').each(function () {
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
