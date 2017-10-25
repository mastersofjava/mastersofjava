var stompClientControl = null;

$(document).ready(function(){
    connectTestFeedback();
    connectCompileFeedback();
    connectControl();
    connectStop();
    connectButtons();
    initializeAssignmentClock();
    initializeCodeMirrors();
});

function connectTestFeedback() {

    var socket = new SockJS('/submit');
    var stompTestFeedbackClient = Stomp.over(socket);
    stompTestFeedbackClient.debug = null;
    stompTestFeedbackClient.connect({}, function (frame) {
        console.log('Connected feedback');
        stompTestFeedbackClient.subscribe('/user/queue/feedback', function (messageOutput) {
            console.log("test feedback");
            var message = JSON.parse(messageOutput.body);
            if (!message.submit) {
                var response = document.getElementById(message.test);
                response.innerHTML = "<pre>" + message.text + "</pre>";
                if (message.success) {
                    $('#' + message.test + '-li').find("a").css("color", "green");
                } else {
                    $('#' + message.test + '-li').find("a").css("color", "red");
                }
            }
        });
    });
}

function connectCompileFeedback() {

    var socket = new SockJS('/submit');
    var stompCompileFeedbacClient = Stomp.over(socket);
    stompCompileFeedbacClient.debug = null;
    stompCompileFeedbacClient.connect({}, function (frame) {
        console.log('Connected compilefeedback');
        stompCompileFeedbacClient.subscribe('/user/queue/compilefeedback', function (messageOutput) {
            console.log("compilefeedback");
            var message = JSON.parse(messageOutput.body);
            var $response = $('#output-content');
            $response.empty();
            $response.append('<pre>' + message.text + '</pre>');
            if (message.success) {
                $('#output').css("color", "green");
            } else {
                $('#output').css("color", "red");
            }
        });

    });
}

function connectControl() {
    var socket = new SockJS('/control');
    stompClientControl = Stomp.over(socket);
    //stompClientControl.debug = null;
    stompClientControl.connect({}, function (frame) {
        console.log('Connected to /control/queue/start');
        stompClientControl.subscribe('/queue/start', function (messageOutput) {
            console.log("/queue/start")
            window.location.reload();
        });

    });
}

function connectStop() {
    var socket = new SockJS('/control');
    var stompClientStop = Stomp.over(socket);
    stompClientStop.debug = null;
    stompClientStop.connect({}, function (frame) {
        console.log('Connected to /control/queue/stop');
        stompClientStop.subscribe('/queue/stop', function (taskTimeMessage) {
            var message = JSON.parse(taskTimeMessage.body);
            console.log("/queue/stop")
            disable();
        });
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
        submit();
        e.preventDefault();
    });
}

var editors = [];

function initializeCodeMirrors() {
    $('textarea[data-cm]').each(function(idx){
        var cm = CodeMirror.fromTextArea(this, {
            lineNumbers: true,
            mode: "text/x-java",
            matchBrackets: true,
            readOnly: $(this).attr('data-cm-readonly')
        });

        editors.push({'cm' : cm, 'readonly': cm.readOnly, 'filename': $(this).attr('data-cm-filename'), 'textarea': this });

        $('a[id="'+$(this).attr('data-cm')+'"]').on('shown.bs.tab', function (e) {
            cm.refresh();
        });
    });
}

function initializeAssignmentClock() {

    var time = 30*60; // in seconds
    var initialOffset = '440';
    var t = 1;
    var $assignmentClock = $('#assignment-clock');

    /* Need initial run as interval hasn't yet occured... */
    $assignmentClock.css('stroke-dashoffset', initialOffset-(initialOffset / time));


    function renderTime(i) {
        var remaining = time - i - 1;
        var minutes = Math.floor(remaining/60);
        var seconds = ("0" + remaining%60).slice(-2);

        $('h2', $assignmentClock).text(minutes+":"+seconds);
        $assignmentClock.css('stroke-dashoffset', initialOffset-((i+1)*(initialOffset/time)));

        var fraction = i/time;
        if (fraction>0.5) {
            if (fraction>0.8) {
                $assignmentClock.css('stroke', 'red');
            } else {
                $assignmentClock.css('stroke', 'orange');
            }
        }
    }

    var interval = setInterval(function() {
        if (t === time) {
            clearInterval(interval);
            return;
        } else {
            renderTime(t);
        }


        t++;
    }, 1000);
}

function compile() {
    stompClientControl.send("/app/submit/compile", {}, JSON.stringify({
        'team': 'team1',
        'source': {} //getContent()
    }));
}

function test() {
    cleartests();
    var tests = $("input:checkbox:checked").map(function () {
        return $(this).val();
    }).get();

    stompClientControl.send("/app/submit/test", {}, JSON.stringify({
        'team': 'team1',
        'source': getContent(),
        'tests': tests
    }));
}

function cleartests() {
    //var curTab = $('.ui-state-active');
    $('#tabs .nav-link').css("color", "black");

}

function disable() {
    // make readonly
    // for (i = 0; i < filesArray.length; i++) {
    //     if (filesArray[i] != null) {
    //         console.log(filesArray[i]);
    //         filesArray[i].cmEditor.setOption("readOnly", true);
    //         console.log('#' + filesArray[i].name + '-tab');
    //         console.log($('#' + filesArray[i].name + '-tab .cm-s-default'));
    //         $('#' + filesArray[i].name + '-tab .cm-s-default').css("background-color", "grey");
    //     }
    // }
    // disable buttons

    $('#compile').attr('disabled', 'disabled');
    $('#test').attr('disabled', 'disabled');
    $('#show-tests').attr('disabled', 'disabled');
    $('#submit').attr('disabled', 'disabled');

}

function getContent() {
    var editables = [];
    $.each( editors, function(idx,val) {
        if( !val.readonly ) {
            var file = {filename: val.filename, content: val.cm.getValue()};
            editables.push(file);
        }
    } );
    console.log(editables);
    return editables;
}

function submit() {
    disable();
    stompClientControl.send("/app/submit/submit", {}, JSON.stringify({
        'team': 'team1',
        'source': getContent()
    }));
}
