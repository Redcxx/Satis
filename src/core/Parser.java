package core;

import core.exceptions.InvalidFormulaException;
import core.symbols.Connective;
import core.symbols.Literal;
import core.symbols.Negation;
import core.trees.BoxNode;
import core.trees.ConnNode;
import core.trees.LitNode;
import core.trees.NegNode;
import org.jetbrains.annotations.Contract;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static core.symbols.Symbol.*;


/**
 * This is a Propositional logic formula checker
 *
 * Usage:
 * <code>new Parser().evaluate([formula])</code>
 * On fail it will throw a Runtime InvalidFormulaException
 * On success nothing happens and you can retrieve its parseTree<Node> through getTree method
 *
 * Time Complexity
 * The checking algorithm runs in one pass O(n)
 * The insertion algorithm runs in O(n log n)
 * Overall this runs in O(n + n log n) = O(n log n)
 *
 * It parse by determining whether previous token can be follow by current token
 *
 * // TODO add checks to insertion?
 */
public class Parser {

    private int index;  // current index
    private Stack<BoxNode> box_nodes_stack;  // for checking valid brackets
    private BoxNode curr_node;
    private boolean incomplete_clause;  // a flag to check if parsing stopped half way of a complete clause
    private String unprocessed_str;  // user input string
    private Token prev_token;  // for checking if previous token followed by current token
    private char[] chars;  // char array form of current input string
    private Stack<Character> brackets_stack;

    private List<Literal> literals;
    private Set<Literal> literalPool;

    public Parser() {

    } // empty

    public static void main(String[] args) {
        Parser p = new Parser();
        Instant start = Instant.now();
        p.evaluate("(Chicken /\\ (~Tiger -> cat) <-> Snake \\/ Dog -> ~Cat /\\ (Snake <-> Duck)) \\/ ~Monkey");
        Instant end = Instant.now();
        System.out.println((String.format("Runtime: %sms", Duration.between(start, end).toMillis())));
        System.out.println(p);
        System.out.println(p.getTree().toString(0));
    }

    private void reset(String newStr) {
        index = 0;
        incomplete_clause = true;
        curr_node = new BoxNode();
        box_nodes_stack = new Stack<>();
        unprocessed_str = newStr;
        brackets_stack = new Stack<>();
        prev_token = Token.START;
        chars = unprocessed_str == null ? null : unprocessed_str.toCharArray();
        literals = new ArrayList<>();
        literalPool = new HashSet<>();
    }

    public BoxNode getTree() {
        ensureEvaluated();
        return curr_node;
    }

    void ensureEvaluated() {
        if (curr_node == null) {
            throw new IllegalStateException("There is no tree before evaluate is called");
        }
    }

    public List<Literal> getLiterals() {
        return literals;
    }

    public Parser evaluate(String s) throws InvalidFormulaException {
        if (s == null) {
            throw new InvalidFormulaException("Propositional Logic formula can't be null");
        }
        reset(s);
        while (index < chars.length) {
            char curr_char = chars[index];
            if (curr_char == ' ') {
                index++;
                continue;
            }
            switch (curr_char) {
                case '~':
                    handleNegation();
                    break;
                case '(':
                    handleLeftBracket();
                    break;
                case ')':
                    handleRightBracket();
                    break;
                case '/':
                    handleForwardSlash();
                    break;
                case '\\':
                    handleBackwardSlash();
                    break;
                case '-':
                    handleDash();
                    break;
                case '<':
                    handleLessThan();
                    break;
                default:
                    if (Character.isLetter(curr_char)) {
                        handleCharacter();
                    } else {
                        handle_error(String.format("Invalid character: \"%s\"", curr_char));
                    }
                    break;
            }  // switch
        } // while

        if (prev_token == Token.START) {
            handle_error("Propositional logic formula can't be empty");
        }
        if (incomplete_clause) {
            handle_error("Incomplete clause");
        }
        if (!brackets_stack.empty()) {
            handle_error("Unclosed opening bracket");
        }

        return this;
    }

    public String toString() {
        return unprocessed_str;
    }

    @Contract(" _ -> fail")
    private void handle_error(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg).append(System.lineSeparator()).append(unprocessed_str).append(System.lineSeparator());
        while (--index > 0) sb.append(' ');
        sb.append(String.format("^^%n"));
        throw new InvalidFormulaException(sb.toString());
    }

    private void insertLiteralToken(String literal_str) {
        Literal literal = Literal.newInstance(literal_str);
        curr_node.insert(new LitNode(literal));
        if (!literalPool.contains(literal)) {
            literals.add(literal);
            literalPool.add(literal);
        }
    }

    private void insertConnectiveToken(String con) {
        curr_node.insert(new ConnNode(Connective.newInstance(con)));
    }

    private void insertNegationToken() {
        curr_node.insert(new NegNode(Negation.newInstance(String.valueOf(NEG))));
    }

    private void insertLeftBracketToken() {
        box_nodes_stack.push(curr_node);
        curr_node = new BoxNode();
    }

    private void insertRightBracketToken() {
        if (!box_nodes_stack.isEmpty()) {
            BoxNode old = curr_node;
            curr_node = box_nodes_stack.pop();
            old.close();
            curr_node.insert(old);
        } else {
            curr_node.close();
        }
    }

    private void handleNegation() {
        if (prev_token == Token.LITERAL || prev_token == Token.RBRACKET) {
            handle_error("Negation not allowed here");
        }
        prev_token = Token.NEG;
        incomplete_clause = true;
        insertNegationToken();
        index++;
    }

    private void handleLeftBracket() {
        if (prev_token == Token.LITERAL || prev_token == Token.RBRACKET) {
            handle_error("Left bracket not allowed here");
        }
        prev_token = Token.LBRACKET;
        brackets_stack.push(LBRACKET);
        incomplete_clause = true;
        insertLeftBracketToken();
        index++;
    }

    private void handleRightBracket() {
        if (prev_token != Token.LITERAL && prev_token != Token.RBRACKET) {
            handle_error("Right bracket not allowed here");
        }
        if (brackets_stack.empty() || brackets_stack.pop() != LBRACKET) {
            handle_error("Unopened closing bracket");
        }
        prev_token = Token.RBRACKET;
        incomplete_clause = false;
        insertRightBracketToken();
        index++;
    }

    private void checkConnective(Token prev) {
        if (prev != Token.LITERAL && prev != Token.RBRACKET) {
            handle_error("Connective not allowed here");
        }
    }

    private void handleForwardSlash() {
        checkConnective(prev_token);
        if (index + 1 >= chars.length || chars[index + 1] != BACKWARD_SLASH) {  // must be AND /\
            handle_error(String.format("Invalid character: \"%s\", do you mean %s?", FORWARD_SLASH, AND));
        } else {
            prev_token = Token.CONNECTIVE;
            incomplete_clause = true;
            insertConnectiveToken(AND);
            index += 2;
        }
    }

    private void handleBackwardSlash() {
        checkConnective(prev_token);
        if (index + 1 >= chars.length || chars[index + 1] != '/') {  // must be OR \/
            handle_error(String.format("Invalid character: \"%s\", do you mean \"%s\" or \"%s\"?",
                    BACKWARD_SLASH, OR, AND));
        } else {
            prev_token = Token.CONNECTIVE;
            incomplete_clause = true;
            insertConnectiveToken(OR);
            index += 2;
        }
    }

    private void handleDash() {
        // if dash followed by <, then it is already checked that we can have connective
        if (index - 1 >= 0 && chars[index - 1] != LESS_THAN) {
            checkConnective(prev_token);
        }
        if (index + 1 >= chars.length || chars[index + 1] != GREATER_THAN) {  // must be ->
            handle_error(String.format("Invalid character: \"%s\", do you mean %s?", DASH, IMPLIES));
        }
        prev_token = Token.CONNECTIVE;
        incomplete_clause = true;
        insertConnectiveToken(IMPLIES);
        index += 2;
    }

    private void handleLessThan() {
        checkConnective(prev_token);
        // must be <->
        if (index + 2 >= chars.length || (chars[index + 1] != DASH || chars[index + 2] != GREATER_THAN)) {
            handle_error(String.format("Invalid character: \"%s\", do you mean %s?", LESS_THAN, IFF));
        } else {
            prev_token = Token.CONNECTIVE;
            incomplete_clause = true;
            insertConnectiveToken(IFF);
            index+=3;
        }
    }

    private void handleCharacter() {
        if (prev_token == Token.RBRACKET || prev_token == Token.LITERAL) {
            handle_error("Literal not allowed here");
        }
        StringBuilder sb = new StringBuilder();
        while (index < chars.length && Character.isLetter(chars[index])) {
            sb.append(chars[index++]);
        }
        incomplete_clause = false;
        insertLiteralToken(sb.toString());
        prev_token = Token.LITERAL;
    }

    private enum Token {
        NEG, RBRACKET, LBRACKET, LITERAL, CONNECTIVE, START  // null does not match anything
    }

}
