package de.jplag.java.commentExtraction;

import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;

public class CustomScanner extends Scanner {
    public CustomScanner(ScannerFactory fac, JavaTokenizer tokenizer) {
        super(fac, tokenizer);
    }
}
