
$(document).ready(function () {
    connect();
    initializeAssignmentClock();
    clientOnload();
});



function connect() {
    var socket = new WebSocket(
        ((window.location.protocol === "https:") ? "wss://" : "ws://")
        + window.location.hostname
        + ':' + window.location.port
        + '/control/websocket');
    window.stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        console.log('Connected to control');
        console.log('Subscribe to /user/queue/controlfeedback');
        stompClient.subscribe('/user/queue/controlfeedback', function (msg) {
            console.log("/user/queue/controlfeedback ");
            showAlert(msg.body);
            if (msg.body.indexOf('reload')!=-1) {
                reloadPage();
            }
        });
        stompClient.subscribe('/queue/controlfeedback', function(msg){
            console.log("/queue/controlfeedback ");
            console.log(msg);
            var m = JSON.parse(msg.body);
            showAlert('['+m.assignment +'] ' + m.cause);
        });
        console.log('Subscribe to /control/queue/time');
        stompClient.subscribe('/queue/time', function (taskTimeMessage) {
            console.log("/queue/time");
            var message = JSON.parse(taskTimeMessage.body);
            if (clock) {
                clock.sync(message.remainingTime, message.totalTime);
            }
        });
        console.log('subscribe to /control/queue/start');
        stompClient.subscribe('/queue/start', function (msg) {
            console.log("/queue/start");
            reloadPage();
        });
        console.log('subscribe to /control/queue/stop');
        stompClient.subscribe('/queue/stop', function (msg) {
            console.log("/queue/stop");
            if (clock) {
                clock.stop();
            }
        });
    });
}

function reloadPage() {
    window.setTimeout(function () {
        window.location.reload();
    }, 1000);
}

function startTask() {
    var taskname = validateAssignmentSelected();
    if (!taskname) {
        return;
    }
    stompClient.send("/app/control/starttask", {}, JSON.stringify({
        'taskName': taskname
    }));

    var tasktime = $("input[name='assignment']:checked").attr('time');

    console.log('tasktime ' + tasktime);
    $assignmentClock = $('#assignment-clock');
    $assignmentClock.attr('data-time', tasktime);
    $assignmentClock.attr('data-time-left', tasktime);
}

function stopTask() {
    if (validateAssignmentSelected()) {
        stompClient.send("/app/control/stoptask", {}, {});
    }
}

function restartAssignment() {
    var taskname = validateAssignmentSelected();
    if (!taskname) {
        return;
    }
    stompClient.send("/app/control/restartAssignment", {}, JSON.stringify({
        'taskName': taskname
    }));
}

function pauseResume() {
    stompClient.send("/app/control/pauseResume", {}, {});
}
function validateAssignmentSelected() {
    $('#alert').empty();
    var taskname = $("input[name='assignment']:checked").val();
    if (!taskname) {
        showAlert("No assignment selected");
    }
    return taskname;
}
function clearAssignments() {
    $('#alert').empty();
    showAlert("competition has been restarted");
    stompClient.send("/app/control/clearCurrentAssignment", {}, {});
}

function doCompetitionSaveName(name, uuid) {
    console.log('name ' + name + " " + uuid);
    if (name) {
        clientSend("/app/control/competitionSaveName", { taskName: 'competitionSaveName', uuid: uuid, value:name });
    }
}
function doCompetitionCreateNew(name) {
    if (name) {
        clientSend("/app/control/competitionCreateNew", { taskName: 'competitionCreateNew', value:name });
    }
    return name;
}
function doCompetitionDelete() {
    var selectedUuid = $('.sessionTable .selected.tableSubHeader').attr('id');
    clientSend("/app/control/competitionDelete", { taskName: 'competitionDelete', uuid:selectedUuid });
}
function clientSend(destinationUri, taskMap) {
    console.log('clientSend '+ destinationUri);
    console.log(taskMap);
    stompClient.send(destinationUri, {}, JSON.stringify(taskMap));
}
function clientSelectSubtable(node) {
    $(node).closest('table').find('tr').removeClass('selected');
    $(node).closest('table').find('tr.subrows').addClass('hide');
    $(node).closest('tbody').find('tr.subrows').removeClass('hide');
    $(node).closest('tbody').find('tr').addClass('selected');
}
function clientOnload() {
    $('[data-toggle="popover"]').popover();
    clientStoreRender();
    $('li.nav-item a.nav-link').click(function() {
        window.setTimeout('clientStoreStateWrite()',200);
    });
}
function clientStoreStateWrite() {

    var idList = [];
    $('.nav-link.active').each(function() {
        idList.push(this.id);
    });
    console.log('clientStoreStateWrite '+JSON.stringify(idList));
    localStorage.setItem('tabState', JSON.stringify(idList));
}
function clientUpdateRole(value) {
    console.log('clientUpdateRole ' + value);
    $('.role').addClass('hide');

    if (value=='admin') {
        $('.role').removeClass('hide');
    }
    if (value=='gamemaster') {
        $('.role.gamemasterRole').removeClass('hide');
    }
}
function clientStoreStateRead() {
    console.log('clientStoreStateRead '+localStorage.getItem('tabState'));
    idList = JSON.parse(localStorage.getItem('tabState'));
    return idList;
}
function clientStoreRender() {
    $(clientStoreStateRead()).each(function() {
        $('#'+this).click();
    });
}
function scanAssignments() {
    var year = $('#selectedYear').val().split('-')[0];

    stompClient.send("/app/control/scanAssignments", {},  JSON.stringify({
        'taskName': year
    }));
}

function showOutput(messageOutput) {
    var response = $('#response');
    response.empty();
    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(messageOutput));
    response.append(p);
}

function showAlert(txt) {
    var alert = $('#alert');
    alert.empty();
    alert.append('<div class="alert alert-danger alert-dismissible fade show" role="alert">' +
        '<span>'+txt+'</span>' +
        '<button type="button" class="close" data-dismiss="alert" aria-label="Close"> <span aria-hidden="true">&times;</span></button>' +
        '</div>');
}

function initializeAssignmentClock() {
    console.log('initializeAssignmentClock ' );
    window.clock = new Clock('440');
    clock.start();
    var isPaused = $('#assignment-clock').hasClass('disabled');
    console.log('isPaused ' + isPaused);
    if (isPaused) {
        clock.setPaused(true);
    }

}

