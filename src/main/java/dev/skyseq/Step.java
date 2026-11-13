package dev.skyseq;

/** One action in a macro sequence. */
public final class Step {

    public enum Type {
        COMMAND("Command"),
        KEY("Press key"),
        CLICK("Click"),
        ESCAPE("Escape"),
        WAIT("Wait"),
        WAIT_OPEN("Wait open"),
        WAIT_CLOSE("Wait close");

        public final String label;

        Type(String label) {
            this.label = label;
        }

        public Type next() {
            Type[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    public Type type = Type.COMMAND;
    /** Command/chat text, key name, mouse button, or menu-title filter, depending on type. "{n}" = cycle value. */
    public String text = "";
    /** Pause after this step (for the Wait type this is the wait itself). */
    public int delayMs = 150;

    public Step() {
    }

    public Step(Type type, String text, int delayMs) {
        this.type = type;
        this.text = text;
        this.delayMs = delayMs;
    }
}
