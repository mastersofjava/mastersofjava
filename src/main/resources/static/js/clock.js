function Clock( initialOffset ) {

    this.offset = initialOffset || '440';
    this.current = 0;

    this.start = function () {
        var $assignmentClock = $('#assignment-clock');
        var $circle = $('.circle_animation', $assignmentClock);
        var time = $assignmentClock.attr('data-time');
        var timeleft = $assignmentClock.attr('data-time-left');
        var finished = $('#content').attr('finished') === 'true';
        var clock = this;
        if (finished) {
            timeleft = $('#content').attr('submittime');
        }
        // make sure it is rendered at least once in case this team has finished
        this.current = time - timeleft;
        renderTime();

        function renderTime() {
            var remaining = time - clock.current;
            if (remaining >= 0) {
                var minutes = Math.floor(remaining / 60);
                var seconds = ("0" + remaining % 60).slice(-2);
                $('h2', $assignmentClock).text(minutes + ":" + seconds);
                $circle.css('stroke-dashoffset', clock.offset - ((clock.current + 1) * (clock.offset / time)));

                var fraction = clock.current / time;
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
            if (finished || clock.current - time >= 0) {
                clearInterval(interval);
                return;
            } else {
                renderTime();
            }
            clock.current++;
        }, 1000);
    };

    this.sync = function( remaining, total ) {
        this.current = total - remaining;
    };
}