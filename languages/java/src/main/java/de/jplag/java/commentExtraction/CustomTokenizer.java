package de.jplag.java.commentExtraction;

import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.parser.UnicodeReader;

import java.nio.CharBuffer;

public class CustomTokenizer extends JavaTokenizer {
    public CustomTokenizer(ScannerFactory fac, CharBuffer cb) {
        super(fac, cb);
    }
    public CustomTokenizer(ScannerFactory fac, char[] array, int length) {
        super(fac, array, length);
    }

    @Override
    protected Tokens.Comment processComment(int pos, int endPos, Tokens.Comment.CommentStyle style) {
        return new AdvancedComment(style, this, pos, endPos);
    }

    protected static class AdvancedComment extends BasicComment {
        /**
         * Constructor.
         *
         * @param cs     comment style
         * @param reader existing reader
         * @param pos    start of meaningful content in buffer.
         * @param endPos end of meaningful content in buffer.
         */
        protected AdvancedComment(CommentStyle cs, UnicodeReader reader, int pos, int endPos) {
            super(cs, reader, pos, endPos);
        }

        @Override
        public String getText() {
            return this.getRawString();
        }
    }
}
