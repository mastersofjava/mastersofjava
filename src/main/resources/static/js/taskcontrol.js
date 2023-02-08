$(document).ready(function () {
    setTimeout(() => {
        connect()
        initializeAssignmentClock()
        initXHR()
    }, 500)
})

function connect() {
    window.stompClient = new StompJs.Client({
        brokerURL: ((window.location.protocol === 'https:') ? 'wss://' : 'ws://')
            + window.location.hostname
            + ':' + window.location.port
            + '/control/websocket',
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    })

    stompClient.onConnect = function (frame) {
        console.log('Connected to control')
        console.log('Subscribe to /user/queue/controlfeedback')
        stompClient.subscribe('/user/queue/controlfeedback',
            function (msg) {
                console.log('/user/queue/controlfeedback ')
                showAlert(msg.body)
                if (msg.body.indexOf('reload') !== -1) {
                    reloadPage()
                }
                msg.ack()
            },
            {ack: 'client'})
        stompClient.subscribe('/queue/controlfeedback',
            function (msg) {
                console.log('/queue/controlfeedback ')
                console.log(msg)
                var m = JSON.parse(msg.body)
                showAlert('[' + m.assignment + '] ' + m.cause)
                msg.ack()
            },
            {ack: 'client'})
        console.log('Subscribe to /control/queue/time')
        stompClient.subscribe('/queue/time',
            function (msg) {
                console.log('/queue/time')
                var message = JSON.parse(msg.body)
                if (clock) {
                    clock.sync(message.remainingTime, message.totalTime)
                }
                msg.ack()
            },
            {ack: 'client'})
        console.log('subscribe to /control/queue/start')
        stompClient.subscribe('/queue/start',
            function (msg) {
                console.log('/queue/start')
                reloadPage()
                msg.ack()
            },
            {ack: 'client'})
        console.log('subscribe to /control/queue/stop')
        stompClient.subscribe('/queue/stop',
            function (msg) {
                console.log('/queue/stop')
                if (clock) {
                    clock.stop()
                }
                msg.ack()
            },
            {ack: 'client'})
    }

    stompClient.activate()
}

function reloadPage() {
    window.setTimeout(function () {
        window.location.reload()
    }, 1000)
}

function isWithRunningAssignment() {
    return $('#play:visible').length === 1
}

function getActiveAssignmentIfAny() {
    if (!isWithRunningAssignment()) {
        return null
    }
    return $('#pills-competitie').attr('title')
}

function getSelectedAssignmentIfAny() {
    return $('input[name=\'assignment\']:checked').val()
}

function isWithCompletedSelectedAssignment() {
    return $('input[name=\'assignment\']:checked').closest('.completed').length === 1
}


function startTask() {
    var taskname = validateAssignmentSelected()
    if (!taskname) {
        return// user gets feedback to first select an assigment beforehand.
    }
    var isDefaultStart = !isWithRunningAssignment() && !isWithCompletedSelectedAssignment()

    if (isDefaultStart) {
        clientSend('/app/control/starttask', {'taskName': taskname})

        var tasktime = $('input[name=\'assignment\']:checked').attr('time')

        console.log('tasktime ' + tasktime)
        $assignmentClock = $('#assignment-clock')
        $assignmentClock.attr('data-time', tasktime)
        $assignmentClock.attr('data-time-left', tasktime)
    } else {
        var activeAssignment = getActiveAssignmentIfAny()
        var selectedAssignment = getSelectedAssignmentIfAny()

        if (activeAssignment === selectedAssignment || isWithCompletedSelectedAssignment()) {
            $('#restartAssignment-modal').find('.openModalViaJs').click()
        } else {
            $('#startAssignmentNow-modal').find('.openModalViaJs').click()
        }
    }
}

function startTaskSmartStartNow() {
    // start directly: ActiveAssignment !== SelectedAssignment
    clientSend('/app/control/stoptask', {'taskName': getSelectedAssignmentIfAny()})
}

function startTaskSmartRestart() {
    // restart smart: ActiveAssignment === SelectedAssignment
    clientSend('/app/control/restartAssignment', {
        'taskName': getSelectedAssignmentIfAny(),
        'value': getSelectedAssignmentIfAny()
    })
}

/**
 * this will stop the current running assignment and save all scores.
 */
function stopTask() {
    if (validateAssignmentSelected()) {
        clientSend('/app/control/stoptask', {})
    }
}

function restartAssignment() {
    var taskname = validateAssignmentSelected()
    if (!taskname) {
        return
    }
    clientSend('/app/control/restartAssignment', {
        'taskName': taskname
    })
}

function pauseResume() {
    clientSend('/app/control/pauseResume', {})
}

function validateAssignmentSelected() {
    $('#alert').empty()
    var taskname = getSelectedAssignmentIfAny()
    if (!taskname) {
        showAlert('No assignment selected')
    }
    return taskname
}

function clearCompetition() {
    $('#alert').empty()
    clientSend('/app/control/clearCompetition', {})
    showAlert('session has been restarted')
}

function doCompetitionCreateNew(name) {
    if (name) {
        name = name.trim()
        if (name.indexOf('|') === -1) {
            name += '|' + $('#selectedYear').val()
        }
        clientSend('/app/control/competitionCreateNew', {taskName: 'competitionCreateNew', value: name})
    }
    return name
}

function doCompetitionDelete() {
    var selectedUuid = $('.sessionTable .selected.tableSubHeader').attr('id')
    clientSend('/app/control/competitionDelete', {taskName: 'competitionDelete', uuid: selectedUuid})
}

function clientSend(destinationUri, taskMap) {
    console.log('clientSend ' + destinationUri)
    console.log(taskMap)
    if (stompClient.connected) {
        stompClient.publish({destination: destinationUri, body: JSON.stringify(taskMap)})
    } else {
        showAlert('Uw connectie is verlopen, dus uw pagina wordt opnieuw geladen')
        reloadPage()
    }

}

function showOutput(messageOutput) {
    var response = $('#response')
    response.empty()
    var p = document.createElement('p')
    p.style.wordWrap = 'break-word'
    p.appendChild(document.createTextNode(messageOutput))
    response.append(p)
}

function initializeAssignmentClock() {
    console.log('initializeAssignmentClock ')
    window.clock = new Clock('440')
    clock.start()
    var isPaused = $('#assignment-clock').hasClass('disabled')
    console.log('isPaused ' + isPaused)
    if (isPaused) {
        clock.setPaused(true)
    }
}

// -- new methods

// used from html
function scanAssignments() {
    post('/api/assignment/discover').then( r => {
        showSuccess(`${r.m}`)
        if( r.reload ) {
            reloadPage()
        }
      },
      r => {
        console.log(r)
          showAlert(`${r.m}`)
      })
}

// used from html
function createCompetition(args,form) {
    const d = formToObject(form)
    post('/api/competition', {'name': d.quick_comp_name, 'assignments': d.assignment})
        .then(() => {
                form.reset()
                showSuccess(`Created competition ${d.quick_comp_name}.`)
            },
            () => {
                showAlert(`Unable to create competition ${d.quick_comp_name}.`)
            })
}

// used from html
function startSession(args) {
    post(`/api/competition/${args.id}/session`)
        .then(r => {
                showSuccess(`Started competition session for competition ${name}, reloading.`)
                reloadPage();
            },
            () => {
                showAlert(`Unable to start competition session for competition ${name}.`)
            })
}

// used from html
function startAssignment(args) {
    confirm(`Start assignment '${args.name}'?`).then(ok => {
        post(`/api/session/${args.sid}/assignment/${args.id}/start`)
            .then(r => {
                    console.log(r)
                    showSuccess(`Started assignment '${args.name}'.`)
                },
                () => {
                    showAlert(`Unable to start assignment '${args.name}'.`)
                })
    }, () => {})
}

function stopAssignment(args) {
    confirm(`Stop assignment '${args.name}'and finalize scores for teams?`).then(ok => {
        post(`/api/session/${args.sid}/assignment/${args.id}/stop`)
            .then(r => {
                    console.log(r)
                    showSuccess(`Stopped assignment '${args.name}', reloading.`)
                    reloadPage()
                },
                () => {
                    showAlert(`Unable to stop assignment '${args.name}'.`)
                })
    }, () => {})

}

function resetAssignment(args) {
    confirm(`Reset assignment '${args.name}'? This will reset scores for all teams.`).then( ok => {
        post(`/api/session/${args.sid}/assignment/${args.id}/reset`)
            .then(r => {
                    console.log(r)
                    showSuccess(`Assignment '${args.name}' reset, reloading.`)
                    reloadPage()
                },
                () => {
                    showAlert(`Unable to reset assignment '${args.name}'.`)
                })
    }, () => {})
}

function formToObject(form) {
    return $(form).serializeArray().reduce((pv, cv) => {
        if (pv.hasOwnProperty(cv.name)) {
            if (Array.isArray(pv[cv.name])) {
                pv[cv.name] = [...pv[cv.name], cv.value]
            } else {
                pv[cv.name] = [pv[cv.name], cv.value]
            }
        } else {
            pv[cv.name] = cv.value
        }
        return pv
    }, {})
}

function initXHR() {
    $('form[data-xhr]').each((idx, el) => {
        el.addEventListener('submit', evt => {
            evt.preventDefault()
            invoke(el)
        })
    })
    $('button[data-xhr]').each((idx, el) => {
        el.addEventListener('click', evt => {
            evt.preventDefault()
            invoke(el)
        })
    })
}

function invoke( el ) {
    const $el = $(el)
    const ref = $el.attr('data-xhr')
    const args = Object.entries($(el).data()).reduce((pv,cv) => {
        if (cv[0].startsWith('xhr') && cv[0].length > 3) {
            let k = cv[0].substring(3);
            pv[k.substring(0,1).toLowerCase() + k.substring(1)] = cv[1]
        }
        return pv;
    }, {})
    if (validFuncRef(ref)) {
        let fn = eval(ref)
        fn(args,el)
    }
}

function post(uri, data = {}) {
    return $.ajax({
        type: 'POST',
        url: uri,
        data: JSON.stringify(data),
        contentType: 'application/json; charset=utf-8',
        dataType: 'json'
    })
}

function validFuncRef(ref) {
    return /^\w+$/.test(ref)
}

function showAlert(txt) {
    showNotice(txt, 'bg-danger')
}

function showInfo(txt) {
    showNotice(txt, 'bg-info')
}

function showSuccess(txt) {
    showNotice(txt, 'bg-success')
}

function showNotice(txt, bg = 'bg-success', color = 'text-white') {
    let toasts = $('#toasts')
    let btn_color = color === 'text-white'?'btn-close-white':'';
    let toast = $.parseHTML(`<div class="toast" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="toast-header ${bg} ${color}">
            <strong class="me-auto">Alert</strong>
            <button type="button" class="btn-close ${btn_color}" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
        <div class="toast-body">${txt}</div>
    </div>`)
    toasts.prepend(toast)
    $(toast).on('hidden.bs.toast', evt => {
        evt.target.remove()
    })
    new bootstrap.Toast(toast[0]).show()
}

function confirm(message) {
    let dialog = $.parseHTML(
        `<div class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">
                <div class="modal-dialog" role="document">
                    <div class="modal-content">
                        <div class="modal-body">
                            <p>${message}</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary cancel">Cancel</button>
                            <button type="button" class="btn btn-primary ok">Ok</button>
                        </div>
                    </div>
                </div>
            </div>`)
    const $body = $("body")
    $body.append(dialog)
    const m = new bootstrap.Modal(dialog[0], {'backdrop': 'static'})
    let $dialog = $(dialog)

    return new Promise((resolve, reject) => {
        if( reject ) {
            $dialog.find('.cancel').on('click', evt => {
                m.dispose()
                dialog[0].remove()
                reject('cancel')
            })
        }
        if( resolve ) {
            $dialog.find('.ok').on('click', evt => {
                m.dispose()
                dialog[0].remove()
                resolve({})
            })
        }
        m.show($body[0]);
    });
}