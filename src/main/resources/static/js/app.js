var stomp = null;
var editors = [];
var clock = null;
var activeAction = null;
var failed = false;

$(document).ready(function () {
    connectCompetition();
    connectButtons();

    initializeAssignmentClock();
    initializeCodeMirrors();
});

function connectCompetition() {
    var socket = new WebSocket(
        ((window.location.protocol === "https:") ? "wss://" : "ws://")
        + window.location.hostname
        + ':' + window.location.port
        + "/ws/competition/websocket");
    stomp = Stomp.over(socket);
    stomp.debug = null;
    stomp.connect({}, function (frame) {
        $('#status').append('<span>Connected</span>');
        stomp.subscribe('/user/queue/competition',
            function (data) {
                var msg = JSON.parse(data.body);
                console.log('received', msg);

                if ('TEST'==msg.messageType && msg.test) {
                    var colorStr = msg.success? 'lightgreen':'pink';
                    $('#tabLink_'+msg.test).css('background-color',colorStr);
                }

                if (userHandlers.hasOwnProperty(msg.messageType)) {
                    userHandlers[msg.messageType](msg);
                }
            });
        stomp.subscribe("/queue/competition",
            function (data) {
                var msg = JSON.parse(data.body);
                if (handlers.hasOwnProperty(msg.messageType)) {
                    handlers[msg.messageType](msg);
                }
            });
    });

    var userHandlers = {};
    userHandlers['COMPILE'] = function (msg) {
        enable();
        appendOutput(msg.message);
    };
    userHandlers['COMPILING_STARTED'] = function (msg) {
        updateOutputHeaderColorActionStarted();
    };
    userHandlers['COMPILING_ENDED'] = function (msg) {
        updateOutputHeaderColorActionEnded(msg.success);
    };
    userHandlers['TEST'] = function (msg) {
        enable();
        appendOutput(msg.test + ':\r\n' + msg.message);
    };
    userHandlers['TESTING_STARTED'] = function (msg) {
        updateOutputHeaderColorActionStarted();
    };
    userHandlers['TESTING_ENDED'] = function (msg) {
        updateOutputHeaderColorActionEnded(msg.success);
    };
    userHandlers['SUBMIT'] = function (msg) {
        if (!msg.success && msg.remainingSubmits > 0) {
            enable();
        } else {
            disable();
        }
        updateSubmits(msg.remainingSubmits);
        updateAlertContainerWithScore(msg);
    };

    var handlers = {};
    handlers['TIMER_SYNC'] = function (msg) {
        if (clock) {
            clock.sync(msg.remainingTime, msg.totalTime);
        }
    };
    handlers['START_ASSIGNMENT'] = function (msg) {
        window.setTimeout(function () {
            window.location.reload()
        }, 1000);
    };
    handlers['STOP_ASSIGNMENT'] = function (msg) {
        disable();
        if (clock) {
            clock.stop();
        }
    }
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
        resetTabColor();
        $('#confirm-submit-modal').modal('hide');
        timerActive = false;
        submit();
        e.preventDefault();
    });
}

function initializeCodeMirrors() {
    $('textarea[data-cm]').each(
        function (idx) {
            var type = $(this).attr('data-cm-file-type');
            var cm = CodeMirror.fromTextArea(this, {
                lineNumbers: true,
                mode: type === 'TASK' ? 'text/plain' : "text/x-java",
                matchBrackets: true,
                readOnly: $(this).attr('data-cm-readonly') === 'true'
            });
            editors.push({
                'cm': cm,
                'readonly': cm.isReadOnly(),
                'name': $(this).attr('data-cm-name'),
                'textarea': this,
                'uuid': $(this).attr('data-cm-id')
            });

            $('a[id="' + $(this).attr('data-cm') + '"]').on('shown.bs.tab',
                function (e) {
                    console.log('shown.bs.tab', e);
                    cm.refresh();
                });

            var $wrapper = $(cm.getWrapperElement());
            $wrapper.resizable({
                resize: function () {
                    cm.setSize($(this).width(), $(this).height());
                    cm.refresh();
                }
            });

            var pos = $('#tabs .tab-content').position();
            var height = window.innerHeight - pos.top - 80;
            $wrapper.css('height', height + 'px');
            cm.refresh();
        });
}

function initializeAssignmentClock() {
    clock = new Clock('440');
    clock.start();
}

function resetOutput() {
    $('#output').removeClass('failure success partial-success');
    $('#output-content').empty();
}

function resetTabColor() {
    $('#output').removeClass('failure success partial-success');
}

function updateOutputHeaderColorActionStarted() {
    var $output = $('#output');
    $output.removeClass('failure success');
    $output.addClass('action-started');
}

function updateOutputHeaderColorActionEnded(success) {
    var $output = $('#output');
    $output.removeClass('failure success action-started');
    if( success ) {
        $output.addClass('success');
    } else {
        $output.addClass('failure');
    }
}

function updateAlertContainerWithScore(message) {
    if (message.success === true) {
        $('#alert-container')
            .empty()
            .append(
                '<div class="alert alert-success p-4" role="alert"><h4 class="alert-heading">Assignment Completed</h4>'
                + '<p>your final score is</p><strong>'
                + message.score + '</strong></div>');
    } else {
        if (parseInt(message.remainingSubmits) <= 0) {
            $('#alert-container')
                .empty()
                .append(
                    '<div class="alert alert-danger p-4" role="alert"><h4 class="alert-heading">Assignment Not OK!</h4>'
                    + '<p>You have no more submits attempts left. Your final score is ' + message.score + '</p></div>');
        } else {
            if (parseInt(message.remainingSubmits) > 0) {
                $('#alert-container')
                    .empty()
                    .append(
                        '<div class="alert alert-warning p-4" role="alert"><h4 class="alert-heading">Assignment Not OK!</h4>'
                        + '<p>No worries, you still have ' + message.remainingSubmits + ' submits attempts left.</p></div>');
            }
        }
    }
}

function appendOutput(txt) {
    $('#output-content').append('<pre>' + escape(txt) + '</pre>');
    showOutput();
}

function escape(txt) {
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
    return ('' + txt).replace(htmlEscaper, function (match) {
        return htmlEscapes[match];
    });
}

function compile() {
    disable();
    resetOutput();
    appendOutput("Compiling ....");
    showOutput();
    activeAction = "COMPILE";
    stomp.send("/app/submit/compile", {}, JSON.stringify({
        'sources': getContent()
    }));
}

function test() {
    disable();
    resetOutput();
    appendOutput("Compiling and testing ....");
    var tests = $("#test-modal input:checkbox:checked").map(function () {
        return $(this).val();
    }).get();
    showOutput();
    activeAction = "TEST";
    stomp.send("/app/submit/test", {}, JSON.stringify({
        'sources': getContent(),
        'tests': tests
    }));
}

function disable() {
    $('#compile').prop('disabled', true);
    $('#test').prop('disabled', true);
    $('#show-tests').prop('disabled', true);
    $('#btn-open-submit').prop('disabled', true);
    $.each(editors, function (idx, val) {
        if (!val.readonly) {
            val.cm.setOption("readOnly", true);
        }
    });
}

function enable() {
    $('#compile').prop('disabled', false);
    $('#test').prop('disabled', false);
    $('#show-tests').prop('disabled', false);
    $('#btn-open-submit').prop('disabled', false);
    $.each(editors, function (idx, val) {
        if (!val.readonly) {
            val.cm.setOption("readOnly", false);
        }
    });
}

function getContent() {
    var editables = [];
    $.each(editors, function (idx, val) {
        if (!val.readonly) {
            var file = {
                uuid: val.uuid,
                content: val.cm.getValue()
            };
            editables.push(file);
        }
    });
    return editables;
}

function submit() {
    disable();
    resetOutput();
    showOutput();
    activeAction = "SUBMIT";
    stomp.send("/app/submit/submit", {}, JSON.stringify({
        'sources': getContent()
    }));
    showSubmitDetails();
}

function showOutput() {
    $('#output').tab('show');
}

function showSubmitDetails() {
    $('#alert-container')
        .empty()
        .append(
            '<div class="alert alert-info p-4" role="alert"><h4 class="alert-heading">Assignment Submitted</h4><p>Well done! You have submitted the assignment for final review. '
            + 'Chill out and wait a few seconds until the results are displayed.</p></div>');
}

function updateSubmits(count) {
    $('#submits').text(count);
}
