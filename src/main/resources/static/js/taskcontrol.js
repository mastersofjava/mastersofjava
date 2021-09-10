
$(document).ready(function () {
    connect();
    initializeAssignmentClock();
    clientOnload();
});

function connect() {
    window.stompClient = new StompJs.Client({
        brokerURL: ((window.location.protocol === "https:") ? "wss://" : "ws://")
            + window.location.hostname
            + ':' + window.location.port
            + "/control/websocket",
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    stompClient.onConnect = function(frame) {
        console.log('Connected to control');
        console.log('Subscribe to /user/queue/controlfeedback');
        stompClient.subscribe('/user/queue/controlfeedback',
            function (msg) {
                console.log("/user/queue/controlfeedback ");
                showAlert(msg.body);
                if (msg.body.indexOf('reload')!==-1) {
                    reloadPage();
                }
                msg.ack();
            },
            {ack: 'client'});
        stompClient.subscribe('/queue/controlfeedback',
            function(msg){
                console.log("/queue/controlfeedback ");
                console.log(msg);
                var m = JSON.parse(msg.body);
                showAlert('['+m.assignment +'] ' + m.cause);
                msg.ack();
            },
            {ack: 'client'});
        console.log('Subscribe to /control/queue/time');
        stompClient.subscribe('/queue/time',
            function (msg) {
                console.log("/queue/time");
                var message = JSON.parse(msg.body);
                if (clock) {
                    clock.sync(message.remainingTime, message.totalTime);
                }
                msg.ack();
            },
            {ack: 'client'});
        console.log('subscribe to /control/queue/start');
        stompClient.subscribe('/queue/start',
            function (msg) {
                console.log("/queue/start");
                reloadPage();
                msg.ack();
            },
            {ack: 'client'});
        console.log('subscribe to /control/queue/stop');
        stompClient.subscribe('/queue/stop',
            function (msg) {
                console.log("/queue/stop");
                if (clock) {
                    clock.stop();
                }
                msg.ack();
            },
            {ack: 'client'});
    };

    stompClient.activate();
}

function reloadPage() {
    window.setTimeout(function () {
        window.location.reload();
    }, 1000);
}

function isWithRunningAssignment() {
    return $('#play:visible').length === 1;
}
function getActiveAssignmentIfAny() {
    if (!isWithRunningAssignment()) {
        return null;
    }
    return $('#pills-competitie').attr('title');
}
function getSelectedAssignmentIfAny() {
    return $("input[name='assignment']:checked").val();
}
function isWithCompletedSelectedAssignment() {
    return $("input[name='assignment']:checked").closest('.completed').length===1;
}
function startTask() {
    var taskname = validateAssignmentSelected();
    if (!taskname) {
        return;// user gets feedback to first select an assigment beforehand.
    }
    var isDefaultStart = !isWithRunningAssignment() && !isWithCompletedSelectedAssignment();

    if (isDefaultStart) {
        clientSend("/app/control/starttask",  {'taskName': taskname});

        var tasktime = $("input[name='assignment']:checked").attr('time');

        console.log('tasktime ' + tasktime);
        $assignmentClock = $('#assignment-clock');
        $assignmentClock.attr('data-time', tasktime);
        $assignmentClock.attr('data-time-left', tasktime);
    } else {
        var activeAssignment = getActiveAssignmentIfAny();
        var selectedAssignment = getSelectedAssignmentIfAny();

        if (activeAssignment===selectedAssignment||isWithCompletedSelectedAssignment()) {
            $('#restartAssignment-modal').find('.openModalViaJs').click();
        } else {
            $('#startAssignmentNow-modal').find('.openModalViaJs').click();
        }
    }
}
function startTaskSmartStartNow() {
    // start directly: ActiveAssignment !== SelectedAssignment
    clientSend("/app/control/stoptask", {'taskName':getSelectedAssignmentIfAny()});
}
function startTaskSmartRestart() {
    // restart smart: ActiveAssignment === SelectedAssignment
    clientSend("/app/control/restartAssignment", {
        'taskName': getSelectedAssignmentIfAny(),
        'value':getSelectedAssignmentIfAny()
    });
}
/**
 * this will stop the current running assignment and save all scores.
 */
function stopTask() {
    if (validateAssignmentSelected()) {
        clientSend("/app/control/stoptask", {});
    }
}

function restartAssignment() {
    var taskname = validateAssignmentSelected();
    if (!taskname) {
        return;
    }
    clientSend("/app/control/restartAssignment", {
        'taskName': taskname
    });
}

function pauseResume() {
    clientSend("/app/control/pauseResume", {});
}
function validateAssignmentSelected() {
    $('#alert').empty();
    var taskname = getSelectedAssignmentIfAny();
    if (!taskname) {
        showAlert("No assignment selected");
    }
    return taskname;
}
function clearCompetition() {
    $('#alert').empty();
    clientSend("/app/control/clearCompetition",{});
    showAlert("competition has been restarted");
}
function updateSettingRegistrationFormDisabled(isInput) {
    clientSend("/app/control/updateSettingRegistration", { taskName: 'updateSettingRegistration', value:''+ (isInput==true) });
}
function updateTeamStatus(uuid, value) {
    clientSend("/app/control/updateTeamStatus", { taskName: 'updateTeamStatus', uuid: uuid, value:value });
}
function doCompetitionSaveName(node, uuid) {
    var name = node.innerText.trim();
    console.log('name ' + name + " " + uuid);
    if (name) {
        clientSend("/app/control/competitionSaveName", { taskName: 'competitionSaveName', uuid: uuid, value:name });
    }
}
function doCompetitionToggleState(uuid, value) {
    console.log('competitionToggleAvailability ' + value + " " + uuid);
    clientSend("/app/control/competitionToggleAvailability", { taskName: 'competitionToggleAvailability', uuid: uuid, value:value });
}
function doCompetitionCreateNew(name) {
    if (name) {
        name = name.trim();
        if (name.indexOf('|')===-1) {
            name += '|'+ $('#selectedYear').val();
        }
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
    if (stompClient.connected) {
        stompClient.publish({destination: destinationUri, body: JSON.stringify(taskMap)})
    } else {
        showAlert('Uw connectie is verlopen, dus uw pagina wordt opnieuw geladen');
        reloadPage();
    }

}

function doValidatePerformance() {
    clientSend("/app/control/performanceValidation", { });
}
function clientSelectSubtable(node, rowIdentifierValue) {
    $(node).closest('table').find('tr').removeClass('selected');
    $(node).closest('table').find('tr.subrows').addClass('hide');
    $(node).closest('tbody').find('tr.subrows').removeClass('hide');
    $(node).closest('tbody').find('tr').addClass('selected');
    
    // close row components in tab
    var hiddenRowComponents = $(node).closest('.tab-pane').find('.rowComponent').addClass('hide');
    console.log('hiddenRowComponents ' +hiddenRowComponents.length + ' val ' + rowIdentifierValue + ' hiddenRow '+ hiddenRowComponents.find('.rowIdentifier').length);
    if (rowIdentifierValue) {
        hiddenRowComponents.find('.rowIdentifier').val(rowIdentifierValue);
    }
}
function clientOnload() {
    clientStoreRender();
    $('li.nav-item a.nav-link').click(function() {
        window.setTimeout('clientStoreStateWrite()',200);
    });
    $('[data-toggle="popover"]').popover();// popovers are not used yet.
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
    var roleSpecificFields = $('.role');
    roleSpecificFields.addClass('hide');

    if (value==='admin') {
        roleSpecificFields.removeClass('hide');
    }
    if (value==='gamemaster') {
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
        var tabButton = $('#'+this);
        if (tabButton.length) {
            tabButton.click();
        } else {
            console.log("not existing tabButton with id: "+this);
        }
    });
}
function scanAssignments() {
    var year = $('#selectedYear').val().split('-')[0];
    clientSend("/app/control/scanAssignments", { 'taskName': year });
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
function doValidateAssignment(assignmentId, isWithSolution) {
    var postFix = isWithSolution?'&solution':'';
    if (assignmentId) {
        window.open('/assignmentAdmin?assignment='+assignmentId+'&validate'+postFix,'admin');
    }
}
function doViewDeltaSolution(assignmentId,node) {
    var isOtherChildEvent = window.event.target.tagName.toLowerCase()==='select';
    if (isOtherChildEvent) {
        return;
    }
    $('#deltaSolution-modal').find('.openModalViaJs').click();
    var title= node.title.split('-')[0];
    $('#deltaSolution-modal .modal-title').html(title);
    $('#deltaSolution-modal button').attr('title', assignmentId);
    var codeTxt = $(node).find('textarea').val().replace(/</g,'&lt;').replace(/>/g,'&gt;');//small encoding into valid html

    $('#deltaSolution-modal pre.code').removeClass('hide').html(codeTxt);
    var reviewTxt = 'Plaats de review comments in de readme.md bij deze opdracht.';
    var isWithReview = $(node).find('pre.review').length !== 0;
    var toggleButton = $('button.reviewButton');
    var startText = toggleButton.attr('text').split(',')[0];
    if (isWithReview) {
        reviewTxt = $(node).find('pre.review').html();

        toggleButton.removeAttr('disabled');
    } else {
        toggleButton.attr('disabled', 'disabled').attr('title', reviewTxt);
    }
    toggleButton.html(startText);
    $('#deltaSolution-modal pre.review').addClass('hide').html(reviewTxt);
}
function doShowReviewDialog(assignmentId, node) {
    $('#deltaSolution-modal pre').toggleClass('hide');
    var toggleButton = $('button.reviewButton');
    var oldText = toggleButton.html();
    var toggleValues = toggleButton.attr('text').split(',');
    var newText = oldText===toggleValues[1]?toggleValues[0]:toggleValues[1];
    toggleButton.html(newText);
}
