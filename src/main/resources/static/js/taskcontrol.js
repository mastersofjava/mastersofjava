let clock = null;
let askingForAudio = false;

$(document).ready(function () {
    setTimeout(() => {
        connect()
        initializeAssignmentClock()
        initXHR()
        initStats()
    }, 1000)
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

    stompClient.onConnect = function () {
        stompClient.subscribe("/queue/session",
            function (data) {
                const msg = JSON.parse(data.body);
                if (competitionHandlers.hasOwnProperty(msg.messageType)) {
                    competitionHandlers[msg.messageType](msg);
                }
                data.ack();
            },
            {ack: 'client'})
    }

    const competitionHandlers = {};
    competitionHandlers['TIMER_SYNC'] = function (msg) {
        if (clock) {
            if (clock.needsAudioAccess()) {
                askForAudio()
            }
            clock.sync(msg.remainingTime, msg.totalTime)
        }
    };
    competitionHandlers['STOP_ASSIGNMENT'] = function (_) {
        if (clock) {
            clock.stop();
        }
    };

    stompClient.activate()
}

function reloadPage(timeout = 1000) {
    window.setTimeout(function () {
        window.location.reload()
    }, timeout)
}

function initializeAssignmentClock() {
    clock = new Clock('440')
    clock.start()
    let isPaused = $('#assignment-clock').hasClass('disabled')
    if (isPaused) {
        clock.setPaused(true)
    }
}

// used from html
function scanAssignments() {
    post('/api/assignment/discover').then(r => {
            showSuccess(`${r.m}`)
            if (r.reload) {
                reloadPage()
            }
        },
        r => {
            showAlert(`${r.m}`)
        })
}

function uploadAssignments(args, form) {
    console.log(args, form, new FormData(form))
    postFormData('/api/assignment/import', new FormData(form))
        .then(r => {
                form.reset()
                showSuccess(`${r.m} Reloading.`)
                reloadPage()
            },
            r => {
                showAlert(`${r.m}`)
            })
}

// used from html
function createCompetition(args, form) {
    const d = formToObject(form)
    if (!(d.assignment instanceof Array)) {
        d.assignment = [d.assignment];
    }
    post('/api/competition', {'name': d.quick_comp_name, 'assignments': d.assignment})
        .then(() => {
                form.reset()
                showSuccess(`Created competition ${d.quick_comp_name}.`)
                reloadPage()
            },
            () => {
                showAlert(`Unable to create competition ${d.quick_comp_name}.`)
            })
}

// used from html
function startGroupSession(args) {
    post(`/api/competition/${args.id}/groupSession`)
        .then(r => {
                showSuccess(`Started group session for competition ${name}, reloading.`)
                reloadPage();
            },
            () => {
                showAlert(`Unable to start group session for competition ${name}.`)
            })
}
function startSingleSession(args) {
    post(`/api/competition/${args.id}/singleSession`)
        .then(r => {
                showSuccess(`Started single session for competition ${name}, reloading.`)
                reloadPage();
            },
            () => {
                showAlert(`Unable to start single session for competition ${name}.`)
            })
}

// used from html
function startAssignment(args) {
    confirm(`Start assignment '${args.name}'?`).then(ok => {
        post(`/api/session/${args.sid}/assignment/${args.id}/start`)
            .then(r => {
                    showSuccess(`Started assignment '${args.name}'.`)
                    clock.playGong();
                    reloadPage(3000)
                },
                () => {
                    showAlert(`Unable to start assignment '${args.name}'.`)
                })
    }, () => {
    })
}

function stopAssignment(args) {
    confirm(`Stop assignment '${args.name}'and finalize scores for teams?`).then(ok => {
        post(`/api/session/${args.sid}/assignment/${args.id}/stop`)
            .then(r => {
                    showSuccess(`Stopped assignment '${args.name}', reloading.`)
                    reloadPage()
                },
                () => {
                    showAlert(`Unable to stop assignment '${args.name}'.`)
                })
    }, () => {
    })

}

function resetAssignment(args) {
    confirm(`Reset assignment '${args.name}'? This will first stop the assignment if running and then reset scores for all teams for this assignment.`).then(ok => {
        post(`/api/session/${args.sid}/assignment/${args.id}/reset`)
            .then(r => {
                    showSuccess(`Assignment '${args.name}' reset, reloading.`)
                    reloadPage()
                },
                () => {
                    showAlert(`Unable to reset assignment '${args.name}'.`)
                })
    }, () => {
    })
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

function initStats() {
    const fetchStats = () => {
        get("/metrics/queues")
            .then(r => {
                updateQueueStats(r)
            })
    }
    window.setInterval(() => {
        fetchStats();
    }, 1000);
    fetchStats();
}

function updateQueueStats(data) {
    const $stats = $('#stats')
    if ($stats) {
        $stats.empty()
        const rowTemplate = (n, w, t, e, k) => {
            return `<tr>
                <td>${n}</td>
                <td>${w}</td>
                <td>${t}</td>
                <td>${e}</td>
                <td>${k}</td>
            </tr>`
        }
        const rows = data
            .sort( (a, b) => {
                if (a.name < b.name) {
                    return -1;
                }
                if (a.name > b.name) {
                    return 1;
                }
                return 0;
            } )
            .map(v => rowTemplate(v.name, v.count, v.added, v.expired, v.killed)).join("\n")
        let statsTable = $.parseHTML(
            `<table class="table table-sm table-striped">
              <thead>
                <tr>
                  <th scope="col">Queue</th>
                  <th scope="col">Waiting</th>
                  <th scope="col">Total</th>
                  <th scope="col">Expired</th>
                  <th scope="col">Killed</th>
                </tr>
              </thead>
              <tbody>
                ${rows}                
              </tbody>
            </table>`)
        $stats.append(statsTable)
    }
}

function invoke(el) {
    const $el = $(el)
    const ref = $el.attr('data-xhr')
    const args = Object.entries($(el).data()).reduce((pv, cv) => {
        if (cv[0].startsWith('xhr') && cv[0].length > 3) {
            let k = cv[0].substring(3);
            pv[k.substring(0, 1).toLowerCase() + k.substring(1)] = cv[1]
        }
        return pv;
    }, {})
    if (validFuncRef(ref)) {
        let fn = eval(ref)
        fn(args, el)
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

function get(uri) {
    return $.ajax({
        type: 'GET',
        url: uri,
        dataType: 'json'
    })
}

function postFormData(uri, data) {
    return $.ajax({
        type: 'POST',
        url: uri,
        data: data,
        processData: false,
        contentType: false
    })
}

function askForAudio() {
    if (!askingForAudio) {
        askingForAudio = true;
        // a click will enable audio
        notify('User action needed to enable audio, click Ok to enable it. Once we become a full single page application this won\'t be needed anymore.', false)
    }
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
    let btn_color = color === 'text-white' ? 'btn-close-white' : '';
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

function createModal(message, allowCancel = true) {
    const cancel = allowCancel ? '<button type="button" class="btn btn-secondary cancel">Cancel</button>' : ''
    let dialog = $.parseHTML(
        `<div class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">
                <div class="modal-dialog" role="document">
                    <div class="modal-content">
                        <div class="modal-body">
                            <p>${message}</p>
                        </div>
                        <div class="modal-footer">
                            ${cancel}
                            <button type="button" class="btn btn-primary ok">Ok</button>
                        </div>
                    </div>
                </div>
            </div>`)
    return dialog[0];
}

function notify(message) {
    const dialog = createModal(message, false)
    const $body = $("body")
    const $dialog = $(dialog)
    $body.append(dialog)

    const m = new bootstrap.Modal(dialog, {'backdrop': 'static'})
    $dialog.find('.ok').on('click', evt => {
        m.dispose()
        dialog.remove()
    })
    m.show($body[0]);
}

function confirm(message, allowCancel = true) {
    const dialog = createModal(message, allowCancel)
    const $dialog = $(dialog)
    const $body = $("body")
    $body.append(dialog)
    const m = new bootstrap.Modal(dialog, {'backdrop': 'static'})
    m.show($body[0]);

    return new Promise((resolve, reject) => {
        if (reject) {
            if (allowCancel) {
                $dialog.find('.cancel').on('click', evt => {
                    m.dispose()
                    dialog.remove()
                    reject('cancel')
                })
            }
        }
        if (resolve) {
            $dialog.find('.ok').on('click', evt => {
                m.dispose()
                dialog.remove()
                resolve({})
            })
        }
    });
}