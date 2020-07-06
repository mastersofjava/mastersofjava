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

                if ('TEST'===msg.messageType && msg.test) {
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
                if (competitionHandlers.hasOwnProperty(msg.messageType)) {
                    competitionHandlers[msg.messageType](msg);
                }
            });
    });

    userHandlers = {};
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

    competitionHandlers = {};
    competitionHandlers['TIMER_SYNC'] = function (msg) {
        lastMessage = msg;
        if (clock && isSessionValid(msg)) {
            clock.sync(msg.remainingTime, msg.totalTime);
            clock.isPaused = !msg.running;
        } else {
            console.log('timer msg retrieved:' +msg.sessionId + " " + msg.remainingTime + ' ' +$('#sessions').val());
        }
    };
    competitionHandlers['START_ASSIGNMENT'] = function (msg) {
        window.setTimeout(function () {
            if (isSessionValid(msg)) {
                window.location.reload()
            }
        }, 10);
    };
    competitionHandlers['STOP_ASSIGNMENT'] = function (msg) {
        if (isSessionValid(msg)) {
            disable();
            if (clock) {
                clock.stop();
            }
        }
    };
    if (window.isWithValidation) {
        window.setTimeout('doUserActionTest();',1000);
    }
}
function isSessionValid(msg) {
    return msg.sessionId==null || $('#sessions').val()===msg.sessionId;
}

function connectButtons() {
    $('#compile').click(function (e) {
        doUserActionCompile();
        e.preventDefault();
    });
    $('#test').click(function (e) {
        $('#test-modal').modal('hide');
        doUserActionTest();
        e.preventDefault();
    });
    $('#submit').click(function (e) {
        resetTabColor();
        $('#confirm-submit-modal').modal('hide');
        timerActive = false;
        doUserActionSubmit();
        e.preventDefault();
    });
}

/**
 * after CodeMirror the Assignment Text, this function insert images in dislay.
 */
function codeMirror_insertImagesInAssignmentText() {

    var list = $('.CodeMirror-code:visible .CodeMirror-line');
    $(list).each(function() {
        var isHtml = this.innerHTML.indexOf('&gt;')!=-1&&this.innerHTML.indexOf('&lt;')!=-1;
        if (isHtml) {
            var input = this.innerHTML.replace(/&gt;/g,'>').replace(/&lt;/g,'<');
            console.log(input);
            this.innerHTML = input;
        }
    });
}
function initializeCodeMirrors() {
    texts = [];
    cmList = [];
    $('textarea[data-cm]').each(
        function (idx) {
            var type = $(this).attr('data-cm-file-type');
            texts.push(this);
            var isTask = type === 'TASK';
            var isReadOnly = $(this).attr('data-cm-readonly') === 'true';

            var cm = CodeMirror.fromTextArea(this, {
                lineNumbers: true,
                mode: isTask ? 'text/plain' : "text/x-java",
                matchBrackets: true,
                readOnly: isReadOnly
            });
            cmList.push(cm);
            editors.push({
                'cm': cm,
                'readonly': cm.isReadOnly(),
                'name': $(this).attr('data-cm-name'),
                'textarea': this,
                'uuid': $(this).attr('data-cm-id')
            });

            var tabLink = $('a[id="' + $(this).attr('data-cm') + '"]').on('shown.bs.tab',
                function (e) {
                    console.log('shown.bs.tab', e);
                    cm.refresh();
                    if (isTask) codeMirror_insertImagesInAssignmentText();
                });

            var $wrapper = $(cm.getWrapperElement());
            $wrapper.resizable({
                resize: function () {
                    cm.setSize($(this).width(), $(this).height());
                    tabLink.trigger('shown.bs.tab');
                }
            });

            var pos = $('#tabs .tab-content').position();
            var height = window.innerHeight - pos.top - 80;
            $wrapper.css('height', height + 'px');
            if (isTask) {
                tabLink.trigger('shown.bs.tab');
            }
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

function doUserActionCompile() {
    disable();
    resetOutput();
    appendOutput("Compiling ....");
    showOutput();
    activeAction = "COMPILE";
    stomp.send("/app/submit/compile", {}, JSON.stringify({
        'sources': getContent(),
        'assignmentName': assignmentName
    }));
}

function doUserActionTest() {
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
        'tests': tests,
        'assignmentName': assignmentName
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

function doUserActionSubmit() {
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
