package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.TokenType;


public final class Token {
    public Token(final TokenType type) {
        mType = type;
        mStringValue = "";
        mIntValue = 0;
        mDoubleValue = 0;
    }

    public Token(final TokenType type, final String value) {
        mType = type;
        mStringValue = value;
        mIntValue = 0;
        mDoubleValue = 0;
    }

    public Token(final TokenType type, final double value) {
        mType = type;
        mStringValue = "";
        mIntValue = 0;
        mDoubleValue = value;
    }

    public Token(final TokenType type, final int value) {
        mType = type;
        mStringValue = "";
        mIntValue = value;
        mDoubleValue = 0;
    }

    public TokenType getType() { return mType; }
    
    public String getString() { return mStringValue; }
    public int    getInt()    { return mIntValue; }
    public double getDouble() { return mDoubleValue; }
    
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

            case NAME: return mStringValue + " (name)";
            case OPERATOR: return mStringValue + " (op)";

            case INT: return Integer.toString(mIntValue);
            case DOUBLE: return Double.toString(mDoubleValue);
            case STRING: return "\"" + mStringValue + "\"";

            case EOF: return "(eof)";

            default: return "(unknown token?!)";
        }
    }
    
    private final TokenType mType;
    private final String mStringValue;
    private final int    mIntValue;
    private final double mDoubleValue;
}
