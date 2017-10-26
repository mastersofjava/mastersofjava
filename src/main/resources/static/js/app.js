var stomp = null;
var editors = [];

$(document).ready(function () {
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
    stompTestFeedbackClient.connect({}, function (frame) {
        console.log('Connected to feedback channel.');
        stompTestFeedbackClient.subscribe('/user/queue/feedback', function (msg) {
            console.log("Received test feedback.");
            var message = JSON.parse(msg.body);
            if (!message.submit) {
                appendOutput(message.text);
                updateOutputHeaderColor(message.success);
            }
        });
        stompTestFeedbackClient.subscribe('/user/queue/compilefeedback', function (msg) {
            console.log("Received compiler feedback.");
            var message = JSON.parse(msg.body);
            appendOutput(message.text);
            updateOutputHeaderColor(message.success);
        });
    });
}

function connectControl() {
    var socket = new SockJS('/control');
    stomp = Stomp.over(socket);
    stomp.debug = null;
    stomp.connect({}, function (frame) {
        console.log('Connected to control channel.');
        stomp.subscribe('/queue/start', function (msg) {
            console.log("Received assignment start.", msg)
            window.location.reload();
        });
        stomp.subscribe("/queue/stop", function (msg) {
            console.log("Received assignment stop.", msg)
            disable();
        })
    });
}

function connectButtons() {
    $('#compile').click(function (e) {
        compile();
        e.preventDefault();
    });
    $('#test').click(function (e) {
        $('#test-modal').modal('hide');
        test();
        e.preventDefault();
    });
    $('#submit').click(function (e) {
        submit();
        e.preventDefault();
    });
}


function initializeCodeMirrors() {
    $('textarea[data-cm]').each(function (idx) {
        var cm = CodeMirror.fromTextArea(this, {
            lineNumbers: true,
            mode: "text/x-java",
            matchBrackets: true,
            readOnly: $(this).attr('data-cm-readonly') === 'true'
        });
        editors.push({
            'cm': cm,
            'readonly': cm.isReadOnly(),
            'filename': $(this).attr('data-cm-filename'),
            'textarea': this
        });

        $('a[id="' + $(this).attr('data-cm') + '"]').on('shown.bs.tab', function (e) {
            cm.refresh();
        });
    });
}

function initializeAssignmentClock() {

    var time = 30 * 60; // in seconds
    var initialOffset = '440';
    var t = 1;
    var $assignmentClock = $('#assignment-clock');

    /* Need initial run as interval hasn't yet occured... */
    $assignmentClock.css('stroke-dashoffset', initialOffset - (initialOffset / time));

    function renderTime(i) {
        var remaining = time - i - 1;
        var minutes = Math.floor(remaining / 60);
        var seconds = ("0" + remaining % 60).slice(-2);

        $('h2', $assignmentClock).text(minutes + ":" + seconds);
        $assignmentClock.css('stroke-dashoffset', initialOffset - ((i + 1) * (initialOffset / time)));

        var fraction = i / time;
        if (fraction > 0.5) {
            if (fraction > 0.8) {
                $assignmentClock.css('stroke', 'red');
            } else {
                $assignmentClock.css('stroke', 'orange');
            }
        }
    }

    var interval = setInterval(function () {
        if (t === time) {
            clearInterval(interval);
            return;
        } else {
            renderTime(t);
        }


        t++;
    }, 1000);
}

function resetOutput() {
    $('#output').removeClass('failure','success');
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

function appendOutput(txt) {
    $('#output-content').append('<pre>' + txt + '</pre>');
    $('#content').tab('show');
}

function compile() {
    resetOutput();
    stomp.send("/app/submit/compile", {}, JSON.stringify({
        'sources': getContent()
    }));
}

function test() {
    resetOutput();
    var tests = $("#test-modal input:checkbox:checked").map(function () {
        return $(this).val();
    }).get();

    stomp.send("/app/submit/test", {}, JSON.stringify({
        'sources': getContent(),
        'tests': tests
    }));
}

function disable() {
    $('#compile').attr('disabled', 'disabled');
    $('#test').attr('disabled', 'disabled');
    $('#show-tests').attr('disabled', 'disabled');
    $('#submit').attr('disabled', 'disabled');
}

function getContent() {
    var editables = [];
    $.each(editors, function (idx, val) {
        if (!val.readonly) {
            var file = {filename: val.filename, content: val.cm.getValue()};
            editables.push(file);
        }
    });
    return editables;
}

function submit() {
    disable();
    stomp.send("/app/submit/submit", {}, JSON.stringify({
        'sources': getContent()
    }));
}
