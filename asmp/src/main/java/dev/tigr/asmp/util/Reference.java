package dev.tigr.asmp.util;

/**
 * @author Tigermouthbear 2/8/21
 * A reference to a method or field
 */
public class Reference {
    private final String owner;
    private final String name;
    private final String desc;
    private final boolean field;

    public Reference(String owner, String name, String desc, boolean field) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.field = field;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return "L" + owner + ";" + name + (field ? ":" : "") + desc;
    }

    public boolean isVoid() {
        return desc.endsWith("V");
    }
}
