public class Assignment {

    public boolean run() {
        try {
            Thread.sleep(getPeriod());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private long getPeriod() {
        long sleep = 0L;
        try {
            sleep = Long.valueOf("{wait}");
        } catch (Exception e) {
            sleep = 0L;
        }
        return sleep;
    }
}
