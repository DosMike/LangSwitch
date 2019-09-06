package de.dosmike.sponge.spannable;

import de.dosmike.sponge.langswitch.LangSwitch;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/** since i can't really set up junit tests */
public class Test {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestCase {}

    public static void test() {
        Set<Method> tests = new HashSet<>();
        int successful = 0;
        for (Method m : Test.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(TestCase.class)) {
                m.setAccessible(true);
                tests.add(m);
            }
        }

        if (tests.isEmpty()) {
            LangSwitch.w("No test cases!");
            return;
        }
        int i=1;
        Test instance = new Test(); //in case method is not static
        for (Method test : tests) {
            LangSwitch.l("Performing test %s (%d/%d)...",test.getName(),i++,tests.size());
            if (performTest(test, instance)) successful++;
        }
        double failing = (1- (double)successful/(double)tests.size())*100;
        LangSwitch.l("Done! %.2f%% failed, %d/%d successful...",failing,successful,tests.size());
    }
    /**
     *
     */
    private static boolean performTest(Method test, Test failsafeInstance) {
        try {
            if ((test.getModifiers() & Modifier.STATIC)>0) {
                test.invoke(null);
            } else {
                test.invoke(failsafeInstance);// forgot static modifier
            }
            return true;
        } catch (Throwable error) {
            new RuntimeException("Test "+test.getName()+" failed", error).printStackTrace();
            return false;
        }
    }

    @TestCase
    private static void testTextFormatUnequals() {
        Text formatted = Text.of("Hello ",TextColors.YELLOW,"World",TextColors.NONE,"!");
        Text unformatted = Text.of("Hello World!");

        assert !formatted.equals(unformatted) : "Formats do not impact Text.equals!";
    }

    @TestCase
    private static void testConvertToText() {
        Spannable span = Spannable.from("This test has some parts colored");
        span.addSpans(new ColorSpan(10,18,TextColors.YELLOW));

        Text result = span.toText();
        Text expected = Text.of("This test ", TextColors.YELLOW, "has some", TextColors.NONE, " parts colored");

        assert result.equals(expected) : "Spannable formats differ from Text formats";
    }

    @TestCase
    private static void testSerialization() {
        Spannable span = Spannable.from("This test has some parts colored");
        span.addSpans(new ColorSpan(10,18,TextColors.YELLOW));

        String result = span.toSerialized();
        String expected =
                TextSerializers.FORMATTING_CODE.serialize(
                Text.of("This test ", TextColors.YELLOW, "has some", TextColors.NONE, " parts colored")
                );

        assert result.equals(expected) : "Serialization for same text differs";
    }

    @TestCase
    private static void testDeserialization() {
        String serial = "This test &ehas some&r parts colored";
        Spannable span = Spannable.from(serial);
        Text text = TextSerializers.FORMATTING_CODE.deserialize(serial);

        assert span.toText().equals(text) : "Deserialization differs between Text and Spannable";
    }

    @TestCase
    private static void testReplaceText() {
        Text original = Text.of("Hello ", TextColors.GREEN, "Friend", TextColors.RESET, "!");
        Spannable span = Spannable.from(original);
        span = span.replace("Friend", Text.of("World"));

        Text expected = Text.of("Hello &aWorld&r!");

        assert span.toText().equals(expected) : "Text replacement failed";
    }


    @TestCase
    private static void testReplaceTextRegex() {
        Text original = Text.of("Cats and Dogs and Foxes");
        Spannable span = Spannable.from(original);
        span = span.replaceAll("(?:[A-Za-z]+s)", Text.of("Animals"));

        Text expected = Text.of("Animals and Animals and Animals", TextColors.NONE);

        assert span.toText().equals(expected) : "Text regex color insert failed";
    }

    @TestCase
    private static void testReplaceTextColorRegex() {
        Text original = Text.of("Cats and Dogs and Foxes");
        Spannable span = Spannable.from(original);
        span = span.replaceAll("(?:[A-Za-z]+s)", Text.of(TextColors.YELLOW, "Animals"));

        Text expected = Text.of(TextColors.YELLOW, "Animals", TextColors.NONE, " and ", TextColors.YELLOW, "Animals", TextColors.NONE, " and ", TextColors.YELLOW, "Animals", TextColors.NONE);

        assert span.toText().equals(expected) : "Text regex color insert failed";
    }

    @TestCase
    private static void testReplaceRegexCaptureGroupNumbered() {
        Text original = Text.of("https://ore.spongepowered.org/dosmike/langswitch");
        Spannable span = Spannable.from(original);
        span = span.replaceFirst("^(.*)/(\\w+)/(\\w+)$", Text.of("$1/$2/otherproject"));

        Text expected = Text.of("https://ore.spongepowered.org/dosmike/otherproject");

        assert span.toText().equals(expected) : "Numbered backref incorrect";
    }

    @TestCase
    private static void testStripSpans() {
        Text original = Text.of(TextColors.YELLOW, "Hello World!");
        Spannable span = Spannable.from(original);
        span.stripSpans(9,11);

        Text expected = Text.of(TextColors.YELLOW, "Hello ", TextColors.NONE, "World", TextColors.YELLOW, "!");

        assert span.toText().equals(expected) : "Breaking up spans does not work";
    }

    @TestCase
    private static void testStripSpans2() {
        Text original = Text.of("Hello ",TextColors.YELLOW,"World",TextColors.NONE,"!");
        Spannable span = Spannable.from(original);
        span.stripSpans(9,11);

        Text expected = Text.of("Hello World!");

        assert span.toText().equals(expected) : "Removing contained spans does not work";
    }
}
