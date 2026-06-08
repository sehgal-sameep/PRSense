package com.codewithsam.prsense.mcp.graph;

import com.codewithsam.prsense.constants.SymbolType;

public enum NodeType {
    CLASS, INTERFACE, METHOD, CONSTRUCTOR, ENUM, RECORD;

    public static NodeType from(SymbolType symbolType) {
        return switch (symbolType) {
            case CLASS       -> CLASS;
            case INTERFACE   -> INTERFACE;
            case METHOD      -> METHOD;
            case CONSTRUCTOR -> CONSTRUCTOR;
            case ENUM        -> ENUM;
            case RECORD      -> RECORD;
        };
    }
}
