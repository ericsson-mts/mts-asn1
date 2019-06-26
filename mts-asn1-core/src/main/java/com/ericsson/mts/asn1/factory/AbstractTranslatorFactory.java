/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.factory;

import com.ericsson.mts.asn1.translator.*;

public abstract class AbstractTranslatorFactory {

    public abstract AbstractBitStringTranslator bitStringTranslator();

    public abstract AbstractBooleanTranslator booleanTranslator();

    public abstract AbstractChoiceTranslator choiceTranslator();

    public abstract AbstractEnumeratedTranslator enumeratedTranslator();

    public abstract AbstractIntegerTranslator integerTranslator();

    public NullTranslator nullTranslator() {
        return new NullTranslator();
    }

    public abstract AbstractOctetStringTranslator octetStringTranslator();

    public abstract AbstractObjectClassFieldTranslator objectClassFieldTypeTranslator();

    public abstract AbstractRealTranslator realTranslator();

    public abstract AbstractRestrictedCharacterStringTranslator characterStringTranslator();

    public abstract AbstractSequenceOfTranslator sequenceOfTranslator();

    public abstract AbstractSequenceTranslator sequenceTranslator();

    public abstract AbstractObjectIdentifierTranslator objectIdentifierTranslator();
}
