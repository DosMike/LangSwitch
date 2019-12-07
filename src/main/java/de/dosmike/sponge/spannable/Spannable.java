package de.dosmike.sponge.spannable;

import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is a class to work with Text in a String-like fashion.<br>
 * I mainly wrote this because I don't understand TextTemplate
 * (yay for API 8 https://github.com/SpongePowered/SpongeAPI/issues/1891)
 * and Text.replace(Pattern, Text) does not seem to be implemented.<br>
 * This class is heavily inspired by String and the Android Spannables:
 * The text is always stored as plain, and has multiple spans anchored to
 * char-positions. Although being a flat structure compared to the vanilla
 * json representation it allows for simple addition of styles, colors and
 * actions, without resulting in weired coloring.<br>
 * Turning the spannable into a Text object creates flat segments along points
 * of interest (where a span starts or ends) that individually apply the spans
 * information (color, style, text actions).<br>
 * While this could be easily expanded by translation / placeholder spans i hope
 * Sponge comes up with something better for API 8.<br>
 * Quick note to the Sponge guys: sorry
 */
public class Spannable implements CharSequence {

    private static final Map<Object, Character> formats = new HashMap<>();
    static {
        formats.put(TextColors.BLACK, '0');
        formats.put(TextColors.DARK_BLUE, '1');
        formats.put(TextColors.DARK_GREEN, '2');
        formats.put(TextColors.DARK_AQUA, '3');
        formats.put(TextColors.DARK_RED, '4');
        formats.put(TextColors.DARK_PURPLE, '5');
        formats.put(TextColors.GOLD, '6');
        formats.put(TextColors.GRAY, '7');
        formats.put(TextColors.DARK_GRAY, '8');
        formats.put(TextColors.BLUE, '9');
        formats.put(TextColors.GREEN, 'a');
        formats.put(TextColors.AQUA, 'b');
        formats.put(TextColors.RED, 'c');
        formats.put(TextColors.LIGHT_PURPLE, 'd');
        formats.put(TextColors.YELLOW, 'e');
        formats.put(TextColors.WHITE, 'f');
        formats.put(TextColors.RESET, 'r');
        formats.put(TextStyles.OBFUSCATED, 'k');
        formats.put(TextStyles.BOLD, 'l');
        formats.put(TextStyles.STRIKETHROUGH, 'm');
        formats.put(TextStyles.UNDERLINE, 'n');
        formats.put(TextStyles.ITALIC, 'o');
    }

    private String plain;
    private Set<Span> spans = new HashSet<Span>();

    /**
     * private constructor for modified spannables
     */
    private Spannable(String plainText, Set<Span> spans) {
        this.plain = plainText;
        this.spans = spans;
        for (Span span : this.spans) {
            if (span.start()<0 || span.end()>length())
                throw new IllegalStateException("Span out of bounds");
        }
    }

    /**
     * private copy constructor
     */
    private Spannable(Spannable spannable) {
        this.plain = spannable.plain;
        for (Span s : spannable.spans)
            this.spans.add(s.copy());
    }

    /**
     * Get the mutable collection of spans currently applying to this Spannable.
     * All span manipulations work with side-effects.
     * @return all spans for this Spannable.
     */
    public Collection<Span> getAllSpans() {
        return spans;
    }

    /**
     * Adds a collection of spans to this Spannable.
     * All span manipulations work with side-effects.
     * @param spans the spans to add
     * @return true if the span was added
     * @see Set#addAll(Collection)
     */
    public boolean addSpans(Span... spans) {
        return this.spans.addAll(Arrays.asList(spans));
    }
    /**
     * Adds a collection of spans to this Spannable.
     * All span manipulations work with side-effects.
     * @param spans the spans to add
     * @return true if the span was added
     * @see Set#addAll(Collection)
     */
    public boolean addSpans(Collection<Span> spans) {
        return this.spans.addAll(spans);
    }

    /**
     * Remove all spans in the specified region.
     * Contained spans will be removed, partially contained spans
     * will be cut at the borders.
     * All span manipulations work with side-effects.
     * @param from first position (inclusive)
     * @param to last position (exclusive)
     */
    public void stripSpans(int from, int to) {
        HashSet<Span> scanList = new HashSet<>(spans);
        for (Span s : scanList) {
            if (s.containedWithin(from, to)) {
                spans.remove(s);
            } else if (s.startsWithin(from, to)) {
                s.setStart(to);
            } else if (s.endsWithin(from, to)) {
                s.setEnd(from);
            } else if (s.containsRange(from, to)) {
                Span rightCopy = s.copy();
                s.setEnd(from);
                rightCopy.setStart(to);
                spans.add(rightCopy);
            }
        }
    }
    /**
     * Remove all specified spans from this Spannable.
     * All span manipulations work with side-effects.
     * @param spans the spans to remove
     * @return true if at least one span was present
     * @see Set#removeAll(Collection)
     */
    public boolean removeSpans(Span... spans) {
        return this.spans.removeAll(Arrays.asList(spans));
    }
    /**
     * Remove all specified spans from this Spannable.
     * All span manipulations work with side-effects.
     * @param spans the spans to remove
     * @return true if at least one span was present
     * @see Set#removeAll(Collection)
     */
    public boolean removeSpans(Collection<Span> spans) {
        return this.spans.removeAll(spans);
    }
    /**
     * Remove spans that match a certain criteria. Within the
     * predicate you can instance check for types of Spans
     * and get more information about the span after casting it
     * to the type.
     * <pre>.removeSpansIf(span-&gt;
     *     ((span instanceof ColorSpan) &amp;&amp;
     *     !((ColorSpan)span).getColor().equals(TextColors.YELLOW)) )</pre>
     * All span manipulations work with side-effects.
     * @param removeCondition test that returns whether a span has to be removed
     * @return true if at least one span was removed
     * @see Set#removeIf(Predicate)
     */
    public boolean removeSpansIf(Predicate<Span> removeCondition) {
        return spans.removeIf(removeCondition);
    }

    /**
     * Find all spans formatting the specified index.
     * Span start and Span end are inclusive
     * @param index the position to collect spans for
     * @return collection of Spans at the index
     */
    private Collection<Span> getSpansAt(int index) {
        return spans.stream().filter(s-> s.start() <= index && s.end() >= index).collect(Collectors.toList());
    }

    /**
     * @return the length of this Spannable as plain text
     * @see CharSequence#length()
     */
    @Override
    public int length() {
        return plain.length();
    }

    /**
     * @return the text character at the specified index
     * @see CharSequence#charAt(int)
     */
    @Override
    public char charAt(int index) {
        return plain.charAt(index);
    }

    /**
     * Creates a mutable copy of this Spannable including
     * formats and additional data.
     * @return the subset of a copy of this spannable
     * @see CharSequence#subSequence(int, int)
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        String plainSub = plain.substring(start, end);
        Set<Span> subSpans = new HashSet<>();
        for (Span span : spans) {
            if (span.containedWithin(start, end)) {
                Span copy = span.copy();
                copy.move(-start);
                subSpans.add(copy);
            } else if (span.startsWithin(start, end)) {
                Span copy = span.copy();
                copy.setStart(start);
                copy.move(-start);
                subSpans.add(copy);
            } else if (span.endsWithin(start, end)) {
                Span copy = span.copy();
                copy.setEnd(end);
                copy.move(-start);
                subSpans.add(copy);
            } else if (span.containsRange(start, end)) {
                Span copy = span.copy();
                copy.setStart(start);
                copy.setEnd(end);
                copy.move(-start);
                subSpans.add(copy);
            }
        }
        return new Spannable(plainSub, subSpans);
    }

    /**
     * Creates a Spannable sub-sequence similar to String#substring(int,int).
     * Formats are automatically copied and adjusted for the sbu-sequence.
     * @param beginIndex first index (inclusive)
     * @param endIndex last index (exclusive)
     * @return a new Spannable containing the sub-sequence
     * @see String#substring(int, int)
     */
    public Spannable subspannable(int beginIndex, int endIndex) {
        return (Spannable)subSequence(beginIndex, endIndex);
    }

    /**
     * Creates a Spannable sub-sequence similar to String#substring(int) to the end of this sequence.
     * Formats are automatically copied and adjusted for the sbu-sequence.
     * @param beginIndex first index (inclusive)
     * @return a new Spannable containing the sub-sequence
     * @see String#substring(int, int)
     */
    public Spannable subspannable(int beginIndex) {
        return subspannable(beginIndex, plain.length());
    }

    /**
     * Remove a certain amount of characters from this sequence at the offset.
     * Removing is done with adjusting span boundaries, removing enclosed spans.
     * After removing, the passed sequence is inserted into this Spannable at the
     * same offset.<br>
     * In contrast to JS, this splice command does not support negative arguments
     * @param offset where to operate
     * @param removeCount the number of characters to remove
     * @param insert the text to insert into this Spannable
     * @return a new modified Spannable
     */
    public Spannable splice(int offset, int removeCount, @Nullable CharSequence insert) {
        Spannable cpy = new Spannable(this);
        if (offset < 0) throw new IllegalArgumentException("offset can't be negative");
        if (removeCount < 0) throw new IllegalArgumentException("removeCount can't be negative");
        if (removeCount > 0) {
            int lastExclusive = offset + removeCount;
            int insertCount = insert != null ? insert.length() : 0;
            int growthCount = insertCount-removeCount;
            cpy.plain = plain.substring(0, offset);
            if ( insertCount > 0 )
                cpy.plain += insert;
            if ( lastExclusive < plain.length() )
                cpy.plain += plain.substring(lastExclusive);

            //removed area wraps well around span, removing it in the process
            // since we can't really resize them
            cpy.spans.removeIf(s->s.containedWithin(offset, lastExclusive) && !s.isRange(offset, lastExclusive));
            //resize existing spans
            for (Span s : cpy.spans) {
                //span is wrapped around the removed area (borders inclusive)
                if (s.containsRange(offset, lastExclusive-1)) {
                    s.expand(growthCount);
                }
                //span is right of removed range
                else if (s.start() >= lastExclusive) {
                    s.move(growthCount);
                }
                //span starts within removed range, pokes out right
                else if (s.startsWithin(offset, lastExclusive)) {
                    s.move(growthCount); //move end index without .set(.get())
                    //move start to out of removed range
                    // since the change is from [offset lastExclusive[ to []
                    // the new "end exclusive" is offset+1
                    s.setStart(lastExclusive);
                }
                //span starts left of removed range, ends within
                else if (s.endsWithin(offset, lastExclusive-1)) {
                    s.setEnd(offset-1);
                }
            }
            //insert new spans if insert is spannable
            if (insert instanceof Spannable) {
                Spannable sins = (Spannable) insert;
                for (Span s : sins.spans) {
                    Span copy = s.copy();
                    copy.move(offset);
                    cpy.spans.add(copy);
                }
            }
        } else { //don't remove stuff
            if (insert != null && insert.length() > 0) {
                cpy.plain = cpy.plain.substring(0, offset) + insert;
                if (offset < plain.length())
                    cpy.plain += cpy.plain.substring(offset);
                for (Span s : cpy.spans) {
                    if (s.start() > offset) {
                        s.move(insert.length());
                    }
                    if (s.endsWithin(offset - 1, offset)) {
                        s.expand(insert.length());
                    }
                }
                if (insert instanceof Spannable) {
                    Spannable sins = (Spannable) insert;
                    for (Span s : sins.spans) {
                        Span copy = s.copy();
                        copy.move(offset);
                        cpy.spans.add(copy);
                    }
                }
            }
        }
        cpy.spans.removeIf(span->span.length()<1);
        return cpy;
    }

    /**
     * Checks whether this spannable starts with a particular char sequence.
     * This check ignores formatting and data
     * @param prefix the sequence to check for
     * @return true if this spannable starts with the specified sequence
     * @see String#startsWith(String)
     */
    public boolean startsWith(CharSequence prefix) {
        return plain.startsWith(prefix.toString());
    }
    /**
     * Checks whether this spannable starts with a particular char sequence.
     * This check ignores formatting and data
     * @param prefix the sequence to check for
     * @return true if this spannable starts with the specified sequence
     * @see String#startsWith(String)
     */
    public boolean startsWith(Text prefix) {
        return plain.startsWith(prefix.toPlain());
    }
    /**
     * Checks whether this spannable ends with a particular char sequence.
     * This check ignores formatting and data
     * @param suffix the sequence to check for
     * @return true if this spannable starts with the specified sequence
     * @see String#endsWith(String)
     */
    public boolean endsWith(CharSequence suffix) {
        return plain.endsWith(suffix.toString());
    }
    /**
     * Checks whether this spannable ends with a particular char sequence.
     * This check ignores formatting and data
     * @param suffix the sequence to check for
     * @return true if this spannable starts with the specified sequence
     * @see String#endsWith(String)
     */
    public boolean endsWith(Text suffix) {
        return plain.endsWith(suffix.toPlain());
    }
    /**
     * Checks whether this spannable contains a particular char sequence.
     * This check ignores formatting and data
     * @param sequence the sequence to check for
     * @return true if this spannable contains the specified sequence
     * @see String#contains(CharSequence)
     */
    public boolean contains(CharSequence sequence) {
        return plain.contains(sequence.toString());
    }
    /**
     * Checks whether this spannable contains a particular char sequence.
     * This check ignores formatting and data
     * @param sequence the sequence to check for
     * @return true if this spannable contains the specified sequence
     * @see String#contains(CharSequence)
     */
    public boolean contains(Text sequence) {
        return plain.contains(sequence.toPlain());
    }
    /**
     * Returns an identical Spannable with all characters turned lower case.
     * @return a lower case copy of this spannable
     * @see String#toLowerCase()
     */
    public Spannable toLowerCase() {
        return new Spannable(plain.toLowerCase(), new HashSet<>(spans));
    }
    /**
     * Returns an identical Spannable with all characters turned upper case.
     * @return a upper case copy of this spannable
     * @see String#toLowerCase()
     */
    public Spannable toUpperCase() {
        return new Spannable(plain.toUpperCase(), new HashSet<>(spans));
    }
    /**
     * Creates a subspannable skipping any whitespaces at the beginning and
     * end of this spannable
     * @return a trimmed copy of this spannable
     * @see String#trim()
     */
    public Spannable trim() {
        int i0 = 0;
        while (Character.isWhitespace(plain.charAt(i0)) && i0<plain.length()-1) i0++;
        int i1 = plain.length()-1;
        while (Character.isWhitespace(plain.charAt(i1)) && i1>0) i1--;
        if (i0 == 0 && i1 == plain.length()-1)
            return new Spannable(plain, new HashSet<>(spans));
        if (i1 <= i0)
            return new Spannable("", new HashSet<>());
        return subspannable(i0, i1+1);
    }

    /**
     * Appends the char sequence to this char sequence. This is a
     * short-cut to concat(append, false). Any spans ending with this
     * Spannable will not be expanded to wrap the appended text.
     * @param append the char sequence to append
     * @return the new concated Spannable
     * @see #concat(CharSequence, boolean)
     */
    public Spannable concat(CharSequence append) {
        return concat(append, false);
    }
    /**
     * Appends the char sequence to this char sequence.
     * If extendSpans is true, all spans that ended with the current instance
     * will be expanded to end after the inserted sequence, applying the last
     * format used in this text to the appended sequence, unless overwritten.
     * @param append the char sequence to append
     * @param extendSpans whether to expand the spans
     * @return the new concated Spannable
     * @see #concat(CharSequence, boolean)
     */
    public Spannable concat(CharSequence append, boolean extendSpans) {
        String newPlain = plain + append.toString();
        Set<Span> newSpans = new HashSet<>();
        for (Span s : spans) {
            if (extendSpans && s.end() == length()) {
                Span copy = s.copy();
                copy.expand(append.length());
                newSpans.add(copy);
            } else {
                newSpans.add(s.copy());
            }
        }
        if (append instanceof Spannable) {
            Spannable other = (Spannable)append;
            for (Span s : other.spans) {
                Span copy = s.copy();
                copy.move(length());
                newSpans.add(copy);
            }
        }
        return new Spannable(newPlain, newSpans);
    }
    /**
     * Appends the char sequence to this char sequence. This is a
     * short-cut to concat(append, false). Any spans ending with this
     * Spannable will not be expanded to wrap the appended text.
     * @param append the char sequence to append
     * @return the new concated Spannable
     * @see #concat(CharSequence, boolean)
     */
    public Spannable concat(Text append) {
        return concat(Spannable.from(append), false);
    }
    /**
     * Appends the char sequence to this char sequence.
     * If extendSpans is true, all spans that ended with the current instance
     * will be expanded to end after the inserted sequence, applying the last
     * format used in this text to the appended sequence, unless overwritten.
     * @param append the char sequence to append
     * @param extendSpans whether to expand the spans
     * @return the new concated Spannable
     * @see #concat(CharSequence, boolean)
     */
    public Spannable concat(Text append, boolean extendSpans) {
        return concat(Spannable.from(append), extendSpans);
    }

    /**
     * Replace all occurrences of the target sequence with the
     * replacement sequence. If your replacement is static this method
     * should be preferred.
     * @param target the sequence to replace
     * @param replacement the new sequence to be inserted
     * @return a Spannable with all occurrences replaced
     * @see String#replace(CharSequence, CharSequence)
     */
    public Spannable replace(CharSequence target, CharSequence replacement) {
        List<Integer> occurrences = new LinkedList<>();
        String search = target.toString();
        int pos = plain.indexOf(search);
        while (pos >= 0) {
            occurrences.add(pos);
            pos = plain.indexOf(search, pos+search.length());
        }
        if (occurrences.isEmpty()) return new Spannable(this);
        Spannable copy = new Spannable(this);
        int skip = replacement.length()-search.length(); //how much the length grows for each replacement
        int offset = 0;
        while (!occurrences.isEmpty()) {
            int index = occurrences.remove(0)+offset;
            copy = copy.splice(index, search.length(), replacement);
            offset += skip;
        }
        return copy;
    }
    /**
     * Replace all occurrences of the target sequence with the
     * replacement sequence. If your replacement is static this method
     * should be preferred.
     * @param target the sequence to replace
     * @param replacement the new sequence to be inserted
     * @return a Spannable with all occurrences replaced
     * @see String#replace(CharSequence, CharSequence)
     */
    public Spannable replace(Text target, CharSequence replacement) {
        return replace(target.toPlain(), replacement);
    }
    /**
     * Replace all occurrences of the target sequence with the
     * replacement sequence. If your replacement is static this method
     * should be preferred.
     * @param target the sequence to replace
     * @param replacement the new sequence to be inserted
     * @return a Spannable with all occurrences replaced
     * @see String#replace(CharSequence, CharSequence)
     */
    public Spannable replace(CharSequence target, Text replacement) {
        return replace(target, Spannable.from(replacement));
    }
    /**
     * Replace all occurrences of the target sequence with the
     * replacement sequence. If your replacement is static this method
     * should be preferred.
     * @param target the sequence to replace
     * @param replacement the new sequence to be inserted
     * @return a Spannable with all occurrences replaced
     * @see String#replace(CharSequence, CharSequence)
     */
    public Spannable replace(Text target, Text replacement) {
        return replace(target.toPlain(), Spannable.from(replacement));
    }

    /**
     * Replaced all matches of the regular expression with the replacement.
     * This should work as usual with all RegEx Backrefs. Spans should be
     * expanded when containing Backrefs.
     * @param regex the regex to search for
     * @param replacement the replacement to insert
     * @see #splice(int, int, CharSequence)
     * @see String#replaceAll(String, String)
     */
    public Spannable replaceAll(@RegExp String regex, CharSequence replacement) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(plain);
        _Replaced rep = new _Replaced(0, new Spannable(this));
        while (m.find(rep.post)) {
            rep = _performReplacement(m, replacement);
            m = p.matcher(rep.result.plain);
        }
        return rep.result;
    }
    /**
     * Replaced all matches of the regular expression with the replacement.
     * This should work as usual with all RegEx Backrefs. Spans should be
     * expanded when containing Backrefs.
     * @param regex the regex to search for
     * @param replacement the replacement to insert
     * @see #splice(int, int, CharSequence)
     * @see String#replaceAll(String, String)
     */
    public Spannable replaceAll(@RegExp String regex, Text replacement) {
        return replaceAll(regex, Spannable.from(replacement));
    }

    /**
     * Replaced the first match of the regular expression with the replacement.
     * This should work as usual with all RegEx Backrefs. Spans should be
     * expanded when containing Backrefs.
     * @param regex the regex to search for
     * @param replacement the replacement to insert
     * @see #splice(int, int, CharSequence)
     * @see String#replaceFirst(String, String)
     */
    public Spannable replaceFirst(@RegExp String regex, CharSequence replacement) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(plain);
        if (m.find()) {
            return _performReplacement(m, replacement).result;
        } else {
            return new Spannable(this);
        }
    }
    /**
     * Replaced the first match of the regular expression with the replacement.
     * This should work as usual with all RegEx Backrefs. Spans should be
     * expanded when containing Backrefs.
     * @param regex the regex to search for
     * @param replacement the replacement to insert
     * @see #splice(int, int, CharSequence)
     * @see String#replaceFirst(String, String)
     */
    public Spannable replaceFirst(@RegExp String regex, Text replacement) {
        return replaceFirst(regex, Spannable.from(replacement));
    }

    /**
     * Small class containing the result of _performReplacement.
     * Namely the post index and the new Spannable
     * @see #_performReplacement(Matcher, CharSequence)
     */
    private static class _Replaced{
        /** Convenience constructor for this struct */
        _Replaced(int a, Spannable b) {
            post = a; result = b;
        }
        /**
         * Index of the first character of the result after the replacement
         */
        int post;
        /**
         * The replaced spannable sequence
         */
        Spannable result;
    }
    /**
     * Since we are not necessarily replacing with String we have to parse replacement
     * tokens out self. This function performs said action according to tokens valid in
     * Java.<br>
     * Based on: https://www.regular-expressions.info/refreplacecharacters.html
     * @param matcher a Matcher that already found a match with #find
     * @param replacement the replacement sequence that might include tokens
     * @return a struct containing the return values
     * @see _Replaced
     */
    private _Replaced _performReplacement(Matcher matcher, CharSequence replacement) {
        // \r \n \t -> line breaks and tabs
        // \uFFFF -> one unicode character
        // \? escapes all characters to ?
        // $n inserts the group n
        // $nn only exists if more than 9 groups are present
        // $* is empty string if group number does not participate
        int groupCountDigits = 0;
        int tmp = matcher.groupCount();
        while (tmp > 0) { groupCountDigits++; tmp /= 10; }
        CharSequence resolved = replacement;
        for (int i = 0; i < resolved.length(); i++) {
            if (resolved.charAt(i) == '\\') {
                switch (resolved.charAt(i+1)) { //look ahead
                    case 'n': {
                        resolved = _splicer(resolved, i, 2, "\n");
                        break;
                    }
                    case 'r': {
                        resolved = _splicer(resolved, i, 2, "\r");
                        break;
                    }
                    case 't': {
                        resolved = _splicer(resolved, i, 2, "\t");
                        break;
                    }
                    case 'u': {
                        //consume 4 more letters and parse as char-code
                        int number = Integer.parseInt(resolved.toString().substring(i+1, i+5), 16);
                        resolved = _splicer(resolved, i, 6, Character.toString((char) number));
                    }
                    default: {
                        resolved = _splicer(resolved, i,1,null);
                    }
                }
            } else if (resolved.charAt(i) == '$') {
                if (resolved.charAt(i+1) == '{') { //named group
                    //scan up to }
                    int to = resolved.toString().indexOf('}', i+1);
                    String name = resolved.toString().substring(i+2, to);
                    String groupContent = matcher.group(name);
                    if (groupContent == null) groupContent = "";
                    resolved = _splicer(resolved, i, to+1, groupContent);
                } else {
                    //collect number
                    int j = i + 1;
                    for (; resolved.charAt(j) >= '0' &&
                            resolved.charAt(j) <= '9' &&
                            j - i <= groupCountDigits; j++)
                        ;
                    int group = Integer.parseInt(resolved.toString().substring(i + 1, j));
                    //non participating groups are empty strings, numbers too high shall throw
                    String groupContent = matcher.group(group);
                    if (groupContent == null) groupContent = "";
                    resolved = _splicer(resolved, i, j - i, groupContent);
                }
            }
        }
        //perform actual replacement
        Spannable replaced = splice(matcher.start(), matcher.end()-matcher.start(), resolved);
        //get post index
        int post = matcher.start() + replaced.length();
        return new _Replaced(post, replaced);
    }
    /**
     * Wrapper method for spslice that performs on Spannables as well as CharSequences.
     * The return type is a Spannable for a Spannable target sequence, or a String instance otherwise.
     * @param target the sequence to splice
     * @param at the start position
     * @param amount the amount of characters to remove
     * @param insert the sequence to insert after removal
     * @return the spliced sequence
     * @see #splice(int, int, CharSequence)
     */
    private CharSequence _splicer(CharSequence target, int at, int amount, @Nullable CharSequence insert) {
        if (target instanceof Spannable) {
            return ((Spannable) target).splice(at, amount, insert);
        } else {
            String ts = target.toString();
            if (insert != null) {
                return ts.substring(0, at) +
                        insert.toString() +
                        ts.substring(at + amount);
            } else {
                return ts.substring(0, at) +
                        ts.substring(at + amount);
            }
        }
    }

    /**
     * Splits this Spannable along a regular expression.
     * @param regex the regular expression used to split
     * @return the split up parts
     * @see String#split(String)
     */
    public Spannable[] split(@RegExp String regex) {
        String[] plainSplit = plain.split(regex);
        Spannable[] result = new Spannable[plainSplit.length];
        int offset = 0;
        int index = 0;
        for (String part : plainSplit) {
            result[index] = subspannable(offset, offset+part.length());
            index++;
            offset += part.length();
        }
        return result;
    }

    /**
     * Checks the plain string, ignoring span data
     * @return first index of sequence within the plain representation
     * @see String#indexOf(String)
     */
    public int indexOf(CharSequence sequence) {
        return plain.indexOf(sequence.toString());
    }
    /**
     * Checks the plain string, ignoring span data and Text data
     * @return first index of sequence within the plain representation
     * @see String#indexOf(String)
     */
    public int indexOf(Text sequence) {
        return plain.indexOf(sequence.toPlain());
    }

    /**
     * Checks the plain string, ignoring span data
     * @return last index of sequence within the plain representation
     * @see String#lastIndexOf(String)
     */
    public int lastIndexOf(CharSequence sequence) {
        return plain.indexOf(sequence.toString());
    }
    /**
     * Checks the plain string, ignoring span data and Text data
     * @return last index of sequence within the plain representation
     * @see String#lastIndexOf(String)
     */
    public int lastIndexOf(Text sequence) {
        return plain.indexOf(sequence.toPlain());
    }

    /**
     * Text actually sucks hard and TextColor.NONE / TextStyle.NONE
     * do not END styles / colors but actually literally do not contain
     * styles / colors. For spannable to text conversion it is important
     * for me to revert styles after a span, but since i can not cleanly
     * terminate single styles / colors i have to perform a full RESET
     * and reapply "unstyled" styles that match the overall style of the
     * Text. The default toText() will use RESET style / color.<br>
     * Converts this spannable into a Text representation. The
     * Text object will maintain all formatting and actions spanned
     * over this sequence.
     * @return the Text representation
     */
    public Text toText() {
        return toText(TextColors.RESET, TextStyles.RESET);
    }
    /**
     * Text actually sucks hard and TextColor.NONE / TextStyle.NONE
     * do not END styles / colors but actually literally do not contain
     * styles / colors. For spannable to text conversion it is important
     * for me to revert styles after a span, but since i can not cleanly
     * terminate single styles / colors i have to perform a full RESET
     * and reapply "unstyled" styles that match the overall style of the
     * Text. The default toText() will use RESET style / color.<br>
     * Converts this spannable into a Text representation. The
     * Text object will maintain all formatting and actions spanned
     * over this sequence.
     * @param resetStyled expects a flat Text object with context style / color
     * @return the Text representation
     */
    public Text toText(Text resetStyled) {
        return toText(resetStyled.getColor(), resetStyled.getStyle());
    }
    /**
     * Text actually sucks hard and TextColor.NONE / TextStyle.NONE
     * do not END styles / colors but actually literally do not contain
     * styles / colors. For spannable to text conversion it is important
     * for me to revert styles after a span, but since i can not cleanly
     * terminate single styles / colors i have to perform a full RESET
     * and reapply "unstyled" styles that match the overall style of the
     * Text. The default toText() will use RESET style / color.<br>
     * Converts this spannable into a Text representation. The
     * Text object will maintain all formatting and actions spanned
     * over this sequence.
     * @return the Text representation
     */
    public Text toText(TextColor resetColor, TextStyle resetStyle) {
        //collect points of interest, where formats might change
        SortedSet<Integer> pois = new TreeSet<>(Comparator.naturalOrder());
        for (Span s : spans) {
            if (s.length()<=0) continue;
            pois.add(s.start());
            pois.add(Math.min(s.end(), plain.length()));
        }
        pois.add(plain.length()); //finish up the rest

        //create stacks
        LinkedList<ColorSpan> colors = new LinkedList<>();
        LinkedList<StyleSpan> style = new LinkedList<>();
        LinkedList<ClickActionSpan> actClick = new LinkedList<>();
        LinkedList<ShiftClickActionSpan> actShiftClick = new LinkedList<>();
        LinkedList<HoverActionSpan> actHover = new LinkedList<>();
        Text.Builder resultBuilder = Text.builder();
        resultBuilder.style( resetStyle );
        resultBuilder.color( resetColor );

        //traverse points of interes
        int previous=0;
        for (int i : pois) {

            //create text segment up to position i (exclusive)
            if (i != 0) {
                Text.Builder builder = Text.builder(plain.substring(previous, i));
                if (!colors.isEmpty()) {
                    colors.getLast().apply(builder);
                } else {
                    resetColor.applyTo(builder);
                }
                if (!style.isEmpty()) {
                    //all active styles need to be applied
                    builder.style(TextStyles.of(style.stream()
                            .map(StyleSpan::getStyle)
                            .distinct()
                            .toArray(TextStyle[]::new)));
                } else {
                    resetStyle.applyTo(builder);
                }
                if (!actClick.isEmpty()) actClick.getLast().apply(builder);
                if (!actShiftClick.isEmpty()) actShiftClick.getLast().apply(builder);
                if (!actHover.isEmpty()) actHover.getLast().apply(builder);
                resultBuilder.append(builder.build());
            }
//            //styles and colors are messy:
//            // remove all styles, re-add default styles to prevent bleeding
//            // the next segment will add those again if needed
//            resultBuilder.append(Text.of(TextStyles.RESET, resetColor, resetStyle));

            previous = i;
            //update span stack for next segment
            Collection<Span> spans = getSpansAt(i);
            for (Span span : spans) {
                if (span.length()<=0) continue;

                //pop spans that are closed after i
                if (span.end() == i) { //span IS closed at this point
                    if (span instanceof ColorSpan) {
                        colors.remove(span);
                    } else if (span instanceof StyleSpan) {
                        style.remove(span);
                    } else if (span instanceof ClickActionSpan) {
                        actClick.remove(span);
                    } else if (span instanceof ShiftClickActionSpan) {
                        actShiftClick.remove(span);
                    } else if (span instanceof HoverActionSpan) {
                        actHover.remove(span);
                    }
                }
                //add spans that start with i
                if (span.start() == i) {
                    if (span instanceof ColorSpan && !colors.contains(span)) {
                        colors.add((ColorSpan) span);
                    } else if (span instanceof StyleSpan && !style.contains(span)) {
                        style.add((StyleSpan) span);
                    } else if (span instanceof ClickActionSpan && !actClick.contains(span)) {
                        actClick.add((ClickActionSpan) span);
                    } else if (span instanceof ShiftClickActionSpan && !actShiftClick.contains(span)) {
                        actShiftClick.add((ShiftClickActionSpan) span);
                    } else if (span instanceof HoverActionSpan && !actHover.contains(span)) {
                        actHover.add((HoverActionSpan) span);
                    }
                }
            }
        }

        return resultBuilder.build();
    }

    /**
     * @return the plain text without formatting or additional data
     */
    public String toString() {
        return plain;
    }
    /**
     * @return the plain text without formatting or additional data
     */
    public CharSequence toPlain() {
        return plain;
    }

    /**
     * Serializes this Spannable with the default format code '&amp;'.
     * The result should be visually identical to serializing #toText()
     * @see org.spongepowered.api.text.serializer.TextSerializers#FORMATTING_CODE
     * @see #toSerialized(char)
     */
    public String toSerialized() {
        return toSerialized('&');
    }
    /**
     * Serializes this Spannable with the given format code.
     * The result should be visually identical to serializing #toText()<br>
     * Fun Fact:<br>
     * Minecraft has a internal formatting character \u00a7, that can mess with Text.
     * @see org.spongepowered.api.text.serializer.TextSerializers#FORMATTING_CODE
     * @see #toSerialized(char)
     */
    public String toSerialized(char escapeCharacter) {
        //collect points of interest, where formats might change
        SortedSet<Integer> pois = new TreeSet<>(Comparator.naturalOrder());
        for (Span s : spans) {
            pois.add(s.start());
            pois.add(Math.min(s.end(), plain.length()));
        }
        pois.add(plain.length()); //finish up the rest

        //create stacks
        LinkedList<ColorSpan> colors = new LinkedList<>();
        LinkedList<StyleSpan> style = new LinkedList<>();
        StringBuilder resultBuilder = new StringBuilder();

        //traverse points of interes
        int previous=0;
        for (int i : pois) {

            //create text segment up to position i (exclusive)
            if (i != 0) {
                resultBuilder.append(plain, previous, i);
            }
            previous = i;

            Set<ColorSpan> openingColorSpans = new HashSet<>();
            Set<ColorSpan> closingColorSpans = new HashSet<>();
            Set<StyleSpan> openingStyleSpans = new HashSet<>();
            Set<StyleSpan> closingStyleSpans = new HashSet<>();
            for (Span span : spans) {
                //pop spans that are closed after i
                if (span.end() == i) { //span IS closed at this point
                    if (span instanceof ColorSpan) {
                        colors.remove(span);
                        closingColorSpans.add((ColorSpan) span);
                    } else if (span instanceof StyleSpan) {
                        style.remove(span);
                        closingStyleSpans.add((StyleSpan) span);
                    }
                }
                //add spans that start with i
                if (span.start() == i) {
                    if (span instanceof ColorSpan) {
                        colors.add((ColorSpan) span);
                        openingColorSpans.add((ColorSpan) span);
                    } else if (span instanceof StyleSpan) {
                        style.add((StyleSpan) span);
                        openingStyleSpans.add((StyleSpan) span);
                    }
                }
            }
            if (!openingColorSpans.isEmpty() || !closingStyleSpans.isEmpty() || !closingColorSpans.isEmpty()) {
                //(re-)apply color (resets all styles), then reapply styles
                if (colors.isEmpty()) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append('r');
                } else {
                    resultBuilder.append(escapeCharacter);
                    Character c = formats.getOrDefault(colors.getLast().getColor(), 'r');
                    resultBuilder.append(c);
                }
                //check active styles
                boolean magic = false, bold = false, strike = false, underline = false, italic = false;
                for (StyleSpan s : style) {
                    if (s.getStyle().isObfuscated().orElse(false)) magic = true;
                    if (s.getStyle().isBold().orElse(false)) bold = true;
                    if (s.getStyle().hasStrikethrough().orElse(false)) strike = true;
                    if (s.getStyle().hasUnderline().orElse(false)) underline = true;
                    if (s.getStyle().isItalic().orElse(false)) italic = true;
                }
                //apply styles one by one
                if (magic) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.OBFUSCATED));
                }
                if (bold) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.BOLD));
                }
                if (strike) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.STRIKETHROUGH));
                }
                if (underline) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.UNDERLINE));
                }
                if (italic) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.ITALIC));
                }
            } else if (!openingStyleSpans.isEmpty()) {
                //check opened styles
                boolean magic = false, bold = false, strike = false, underline = false, italic = false;
                for (StyleSpan s : openingStyleSpans) {
                    if (s.getStyle().isObfuscated().orElse(false)) magic = true;
                    if (s.getStyle().isBold().orElse(false)) bold = true;
                    if (s.getStyle().hasStrikethrough().orElse(false)) strike = true;
                    if (s.getStyle().hasUnderline().orElse(false)) underline = true;
                    if (s.getStyle().isItalic().orElse(false)) italic = true;
                }
                //apply styles one by one
                if (magic) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.OBFUSCATED));
                }
                if (bold) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.BOLD));
                }
                if (strike) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.STRIKETHROUGH));
                }
                if (underline) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.UNDERLINE));
                }
                if (italic) {
                    resultBuilder.append(escapeCharacter);
                    resultBuilder.append(formats.get(TextStyles.ITALIC));
                }
            }
        }

        return resultBuilder.toString();
    }

    /**
     * Wraps a char sequence like a string in a new Spannable.
     * If the char sequence uses internal formatting char those will automatically be wrapped
     * @param sequence the character sequence to wrap
     * @return the sequence as Spannable
     */
    public static Spannable from(CharSequence sequence) {
        if (sequence instanceof Spannable) {
            return new Spannable((Spannable) sequence);
        } else {
            return parseSerialized(sequence.toString(), '\u00a7');
            //return new Spannable(sequence.toString(), new HashSet<>());
        }
    }
    /**
     * Parses a text and it's children into spans.
     * All text-actions are preserved by conversion.
     * @param text the text object to convert
     * @return new Spannable representation of the text
     */
    public static Spannable from(Text text) {
//        Sponge.getServer().getConsole().sendMessage(Text.of("Parsing Spannable from: ", text));
        int offset = 0;
        StringBuilder plain = new StringBuilder();
        Set<Span> spans = new HashSet<>();
        for (Text element : text.withChildren()) {
//            Text.Builder part = Text.builder();
//            part.append(Text.of("  Part ", element.toPlainSingle(), " (c ", element.getChildren().size(), ")"));
            plain.append(element.toPlainSingle());
            int length = element.toPlainSingle().length();
            if (formats.containsKey(element.getColor())) {
                spans.add(new ColorSpan(offset, offset+length, element.getColor()));
//                part.append(Text.of(" color "+element.getColor().toString()));
            }
            if (formats.containsKey(element.getStyle())) {
                spans.add(new StyleSpan(offset, offset+length, element.getStyle()));
//                part.append(Text.of(" style "+element.getStyle().toString()));
            }
            if (element.getClickAction().isPresent()) {
                spans.add(new ClickActionSpan(offset, offset+length, element.getClickAction().get()));
//                part.append(Text.of(" click action"));
            }
            if (element.getHoverAction().isPresent()) {
                spans.add(new HoverActionSpan(offset, offset+length, element.getHoverAction().get()));
//                part.append(Text.of(" hover action"));
            }
            if (element.getShiftClickAction().isPresent()) {
                spans.add(new ShiftClickActionSpan(offset, offset+length, element.getShiftClickAction().get()));
//                part.append(Text.of(" shift-click action"));
            }
            offset += length;
//            Sponge.getServer().getConsole().sendMessage(part.build());
        }
        return new Spannable(plain.toString(), spans);
    }
    /**
     * Parse a String as serialized string with the ampersand (&amp;) as formatting char.
     * This should have the same effect as using {@link TextSerializers#FORMATTING_CODE}.<br>
     * Note that the Minecraft internal formatting character § (section sign) is always parsed
     * as well.
     * @param serialized the serial representation of a formatted text
     * @return the Spannable version of the parsed text
     * @see TextSerializers#FORMATTING_CODE
     */
    public static Spannable parseSerialized(String serialized) {
        return from(TextSerializers.FORMATTING_CODE.deserialize(serialized.replace('\u00A7', '&')));
    }

    /**
     * Parse a String as serialized string with alternative formatting char.
     * This should have the same effect as using {@link TextSerializers#FORMATTING_CODE}.<br>
     * Note that the Minecraft internal formatting character § (section sign) is always parsed
     * as well.
     * @param serialized the serial representation of a formatted text
     * @return the Spannable version of the parsed text
     * @see TextSerializers#formattingCode(char)
     */
    public static Spannable parseSerialized(String serialized, char escapeCharacter) {
        String preplaced = escapeCharacter == '\u00a7' ? serialized : serialized.replace('\u00A7', escapeCharacter);
        return from(TextSerializers.formattingCode(escapeCharacter).deserialize(preplaced));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Spannable spannable = (Spannable) o;
        return plain.equals(spannable.plain) &&
                spans.equals(spannable.spans);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plain, spans);
    }
}
