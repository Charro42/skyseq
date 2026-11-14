package dev.skyseq;

/** One action in a macro sequence. */
public final class Step {

    public enum Type {
        COMMAND("Command", "type a /command or chat message"),
        KEY("Press key", "press a keyboard key, e.g. 1 or E"),
        CLICK("Click", "click a mouse button at the cursor"),
        ESCAPE("Escape", "close the open menu"),
        WAIT("Wait", "pause for the delay time"),
        WAIT_OPEN("Wait open", "wait until a menu opens"),
        WAIT_CLOSE("Wait close", "wait until the menu closes");

        public final String label;
        public final String help;

        Type(String label, String help) {
            this.label = label;
            this.help = help;
        }
    }

    public Type type = Type.COMMAND;
    /** Command/chat text, key name, mouse button, or menu-title filter, depending on type. "{n}" = cycle value. */
    public String text = "";
    /** Pause after this step (for the Wait type this is the wait itself). */
    public int delayMs = 150;
    /** Only run when n is one of these (comma-separated, e.g. "1,3"). Blank = always. */
    public String when = "";

    public Step() {
    }

    public Step(Type type, String text, int delayMs) {
        this.type = type;
        this.text = text;
        this.delayMs = delayMs;
    }

    /** Does this step run when n has the given value? */
    public boolean appliesTo(String value) {
        if (when == null || when.isBlank()) {
            return true;
        }
        for (String token : when.split(",")) {
            if (token.trim().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
