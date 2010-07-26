package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.TokenType;


public final class Token {
    public Token(final TokenType type) {
        mType = type;
        mValue = null;
    }

    public Token(final TokenType type, final Object value) {
        mType = type;
        mValue = value;
    }

    public TokenType getType() { return mType; }
    
    public boolean getBool()   { return ((Boolean)mValue).booleanValue(); }
    public int     getInt()    { return ((Integer)mValue).intValue(); }
    public double  getDouble() { return ((Double)mValue).doubleValue(); }
    public String  getString() { return (String)mValue; }
    
    public String toString() {
        switch (mType)
        {
            case LEFT_PAREN: return "(";
            case RIGHT_PAREN: return ")";
            case LEFT_BRACKET: return "[";
            case RIGHT_BRACKET: return "]";
            case LEFT_BRACE: return "{";
            case RIGHT_BRACE: return "}";
            case COMMA: return ",";
            case LINE: return "(line)";
            case DOT: return ".";

            case NAME: return getString() + " (name)";
            case OPERATOR: return getString() + " (op)";

            case BOOL: return Boolean.toString(getBool());
            case INT: return Integer.toString(getInt());
            case DOUBLE: return Double.toString(getDouble());
            case STRING: return "\"" + getString() + "\"";

            case EOF: return "(eof)";

            default: return "(unknown token?!)";
        }
    }
    
    private final TokenType mType;
    private final Object    mValue;
}
