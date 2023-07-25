package dan200.computercraft.api.lua;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;


public class UTFString {
    private final String value;
    private final int[] codepoints;

    public static final UTFString EMPTY_STRING = new UTFString("");

    public UTFString(String value){
        this.value = value;
        this.codepoints = value.codePoints().toArray();
    }

    @LuaFunction("len")
    public final int getLength(){
        return this.codepoints.length;
    }

    private static int correctPos(int pos, int len){
        if(-pos > len) return 0;
        if(pos < 0) return len + pos + 1;
        return pos;
    }

    @LuaFunction("sub")
    public final UTFString substring(IArguments args) throws LuaException {
        var i = correctPos(args.getInt(1), getLength());
        var j = correctPos(args.optInt(2, -1), getLength());
        if(i < 1) i = 1;
        if (j > getLength()) j = getLength();
        if(i <= j){

            return new UTFString(new String(codepoints, i - 1, j - i + 1));
        }
        return EMPTY_STRING;
    }

    @LuaFunction("byte")
    public final MethodResult getCodepoints(IArguments args) throws LuaException {
        var i = correctPos(args.optInt(1, 1), getLength());
        var j = correctPos(args.optInt(2, i), getLength());
        if(i < 1) i = 1;
        if (j > getLength()) j = getLength();
        if (i == j) return MethodResult.of(codepoints[i - 1]);
        if (i > j) return MethodResult.of();
        var n = j - i + 1;
        var cps = new Integer[n];
        for (int i1 = 0; i1 < n; i1++){
            cps[i1] = codepoints[i + i1 - 1];
        }
        return MethodResult.of((Object[]) cps);
    }

    @LuaFunction
    public final String toAnsi(){
        return new String(this.value.codePoints().map(cp -> cp > 255 ? 63 : cp).toArray(), 0, getLength());
    }

    @LuaFunction
    public final UTFString format(IArguments args) throws LuaException {
        var builder = new StringBuilder();
        var len = getLength();
        var arg = 0;
        for (int i = 0; i < len; ) {
            int cp = codepoints[i++];
            if(cp != '%'){
                builder.appendCodePoint(cp);
                continue;
            }
            if (i >= len) throw new LuaException("invalid option '%' to 'format'");

            if(codepoints[i] == '%'){
                i++;
                builder.append('%');
                continue;
            }

            arg++;
            if(arg > args.count()) throw new LuaException(String.format("bad argument #%d to 'format' (no value)", arg));
            boolean leftAdjust = false, explicitPlus = false, space = false, alternateForm = false, zeroPad = false;
            StringBuilder formatter = new StringBuilder("%");
            boolean moreFlags = true;
            while(moreFlags){
                switch (cp = (i < len) ? codepoints[i++] : 0){
                    case '-', '+', ' ', '#', '0' -> formatter.appendCodePoint(cp);
                    default -> moreFlags = false;
                }
            }

            if(Character.isDigit(cp)){
                formatter.appendCodePoint(cp);
                cp = (i < len) ? codepoints[i++] : 0;
                if (Character.isDigit(cp)) {
                    formatter.appendCodePoint(cp);
                    cp = (i < len) ? codepoints[i++] : 0;
                }
            }

            if(cp == '.'){
                formatter.appendCodePoint(cp);
                cp = (i < len) ? codepoints[i++] : 0;
                if (Character.isDigit(cp)) {
                    formatter.appendCodePoint(cp);
                    cp = (i < len) ? codepoints[i++] : 0;
                    if (Character.isDigit(cp)) {
                        formatter.appendCodePoint(cp);
                        cp = (i < len) ? codepoints[i++] : 0;
                    }
                }
            }

            if (Character.isDigit(cp)) {
                throw new LuaException("invalid format (width or precision too long)");
            }

            switch (cp){
                case 'c' -> builder.append(args.getInt(arg));
                case 'i', 'd', 'o', 'u', 'x', 'X' -> {
                    var number = args.getLong(arg);
                    builder.append(String.format(formatter.appendCodePoint(cp).toString(), number));
                }
                case 'e', 'E', 'f', 'g', 'G' -> {
                    var number = args.getDouble(arg);
                    builder.append(String.format(formatter.appendCodePoint(cp).toString(), number));
                }
                case 'q' -> {
                    var obj = args.get(arg);
                    if(obj instanceof String s) addQuoted(builder, s);
                    else if(obj instanceof Number number){
                        double value = number.doubleValue();
                        builder.append((long) value == value ? Long.toString((long) value) : Double.toHexString(value));
                    }
                    else if(obj instanceof Boolean flag){
                        builder.append(flag);
                    }
                    else if(obj == null){
                        builder.append("nil");
                    }
                    else{
                        throw new LuaException(String.format("bad argument #%d (value has no literal representation)", arg));
                    }
                }
                case 's' -> {
                    var s = args.getStringCoerced(arg);
                    builder.append(String.format(formatter.appendCodePoint(cp).toString(), s));
                }
                default -> throw new LuaException("invalid option '%" + (char) cp + "' to 'format'");
            }
        }
        return new UTFString(builder.toString());
    }

    @LuaFunction("rep")
    public final UTFString repeat(IArguments args) throws LuaException {
        var n = args.getInt(1);
        var sep = args.get(2);
        if(sep == null){
            return new UTFString(value.repeat(n));
        }
        var strings = new String[n];
        Arrays.fill(strings, value);
        String delimiter;
        if(sep instanceof UTFString uStr) delimiter = uStr.value;
        else if(sep instanceof String str) delimiter = str;
        else throw LuaValues.badArgumentOf(args, 2, "string or UTFString");
        return new UTFString(String.join(delimiter, strings));
    }

    @LuaFunction("lower")
    public final UTFString toLowerCase(){
        return new UTFString(value.toLowerCase(Locale.ROOT));
    }

    @LuaFunction("upper")
    public final UTFString toUpperCase(){
        return new UTFString(value.toUpperCase(Locale.ROOT));
    }

    @LuaFunction
    public final UTFString reverse(){
        var cps = new int[codepoints.length];
        for (int i = 0; i < codepoints.length; i++) {
            cps[i] = codepoints[codepoints.length - i - 1];
        }
        return new UTFString(new String(cps, 0, cps.length));
    }

    @LuaFunction
    public final MethodResult find(IArguments args) throws LuaException {
        var substr = args.get(1);
        var init = args.optInt(2, 1);
        var plain = args.optBoolean(3, false);
        if (substr instanceof UTFString uStr) return _find(uStr.value, init, plain, true);
        else if(substr instanceof String str) return _find(str, init, plain, true);
        throw LuaValues.badArgumentOf(args, 1, "string or UTFString");
    }

    @LuaFunction
    public final MethodResult match(IArguments args) throws LuaException {
        var pattern = args.get(1);
        var init = args.optInt(2, 1);
        if (pattern instanceof UTFString uStr) return _find(uStr.value, init, false, false);
        else if(pattern instanceof String str) return _find(str, init, false, false);
        throw LuaValues.badArgumentOf(args, 1, "string or UTFString");
    }

    @LuaFunction
    public final ILuaFunction gmatch(IArguments args) throws LuaException {
        var patternObj = args.get(1);
        String pattern;
        if (patternObj instanceof UTFString uStr) pattern = uStr.value;
        else if(patternObj instanceof String str) pattern = str;
        else throw LuaValues.badArgumentOf(args, 1, "string or UTFString");
        return new ILuaFunction() {

            Matcher matcher = new Matcher(value, pattern);
            int srclen = UTFString.this.getLength();
            int strOffset = 0;
            @Override
            public MethodResult call(IArguments args1) throws LuaException {
                for (; strOffset < srclen; strOffset++) {
                    matcher.reset();
                    int res = matcher.match(strOffset, 0);
                    if (res >= 0) {
                        int soff = strOffset;
                        strOffset = res;
                        if (res == soff) strOffset++;
                        return matcher.getCaptures(true, soff, res);
                    }
                }
                return MethodResult.of();
            }
        };
    }

    @LuaFunction
    public final MethodResult gsub(IArguments args) throws LuaException {
        var patternObj = args.get(1);
        String pattern;
        if(patternObj instanceof UTFString uStr) pattern = uStr.value;
        else if(patternObj instanceof String str) pattern = str;
        else throw LuaValues.badArgumentOf(args, 1, "string or UTFString");
        var repl = args.get(2);
        var maxRepl = args.optInt(3, getLength() + 1);

        final boolean anchor = pattern.startsWith("^");
        StringBuilder builder = new StringBuilder();
        Matcher matcher = new Matcher(value, pattern);
        int strOffset = 0;
        int i = 0;
        while(i < maxRepl){
            matcher.reset();
            int res;
            res = matcher.match(strOffset, anchor ? 1 : 0);
            if (res != -1) {
                i++;
                matcher.add_value(builder, strOffset, res, repl);
            }
            if (res != -1 && res > strOffset) {
                strOffset = res;
            } else if (strOffset < getLength()) {
                builder.appendCodePoint(codepoints[strOffset++]);
            } else {
                break;
            }
            if (anchor) {
                break;
            }
        }
        builder.append(new String(codepoints, strOffset, getLength() - strOffset));
        return MethodResult.of(new UTFString(builder.toString()), i);
    }

    @LuaFunction("tostring")
    public final String toUtf8String(){
        return String.valueOf(StandardCharsets.ISO_8859_1.decode(StandardCharsets.UTF_8.encode(value)));
    }

    private static void addQuoted(StringBuilder builder, String s) {
        char c;
        builder.append('"');
        for (int i = 0, n = s.length(); i < n; i++) {
            switch (c = s.charAt(i)) {
                case '"', '\\', '\n' -> {
                    builder.append('\\');
                    builder.append(c);
                }
                case '\r' -> builder.append("\\r");
                case '\0' -> builder.append("\\000");
                default -> builder.append(c);
            }
        }
        builder.append('"');
    }

    private MethodResult _find(String pattern, int init, boolean plain, boolean firstMatch) throws LuaException {
        init = Math.min(correctPos(init, getLength()), getLength()) - 1;
        var simpleMatch = true;
        if (!plain) {
            for(var c : "^$*+?.([%-".toCharArray()){
                if(pattern.indexOf(c) != -1){
                    simpleMatch = false;
                    break;
                }
            }
        }
        if(simpleMatch){
            int result = value.indexOf(pattern, init);
            if(result != -1){
                return MethodResult.of(result + 1, result + pattern.codePointCount(0, pattern.length()));
            }
        }
        else{
            var matcher = new Matcher(value, pattern);

            boolean anchor = false;
            int patOffset = 0;
            if(pattern.startsWith("^")){
                anchor = true;
                patOffset = 1;
            }

            int strOffset = init;
            do{
                int res;
                matcher.reset();
                if ((res = matcher.match(strOffset, patOffset)) != -1) {
                    if (firstMatch) {
                        var captures = matcher.getCaptures(false, strOffset, res);
                        if(captures.getResult() == null){
                            return MethodResult.of(strOffset + 1, res);
                        }
                        else{
                            Object[] result = new Object[captures.getResult().length + 2];
                            result[0] = strOffset + 1;
                            result[1] = res;
                            System.arraycopy(captures.getResult(), 0, result, 2, captures.getResult().length);
                            return MethodResult.of(result);
                        }
                    } else {
                        return matcher.getCaptures(true, strOffset, res);
                    }
                }
            } while (strOffset++ < getLength() && !anchor);
        }
        return MethodResult.of();
    }

    @Override
    public String toString() {
        return value;
    }

    private static class Matcher{
        private static final int MAX_CAPTURES = 32;
        private static final int CAP_UNFINISHED = -1;
        private static final int CAP_POSITION = -2;

        private final String str;
        private final int[] strCps;
        private final String pattern;
        private final int[] patCps;

        private int level = 0;
        private int[] capturePos = new int[MAX_CAPTURES];
        private int[] captureLen = new int[MAX_CAPTURES];

        Matcher(String str, String pattern){
            this.str = str;
            this.pattern = pattern;
            this.strCps = this.str.codePoints().toArray();
            this.patCps = this.pattern.codePoints().toArray();
        }

        public void reset(){
            this.level = 0;
        }

        private void add_s(StringBuilder builder, String news, int strOffset, int e) throws LuaException {
            int l = news.length();
            for (int i = 0; i < l; ++i) {
                char b = news.charAt(i);
                if (b != '%') {
                    builder.append(b);
                } else {
                    ++i; // skip ESC
                    b = i < l ? news.charAt(i) : 0;
                    if (!Character.isDigit(b)) {
                        builder.append(b);
                    } else if (b == '0') {
                        builder.append(new String(strCps, strOffset, e - strOffset));
                    } else {
                        Object value = getCapture(b - '1', strOffset, e);
                        builder.append(value);
                    }
                }
            }
        }

        public void add_value(StringBuilder builder, int strOffset, int end, @Nullable Object repl) throws LuaException {
            Object replace;
            if(repl instanceof String str){
                add_s(builder, str, strOffset, end);
                return;
            }
            else if(repl instanceof UTFString uStr){
                add_s(builder, uStr.value, strOffset, end);
                return;
            }
            else if(repl instanceof Number num){
                add_s(builder, num.toString(), strOffset, end);
                return;
            }
            else if(repl instanceof Map<?,?> map){
                replace = map.get(getCapture(0, strOffset, end));
            }
            else{
                throw new LuaException("bad argument: string/table expected");
            }

            finishAddValue(builder, strOffset, end, replace);
        }

        public void finishAddValue(StringBuilder lbuf, int strOffset, int end, Object repl) throws LuaException {
            if(repl instanceof Boolean flag && !flag){
                lbuf.append(new String(strCps, strOffset, end - strOffset));
            }
            else if(repl instanceof String str){
                lbuf.append(str);
            }
            else if(repl instanceof UTFString uStr){
                lbuf.append(uStr.value);
            }
            else{
                throw new LuaException("invalid replacement value (a " + LuaValues.getType(repl) + ")");
            }
        }

        MethodResult getCaptures(boolean wholeMatch, int strOffset, int end) throws LuaException {
            int nlevels = (this.level == 0 && wholeMatch) ? 1 : this.level;
            switch (nlevels) {
                case 0 -> {
                    return MethodResult.of();
                }
                case 1 -> {
                    return MethodResult.of(getCapture(0, strOffset, end));
                }
            }
            Object[] v = new Object[nlevels];
            for (int i = 0; i < nlevels; ++i) {
                v[i] = getCapture(i, strOffset, end);
            }
            return MethodResult.of(v);
        }

        private Object getCapture(int captIdx, int strOffset, int end) throws LuaException {
            if (captIdx >= this.level) {
                if (captIdx == 0) {
                    return new UTFString(new String(strCps, strOffset, end - strOffset));
                } else {
                    throw new LuaException("invalid capture index");
                }
            } else {
                int l = captureLen[captIdx];
                if (l == CAP_UNFINISHED) {
                    throw new LuaException("unfinished capture");
                }
                if (l == CAP_POSITION) {
                    return capturePos[captIdx] + 1;
                } else {
                    int begin = capturePos[captIdx];
                    return new UTFString(new String(strCps, begin, l));
                }
            }
        }

        private int checkCapture(int l) throws LuaException {
            l -= '1';
            if (l < 0 || l >= level || this.captureLen[l] == CAP_UNFINISHED) {
                throw new LuaException("invalid capture index");
            }
            return l;
        }

        private int captureToClose() throws LuaException {
            int level = this.level;
            for (level--; level >= 0; level--) {
                if (captureLen[level] == CAP_UNFINISHED) {
                    return level;
                }
            }
            throw new LuaException("invalid pattern capture");
        }

        int classEnd(int patOffset) throws LuaException {
            switch (patCps[patOffset++]) {
                case '%' -> {
                    if (patOffset == patCps.length) {
                        throw new LuaException("malformed pattern (ends with %)");
                    }
                    return patOffset + 1;
                }
                case '[' -> {
                    if (patOffset == patCps.length) throw new LuaException("malformed pattern (missing ']')");
                    if (patCps[patOffset] == '^') {
                        patOffset++;
                        if (patOffset == patCps.length) throw new LuaException("malformed pattern (missing ']')");
                    }
                    do {
                        if (patCps[patOffset++] == '%' && patOffset < patCps.length) patOffset++;
                        if (patOffset == patCps.length) throw new LuaException("malformed pattern (missing ']')");
                    } while (patCps[patOffset] != ']');
                    return patOffset + 1;
                }
                default -> {
                    return patOffset;
                }
            }
        }

        static boolean matchClass(int c, int classChar) {
            final int lcl = Character.toLowerCase(classChar);
            boolean res;
            switch (lcl) {
                case 'a' -> res = Character.isLetter(c);
                case 'd' -> res = Character.isDigit(c);
                case 'l' -> res = Character.isLowerCase(c);
                case 'u' -> res = Character.isUpperCase(c);
                case 'c' -> res = Character.isISOControl(c);
                case 'p' -> {
                    var charType = Character.getType(c);
                    res = charType == Character.DASH_PUNCTUATION ||
                        charType == Character.START_PUNCTUATION ||
                        charType == Character.END_PUNCTUATION ||
                        charType == Character.CONNECTOR_PUNCTUATION ||
                        charType == Character.OTHER_PUNCTUATION ||
                        charType == Character.INITIAL_QUOTE_PUNCTUATION ||
                        charType == Character.FINAL_QUOTE_PUNCTUATION;
                }
                case 's' -> res = Character.isSpaceChar(c);
                case 'w' -> res = Character.isLetterOrDigit(c);
                case 'x' -> res = '0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F' ||
                    0xff10 <= c && c <= 0xff19 || 0xff21 <= c && c <= 0xff26 || 0xff41 <= c && c <= 0xff46;
                case 'z' -> res = (c == 0);
                default -> {
                    return classChar == c;
                }
            }
            return (lcl == classChar) == res;
        }

        boolean matchBracketClass(int c, int poff, int ec) {
            boolean sig = true;
            if (patCps[poff + 1] == '^') {
                sig = false;
                poff++;
            }
            while (++poff < ec) {
                if (patCps[poff] == '%') {
                    poff++;
                    if (matchClass(c, patCps[poff])) {
                        return sig;
                    }
                } else if ((patCps[poff + 1] == '-') && (poff + 2 < ec)) {
                    poff += 2;
                    if (patCps[poff - 2] <= c && c <= patCps[poff]) {
                        return sig;
                    }
                } else if (patCps[poff] == c) return sig;
            }
            return !sig;
        }

        boolean singleMatch(int c, int patOffset, int ep) {
            return switch (patCps[patOffset]) {
                case '.' -> true;
                case '%' -> matchClass(c, patCps[patOffset + 1]);
                case '[' -> matchBracketClass(c, patOffset, ep - 1);
                default -> patCps[patOffset] == c;
            };
        }

        int match(int strOffset, int patOffset) throws LuaException {
            while(true){
                if (patOffset == patCps.length) {
                    return strOffset;
                }
                switch (patCps[patOffset]){
                    case '(' -> {
                        if (++patOffset < patCps.length && patCps[patOffset] == ')') {
                            return startCapture(strOffset, patOffset + 1, CAP_POSITION);
                        } else {
                            return startCapture(strOffset, patOffset, CAP_UNFINISHED);
                        }
                    }
                    case ')' -> {
                        return endCapture(strOffset, patOffset + 1);
                    }
                    case '%' -> {
                        if (patOffset + 1 == patCps.length) {
                            throw new LuaException("malformed pattern (ends with '%')");
                        }
                        switch (patCps[patOffset + 1]) {
                            case 'b' -> {
                                strOffset = matchBalance(strOffset, patOffset + 2);
                                if (strOffset == -1) return -1;
                                patOffset += 4;
                                continue;
                            }
                            case 'f' -> {
                                patOffset += 2;
                                if (patOffset == patCps.length || patCps[patOffset] != '[') {
                                    throw new LuaException("missing '[' after '%f' in pattern");
                                }
                                int ep = classEnd(patOffset);
                                int previous = (strOffset == 0) ? 0 : strCps[strOffset - 1];
                                if (matchBracketClass(previous, patOffset, ep - 1) || (strOffset < strCps.length && !matchBracketClass(strCps[strOffset], patOffset, ep - 1))) {
                                    return -1;
                                }
                                patOffset = ep;
                                continue;
                            }
                            default -> {
                                int c = patCps[patOffset + 1];
                                if (Character.isDigit(c)) {
                                    strOffset = matchCapture(strOffset, c);
                                    if (strOffset == -1) {
                                        return -1;
                                    }
                                    return match(strOffset, patOffset + 2);
                                }
                            }
                        }
                    }
                    case '$' -> {
                        if (patOffset + 1 == patCps.length) {
                            return (strOffset == strCps.length) ? strOffset : -1;
                        }
                    }
                }
                int ep = classEnd(patOffset);
                boolean m = strOffset < strCps.length && singleMatch(strCps[strOffset], patOffset, ep);
                int pc = (ep < patCps.length) ? patCps[ep] : '\0';
                switch (pc) {
                    case '?' -> {
                        int res;
                        if (m && ((res = match(strOffset + 1, ep + 1)) != -1)) {
                            return res;
                        }
                        patOffset = ep + 1;
                    }
                    case '*' -> {
                        return maxExpand(strOffset, patOffset, ep);
                    }
                    case '+' -> {
                        return (m ? maxExpand(strOffset + 1, patOffset, ep) : -1);
                    }
                    case '-' -> {
                        return minExpand(strOffset, patOffset, ep);
                    }
                    default -> {
                        if (!m) {
                            return -1;
                        }
                        strOffset++;
                        patOffset = ep;
                    }
                }
            }
        }

        int maxExpand(int strOffset, int patOffset, int ep) throws LuaException {
            int i = 0;
            while (strOffset + i < strCps.length &&
                singleMatch(strCps[strOffset + i], patOffset, ep)) {
                i++;
            }
            while (i >= 0) {
                int res = match(strOffset + i, ep + 1);
                if (res != -1) {
                    return res;
                }
                i--;
            }
            return -1;
        }

        int minExpand(int strOffset, int patOffset, int ep) throws LuaException {
            for (; ; ) {
                int res = match(strOffset, ep + 1);
                if (res != -1) {
                    return res;
                } else if (strOffset < strCps.length && singleMatch(strCps[strOffset], patOffset, ep)) {
                    strOffset++;
                } else {
                    return -1;
                }
            }
        }

        int startCapture(int strOffset, int patOffset, int sp) throws LuaException {
            int res;
            int level = this.level;
            if (level >= MAX_CAPTURES) {
                throw new LuaException("too many captures");
            }
            capturePos[level] = strOffset;
            captureLen[level] = sp;
            this.level = level + 1;
            if ((res = match(strOffset, patOffset)) == -1) {
                this.level--;
            }
            return res;
        }

        int endCapture(int strOffset, int patOffset) throws LuaException {
            int l = captureToClose();
            int res;
            captureLen[l] = strOffset - capturePos[l];
            if ((res = match(strOffset, patOffset)) == -1) {
                captureLen[l] = CAP_UNFINISHED;
            }
            return res;
        }

        int matchCapture(int strOffset, int l) throws LuaException {
            l = checkCapture(l);
            int len = captureLen[l];
            if ((strCps.length - strOffset) >= len &&
                Arrays.equals(strCps, capturePos[l], capturePos[l] + len, strCps, strOffset, strOffset + l)) {
                return strOffset + len;
            } else {
                return -1;
            }
        }

        int matchBalance(int strOffset, int patOffset) throws LuaException {
            final int plen = patCps.length;
            if (patOffset == plen || patOffset + 1 == plen) {
                throw new LuaException("unbalanced pattern");
            }
            if (strOffset >= strCps.length || strCps[strOffset] != patCps[patOffset]) {
                return -1;
            } else {
                int b = patCps[patOffset];
                int e = patCps[patOffset + 1];
                int cont = 1;
                while (++strOffset < strCps.length) {
                    if (strCps[strOffset] == e) {
                        if (--cont == 0) return strOffset + 1;
                    } else if (strCps[strOffset] == b) cont++;
                }
            }
            return -1;
        }
    }
}
