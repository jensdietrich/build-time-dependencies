package bar;

// provides a field and a service
// contains some subtle changes from 1.0.0
public class Bar {

    // changes constant value - will affect constant pools downstream as this is inlined
    public static final String CONST = "this is a constant from bar-1.1.0";

    // changes signature - will affect callsites downstream
    public static String bar() {
        return "bar";
    }

}
