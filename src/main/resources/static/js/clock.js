function Clock(initialOffset) {
    this.offset = initialOffset || '440';
    this.current = 0;
    this.time = $('#assignment-clock').attr('data-time');
    this.finished = false;
    this.isPaused = false;
    this.soundPlayer = new SoundPlayer();
    this.ticksStart = -1;

    this.start = function () {
        let $assignmentClock = $('#assignment-clock');
        let $circle = $('.circle_animation', $assignmentClock);
        let timeleft = $assignmentClock.attr('data-time-left');
        let clock = this;

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
                const dashoff = clock.offset - (clock.current * (clock.offset / clock.time))
                $circle.css('stroke-dashoffset', dashoff > 0 ? dashoff : 0 )

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
        let interval = setInterval(() => {
            if (clock.finished || clock.current - clock.time >= 0) {
                clearInterval(interval);
                clock.current = clock.time
                renderTime();
            } else {
                if (!clock.isPaused) {
                    renderTime();
                    if( this.soundPlayer.isReady()) {
                        let remaining = clock.time - clock.current;
                        if (remaining > 0 && remaining <= 10) {
                            this.soundPlayer.playFastTicking()
                        } else if (remaining > 10 && remaining <= 30) {
                            this.soundPlayer.playSlowTicking()
                        } else if (remaining <= 0) {
                            this.soundPlayer.stopAll()
                        }
                    }
                }
                clock.current++;
            }
        }, 1000);
    };

    this.sync = function (remaining, total) {
        if (remaining > 1) {
            this.current = total - remaining;
        }
    };

    this.needsAudioAccess = function () {
        if( this.ticksStart > -1 ) {
            return (this.ticksStart - this.current) <= 60 && !this.soundPlayer.isReady();
        }
        return (this.time - this.current) <= 60 && !this.soundPlayer.isReady();
    }

    this.stop = function () {
        this.finished = true;
        this.soundPlayer.stopAll()
    };
    this.setPaused = function (isPaused) {
        this.isPaused = isPaused;
    };
    this.getPaused = function () {
        return this.isPaused;
    }

    this.playGong = () => {
        this.soundPlayer.playGong();
    }
    function SoundPlayer() {
        this.gong = null;
        this.fastTick = null;
        this.fastTickPlaying = false;
        this.slowTick = null;
        this.slowTickPlaying = false;
        this.ready = false;

        this.isReady = () => {
            return this.ready;
        }


        this.playGong = () => {
            if (this.ready) {
                this.fastTick.stop()
                this.slowTick.stop()
                this.gong.play()
            }
        }

        this.playSlowTicking = () => {
            if (this.ready && !this.slowTickPlaying) {
                this.slowTickPlaying = true;
                this.gong.stop()
                this.fastTick.stop()
                this.slowTick.play()
                this.fastTickPlaying = false;
            }
        }

        this.playFastTicking = () => {
            if (this.ready && !this.fastTickPlaying) {
                this.fastTickPlaying = true;
                this.gong.stop()
                this.slowTick.stop()
                this.fastTick.play()
                this.slowTickPlaying = false;
            }
        }

        this.stopAll = () => {
            this.slowTickPlaying = false;
            this.fastTickPlaying = false;
            if (this.ready) {
                Howler.stop();
            }
        }
        const initSounds = () => {
            if (window['Howl']) {
                this.gong = new Howl({src: ['/gong.wav']})
                this.fastTick = new Howl({src: ['/clock-tick.mp3'], loop: true, rate: 1.5})
                this.slowTick = new Howl({src: ['/clock-tick.mp3'], loop: true, rate: 0.7})
                this.ready = true;
            }
            window.removeEventListener("click", initSounds)
        }

        // can't use audio before a user event
        window.addEventListener("click", initSounds)
    }
}
