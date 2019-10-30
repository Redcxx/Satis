package core.trees;

import core.exceptions.InvalidInsertionException;
import core.symbols.Literal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static core.common.Utilities.printMap;

public class BoxNode extends BinaryNode {

    private Node head;
    private boolean closed = false;

    public BoxNode() {
        super();
        head = null;
        closed = false;
        this.node_value = null;
    }

    @Override
    public Node insert(LitNode node) {
        if (closed) {
            throw new InvalidInsertionException("Inserting Literal immediately after Right bracket");
        } else {
            if (head == null) head = node;
            else head = head.insert(node);
        }
        return this;
    }

    @Override
    public Node insert(BoxNode node) {
        if (closed) {
            throw new InvalidInsertionException("Inserting Left bracket immediately after Right bracket");
        } else {
            if (head == null) head = node;
            else head = head.insert(node);
        }
        return this;
    }

    @Override
    public Node insert(ConnNode node) {
        if (closed) {
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
        if (closed) {
            throw new InvalidInsertionException("Inserting Negation immediately after Right bracket");
        } else {
            if (head == null) head = node;
            else head = head.insert(node);
            return this;
        }
    }

    public void close() {
        if (closed) throw new IllegalStateException("This Box node is already closed");
        else closed = true;
    }

    @Override
    public String toString(int depth) {
        return head.toString(depth + 1);
    }

    @Override
    public boolean isSatisfiable(Map<Literal, Boolean> interpretation, boolean truth_value) {
        if (head == null) {
            throw new IllegalStateException("Checking satisfiability of empty box node");
        }
        return head.isSatisfiable(interpretation, truth_value);
    }
}
