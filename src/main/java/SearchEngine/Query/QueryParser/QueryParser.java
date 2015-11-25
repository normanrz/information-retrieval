package SearchEngine.Query.QueryParser;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;

/**
 * Created by norman on 25.11.15.
 */
@BuildParseTree
public class QueryParser extends BaseParser<Object> {

    Rule MultipleRules(Rule rule, Rule separator) {
        return Sequence(rule, ZeroOrMore(Sequence(separator, rule)));
    }

    Rule Word() {
        return OneOrMore(CharRange('a', 'z'));
    }

    Rule Number() {
        return OneOrMore(CharRange('0', '9'));
    }

    Rule Token() {
        return Sequence(Word(), Optional(Ch('*')));
    }

    Rule Prf() {
        return Sequence(Ch('#'), Number());
    }

    Rule Phrase() {
        return Sequence(
                Ch('"'),
                OptionalWhitespace(),
                Token(),
                ZeroOrMore(Sequence(Whitespace(), Token())),
                OptionalWhitespace(),
                Ch('"'));
    }

    Rule TokenOrPhrase() {
        return FirstOf(Token(), Phrase());
    }

    Rule NotToken() {
        return Sequence(String("NOT"), Whitespace(), Token());
    }

    Rule BooleanAndSentence() {
        return FirstOf(
                Sequence(
                        BooleanPrimarySentence(),
                        Whitespace(),
                        String("AND"),
                        Whitespace(),
                        BooleanAndSentence()
                ),
                Sequence(BooleanPrimarySentence(), Whitespace(), BooleanAndSentence()),
                BooleanPrimarySentence()
        );
    }

    Rule BooleanOrSentence() {
        return FirstOf(
                Sequence(
                        BooleanAndSentence(),
                        Whitespace(),
                        String("OR"),
                        Whitespace(),
                        BooleanOrSentence()),
                BooleanAndSentence());
    }

    Rule BooleanPrimarySentence() {
        return FirstOf(
                Sequence(
                        Ch('('),
                        OptionalWhitespace(),
                        BooleanSentence(),
                        OptionalWhitespace(),
                        Ch(')')),
                TokenOrPhrase());
    }

    Rule BooleanSentence() {
        return BooleanOrSentence();
    }

    Rule Whitespace() {
        return OneOrMore(AnyOf(" \t\f"));
    }

    Rule OptionalWhitespace() {
        return ZeroOrMore(AnyOf(" \t\f"));
    }

    Rule Subquery() {
        return FirstOf(
                BooleanSentence(),
                MultipleRules(TokenOrPhrase(), Whitespace())
        );
    }

    Rule Query() {
        return FirstOf(
                Sequence(Subquery(), Whitespace(), Prf()),
                Sequence(Subquery(), Whitespace(), NotToken()),
                Subquery());
    }

    public Rule FullQuery() {
        return Sequence(OptionalWhitespace(), Query(), OptionalWhitespace(), EOI);
    }

}
