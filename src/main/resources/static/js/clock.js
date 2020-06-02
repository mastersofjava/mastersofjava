function Clock(initialOffset) {

    this.offset = initialOffset || '440';
    this.current = 0;
    this.time = $('#assignment-clock').attr('data-time');
    this.finished = false;
    this.isPaused = false;

    this.start = function () {
        var $assignmentClock = $('#assignment-clock');
        var $circle = $('.circle_animation', $assignmentClock);
        var timeleft = $assignmentClock.attr('data-time-left');
        var clock = this;
        if (clock.finished) {
            timeleft = $('#content').attr('submittime');
        }
        // make sure it is rendered at least once in case this team has finished
        this.current = clock.time - timeleft;

        var isStarting = (100*this.current / clock.time)<1;
        if (isStarting) {
            doPlaySoundOnAssigmentStart();
        }
        renderTime();

        function renderTime() {
            var remaining = clock.time - clock.current;
            if (remaining >= 0) {
                var minutes = Math.floor(remaining / 60);
                var seconds = ("0" + remaining % 60).slice(-2);

                $('h2', $assignmentClock).text(minutes + ":" + seconds);
                $circle.css('stroke-dashoffset', clock.offset - ((clock.current + 1) * (clock.offset / clock.time)));

                var fraction = clock.current / clock.time;
                if (fraction > 0.5) {
                    if (fraction > 0.8) {
                        $circle.css('stroke', 'red');
                    } else {
                        $circle.css('stroke', 'orange');
                    }
                }
            }
        }
        // the countdown in the client (synchronized also from the server every 10 seconds)
        var interval = setInterval(function () {
            if (clock.finished || clock.current - clock.time >= 0) {
                clearInterval(interval);
                return;
            } else {
                if (!clock.isPaused) {
                    renderTime();

                    if (clock.current - clock.time===-15) {
                        doPlaySoundOnAssigmentLast15Seconds();
                    } else
                    if (clock.current - clock.time===-25) {
                        doPlaySoundOnAssigmentLast10SecondsBeforeLast15();
                    }

                }
            }
            clock.current++;
        }, 1000);
    };

    this.sync = function (remaining, total) {
        if (remaining > 1) {
            this.current = total - remaining;
        }
    };

    this.stop = function () {
        var $assignmentClock = $('#assignment-clock');
        this.finished = true;
        this.current = this.time;
        $('h2', $assignmentClock).text("0:00");
    };
    this.setPaused = function (isPaused) {
        this.isPaused = isPaused;
    };
    this.getPaused = function () {
        return this.isPaused;
    }
}

function doPlaySoundOnUserStatus(fileName) {
    if (!window['Howl']) {
        return;
    }
    howl=new Howl({urls: [fileName]});
    howl.play();
}
function doPlaySoundOnAssigmentStart() {
    doPlaySoundOnUserStatus('/gong.wav');
}
function doPlaySoundOnAssigmentLast10SecondsBeforeLast15() {
    doPlaySoundOnUserStatus('/tictac2.wav');
}
function doPlaySoundOnAssigmentLast15Seconds() {
    doPlaySoundOnUserStatus('/tikking.wav');
}
