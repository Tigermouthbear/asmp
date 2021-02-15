package dev.tigr.asmp.util;

/**
 * @author Tigermouthbear 2/8/21
 * A reference to a method or field
 * field has no desc
 */
public class Reference {
    private String owner;
    private String name;
    private String desc;
    private final boolean field;

    public Reference(String owner, String name) {
        this.owner = owner;
        this.name = name;
        this.desc = null;
        this.field = true;
    }

    public Reference(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.field = false;
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

    public void setOwner(String value) {
        this.owner = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "L" + owner + ";" + name + (field ? "" : desc);
    }

    public boolean isVoid() {
        return desc.endsWith("V");
    }
}
