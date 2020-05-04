function Clock(initialOffset) {

    this.offset = initialOffset || '440';
    this.current = 0;
    this.time = $('#assignment-clock').attr('data-time');
    this.finished = false;

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

        var interval = setInterval(function () {
            if (clock.finished || clock.current - clock.time >= 0) {
                clearInterval(interval);
                return;
            } else {
                renderTime();
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
}
