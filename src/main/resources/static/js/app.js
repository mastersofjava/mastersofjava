let stomp = null;
let editors = [];
let clock = null;
let activeAction = null;
let failed = false;

$(document).ready(function () {
    connectCompetition();
    connectButtons();

    initializeAssignmentClock();
    initializeTextPanes();
    initializeCodeMirrors();
    $(window).resize(function () {
        resizeContent()
    });
    window.setTimeout('$( window ).resize();', 200);
});

function connectCompetition() {
    stomp = new StompJs.Client({
        brokerURL: ((window.location.protocol === "https:") ? "wss://" : "ws://")
            + window.location.hostname
            + ':' + window.location.port
            + "/ws/session/websocket",
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    stomp.onDisconnect = function(frame) {
        $('#status').html('<span>Reconnecting ...</span>');
    }

    stomp.onConnect = function (frame) {
        $('#status').html('<span>Connected</span>');
        // Do something, all subscribes must be done is this callback
        // This is needed because this will be executed after a (re)connect
        stomp.subscribe('/user/queue/session',
            function (data) {
                var msg = JSON.parse(data.body);
                console.log('received', msg);

                if ('TEST' === msg.messageType && msg.test) {
                    var colorStr = msg.success ? 'lightgreen' : 'pink';
                    $('#tabLink_' + msg.test).css('background-color', colorStr);
                }

                if (userHandlers.hasOwnProperty(msg.messageType)) {
                    userHandlers[msg.messageType](msg);
                }
                data.ack();
            },
            {ack: 'client'});
        stomp.subscribe("/queue/session",
            function (data) {
                var msg = JSON.parse(data.body);
                if (competitionHandlers.hasOwnProperty(msg.messageType)) {
                    competitionHandlers[msg.messageType](msg);
                }
                data.ack();
            },
            {ack: 'client'});

        const userHandlers = {};
        userHandlers['COMPILE'] = function (msg) {
            enable();
            appendOutput(msg.message && msg.message.length > 0 ? msg.message : ( msg.success ? 'OK' : 'FAILED' ) );
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
            if (!msg.completed) {
                enable();
            } else {
                disable();
            }
            updateSubmits(msg.remainingSubmits);
            updateAlertContainerWithScore(msg);
            resizeContent();
        };

        const competitionHandlers = {};
        competitionHandlers['TIMER_SYNC'] = function (msg) {
            let lastMessage = msg;
            if (clock) {
                clock.sync(msg.remainingTime, msg.totalTime);
                clock.isPaused = !msg.running;
                if (msg.remainingTime === 0) {
                    disable();
                }
            } else {
                console.log('Unhandled timer sync received',msg);
            }
        };
        competitionHandlers['START_ASSIGNMENT'] = function (msg) {
            window.setTimeout(function () {
                window.location.reload()
            }, 10);
        };
        competitionHandlers['STOP_ASSIGNMENT'] = function (msg) {
            disable();
            if (clock) {
                clock.stop();
            }
        };
    };

    stomp.onStompError = function (frame) {
        // Will be invoked in case of error encountered at Broker
        // Bad login/passcode typically will cause an error
        // Complaint brokers will set `message` header with a brief message. Body may contain details.
        // Compliant brokers will terminate the connection after any error
        console.log('Broker reported error: ' + frame.headers['message']);
        console.log('Additional details: ' + frame.body);
    };

    stomp.activate();
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

    const list = $('.CodeMirror-code:visible .CodeMirror-line');
    $(list).each(function () {
        const isHtml = this.innerHTML.indexOf('&gt;') !== -1 && this.innerHTML.indexOf('&lt;') !== -1;
        if (isHtml) {
            const input = this.innerHTML.replace(/&gt;/g, '>').replace(/&lt;/g, '<');
            console.log(input);
            this.innerHTML = input;
        }
    });
}

function initializeTextPanes() {
    $('div.markdown').each(
        function (idx) {
            $(this).html(function (idx, content) {
                return marked.parse(content)
            })
        }
    )
}

function resizeContent() {
    let pos = $('#tabs .tab-content').position();
    if (pos) {
        const height = window.innerHeight - pos.top - 80;
        $('#tabs .content').each(function (idx) {
            $(this).css('height', height + 'px');
        })

        $('#tabs .CodeMirror.ui-resizable').each(function (idx) {
            $(this).css('height', height + 'px');
        })
    }
}

function initializeCodeMirrors() {
    // let texts = [];
    // let cmList = [];
    $('textarea[data-cm]').each(
        function (idx) {
            let type = $(this).attr('data-cm-file-type');
            // texts.push(this);
            let isTask = type === 'TASK';
            let isReadOnly = $(this).attr('data-cm-readonly') === 'true';
            let cm = CodeMirror.fromTextArea(this, {
                lineNumbers: true,
                mode: isTask ? 'text/x-markdown' : "text/x-java",
                matchBrackets: true,
                readOnly: isReadOnly
            });
            // cmList.push(cm);
            editors.push({
                'cm': cm,
                'readonly': cm.isReadOnly(),
                'name': $(this).attr('data-cm-name'),
                'textarea': this,
                'uuid': $(this).attr('data-cm-id')
            });

            let tabLink = $('button[id="' + $(this).attr('data-cm') + '"]').on('shown.bs.tab',
                function (e) {
                    cm.refresh();
                    if (isTask) codeMirror_insertImagesInAssignmentText();
                    $(window).resize();
                });

            let $wrapper = $(cm.getWrapperElement());
            $wrapper.resizable({
                resize: function () {
                    cm.setSize($(this).width(), $(this).height());
                    tabLink.trigger('shown.bs.tab');
                }
            });
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
    const $output = $('#output');
    $output.removeClass('failure success');
    $output.addClass('action-started');
}

function updateOutputHeaderColorActionEnded(success) {
    const $output = $('#output');
    $output.removeClass('failure success action-started');
    if (success) {
        $output.addClass('success');
    } else {
        $output.addClass('failure');
    }
}

function updateAlertContainerWithScore(message) {
    if (message.completed && message.success === true) {
        $('#alert-container')
            .empty()
            .append(
                '<div class="alert alert-success p-4" role="alert"><h4 class="alert-heading">Assignment Completed</h4>'
                + '<p>Your final score is</p><strong>'
                + message.score + '</strong>'
                +'<p class="pre-wrap">' + message.scoreExplanation +'</p>'
                +'</div>');
    } else if( message.rejected ) {
        $('#alert-container')
            .empty()
            .append(
                '<div class="alert alert-danger p-4" role="alert"><h4 class="alert-heading">Submit Rejected</h4>'
                + '<p>'+ message.message +'</p></div>');
    } else {
        if (message.completed) {
            $('#alert-container')
                .empty()
                .append(
                    '<div class="alert alert-danger p-4" role="alert"><h4 class="alert-heading">Assignment Not OK!</h4>'
                    + '<p>Your final score is <strong>' + message.score + '</strong></p>'
                	+'<p class="pre-wrap">' + message.scoreExplanation +'</p>'
                    +'</div>');
        } else {
            if (parseInt(message.remainingSubmits) > 0) {
                $('#alert-container')
                    .empty()
                    .append(
                        '<div class="alert alert-warning p-4" role="alert"><h4 class="alert-heading">Assignment Not OK!</h4>'
                        + '<p>No worries, you still have ' + message.remainingSubmits + ' attempts left.</p></div>');
            }
        }
    }
}

function appendOutput(txt) {
    $('#output-content').append('<pre>' + escape(txt) + '</pre>');
    showOutput();
}

function escape(txt) {
    const htmlEscapes = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#x27;',
        '/': '&#x2F;'
    };

    // Regex containing the keys listed immediately above.
    const htmlEscaper = /[&<>"'\/]/g;

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
    publish("/app/submit/compile", JSON.stringify({
        'sources': getContent(),
        'assignmentName': assignmentName,
        'uuid': $('#sessions').val(),
        'timeLeft': getTimeleft()
    }));
}

function doUserActionTest() {
    disable();
    resetOutput();
    appendOutput("Compiling and testing ....");
    const tests = $("#test-modal input:checkbox:checked").map(function () {
        return $(this).val();
    }).get();
    showOutput();
    activeAction = "TEST";
    publish("/app/submit/test", JSON.stringify({
        'sources': getContent(),
        'tests': tests,
        'assignmentName': assignmentName,
        'uuid': $('#sessions').val(),
        'timeLeft': getTimeleft()
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
    const editables = [];
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
    publish("/app/submit/submit", JSON.stringify({
        'sources': getContent(),
        'uuid': $('#sessions').val(),
        'assignmentName': assignmentName,
        'timeLeft': getTimeleft()
    }));
    showSubmitDetails();
    resizeContent();
}

function getTimeleft() {
    return '' + $('#assignment-clock').data()['timeLeft'];
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

function publish(destination, body) {
    stomp.publish({destination: destination, body: body});
}
