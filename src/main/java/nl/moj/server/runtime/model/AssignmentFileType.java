package nl.moj.server.runtime.model;

public enum AssignmentFileType {
    TEST(true),
    HIDDEN_TEST(false),
    EDIT(true),
    READONLY(true),
    HIDDEN(false),
    TASK(true),
    SOLUTION(false),
    RESOURCE(true),
    TEST_RESOURCE(true),
    HIDDEN_TEST_RESOURCE(false);

    private boolean visible;

    AssignmentFileType(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return this.visible;
    }
}
