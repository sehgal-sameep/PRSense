package com.codewithsam.prsense.mcp.graph;

public enum EdgeType {
    EXTENDS,       // ClassA extends ClassB
    IMPLEMENTS,    // ClassA implements InterfaceB
    DEPENDS_ON,    // ClassA has a field of type ClassB (injection)
    CALLS,         // MethodA calls MethodB
    USES,          // ClassA references ClassB in a method signature
    RETURNS        // MethodA returns type ClassB
}
