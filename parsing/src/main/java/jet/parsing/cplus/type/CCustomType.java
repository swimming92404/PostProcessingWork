package jet.parsing.cplus.type;

import jet.parsing.cplus.reader.CReader;

/**
 * This class represents the type of c++ language.
 */
public class CCustomType implements CType{
    /** The Type name. e.g "int", "float" */
    protected String mName;

    protected int mSize;

    public CCustomType(CReader reader) {
//        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public int getSize() {
        return mSize;
    }

    public boolean isPrimitive() {
        return false;
    }
}
