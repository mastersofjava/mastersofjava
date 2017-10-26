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
        stompTestFeedbackClient.subscribe('/user/queue/feedback', function (msg) {
            var message = JSON.parse(msg.body);
            if (!message.submit) {
                appendOutput(message.text);
                updateOutputHeaderColor(message.success);
            }
        });
        stompTestFeedbackClient.subscribe('/user/queue/compilefeedback', function (msg) {
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
    stomp.connect({}, function (frame) {
        $('#status').append('<span>Connected</span>');
        stomp.subscribe('/queue/start', function (msg) {
            window.location.reload();
        });
        stomp.subscribe("/queue/stop", function (msg) {
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

    var $assignmentClock = $('#assignment-clock');
    var $circle = $('.circle_animation', $assignmentClock);
    var time = $assignmentClock.attr('data-time');
    var initialOffset = '440';
    var t = time - $assignmentClock.attr('data-time-left');


    /* Need initial run as interval hasn't yet occured... */
    $circle.css('stroke-dashoffset', initialOffset - (initialOffset / time));

    function renderTime(i) {
        var remaining = time - i - 1;
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
    $('#output-content').append('<pre>' + escape(txt) + '</pre>');
    $('#content').tab('show');
}

function escape( txt ) {
    var htmlEscapes = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#x27;',
        '/': '&#x2F;'
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
    showSubmitDetails();
}

function showSubmitDetails() {
    $('#alert-container')
        .empty()
        .append('<div class="alert alert-success p-4" role="alert"><h4 class="alert-heading">Assignment Submitted</h4><p>Well done!. You have submitted the assignment for final review. ' +
            'Chill out and wait until the next assignment starts.</p></div>');
}
