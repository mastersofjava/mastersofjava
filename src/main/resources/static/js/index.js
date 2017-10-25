function initializeCodeMirrors() {
	$('textarea[data-cm]').each(function(idx){
        var cm = CodeMirror.fromTextArea(this, {
            lineNumbers: true,
            mode: "text/x-java",
			matchBrackets: true,
			readOnly: $(this).attr('data-cm-readonly')
        });
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

$(document).ready(function(){
	initializeAssignmentClock();
	initializeCodeMirrors();
});
