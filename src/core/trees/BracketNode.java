package core.trees;

import core.exceptions.InvalidInsertionException;

import static core.symbols.Symbol.LBRACKET;
import static core.symbols.Symbol.RBRACKET;

public class BracketNode extends Node {

    Node head;
    private boolean closed;

    public BracketNode() {
        super();
        head = null;
        closed = false;
        this.value = null;
    }

    @Override
    public Node insert(LitNode node) {
        if (isClosed()) {
            throw new InvalidInsertionException("Inserting Literal immediately after Right bracket");
        } else {
            if (head == null) head = node;
            else head = head.insert(node);
        }
        return this;
    }

    @Override
    public Node insert(BracketNode node) {
        if (isClosed()) {
            throw new InvalidInsertionException("Inserting Left bracket immediately after Right bracket");
        } else {
            if (head == null) head = node;
            else head = head.insert(node);
        }
        return this;
    }

    @Override
    public Node insert(ConnNode node) {
        if (isClosed()) {
            node.left = this;
            return node;
        } else {
            if (head == null) head = node;
            else head = head.insert(node);
            return this;
        }
    }

    @Override
    public Node insert(NegNode node) {
        if (isClosed()) {
            throw new InvalidInsertionException("Inserting Negation immediately after Right bracket");
        } else {
            if (head == null) head = node;
            else head = head.insert(node);
            return this;
        }
    }

    public void close() {
        if (isClosed()) throw new IllegalStateException("This bracket node is already closed");
        else closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public StringBuilder toTreeStringBuilder(int depth) {
        StringBuilder head_sb = head == null ? new StringBuilder() : head.toTreeStringBuilder(depth);
        String spaces = getSpaces(depth);
        return new StringBuilder(spaces)
                .append(LBRACKET).append(System.lineSeparator())
                .append(head_sb)
                .append(spaces)
                .append(RBRACKET).append(System.lineSeparator());
    }

    @Override
    public StringBuilder toBracketStringBuilder() {
        StringBuilder head_sb = head == null ? new StringBuilder() : head.toBracketStringBuilder();
        return new StringBuilder()
                .append(LBRACKET)
                .append(head_sb)
                .append(RBRACKET);
    }

    private void ensureComplete() {
        if (head == null) throw new IllegalStateException("Empty bracket: " + toString());
        else if (!isClosed()) throw new IllegalStateException("Unclosed bracket node: " + toString());
    }

    @Override
    protected void eliminateArrows() {
        ensureComplete();
        head.eliminateArrows();
    }

    @Override
    protected Node invertNegation() {
        ensureComplete();
        head = head.invertNegation();
        return this;
    }

    @Override
    protected Node copy() {
        ensureComplete();
        BracketNode new_node = new BracketNode();
        new_node.head = head.copy();
        new_node.close();
        return new_node;
    }

    @Override
    protected StringBuilder toStringBuilder() {
        StringBuilder head_sb = head == null ? new StringBuilder() : head.toStringBuilder();
        return new StringBuilder()
                .append(LBRACKET)
                .append(head_sb)
                .append(RBRACKET);
    }

    @Override
    public boolean isTrue() {
        ensureComplete();
        return head.isTrue();
    }
}