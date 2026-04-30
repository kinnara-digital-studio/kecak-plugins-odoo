package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OdooFilterConverter {
    /**
     * Converts an array of SearchFilter objects into an Odoo
     * prefix domain Object[] array. Honors the mathematical precedence
     * of AND relative to OR operations.
     */

    public static Object[] convert(SearchFilter[] filters) {
        if (filters == null || filters.length == 0) {
            return new Object[0];
        }
        List<String> OperatorList = new ArrayList<>();
        List<SearchFilter> FilterList = new ArrayList<>();

        for (int i = 0; i < filters.length; i++) {
            if (i < filters.length - 1) {
                String join = filters[i].getJoin();
                if (join == null || join.trim().isEmpty()) {
                    join = SearchFilter.AND;
                }
                if (join.equals("OR")) {
                    String or = "|";
                    OperatorList.add(or);
                } else {
                    String and = "&";
                    OperatorList.add(and);
                }
            }
            FilterList.add(filters[i]);
        }

        Collections.reverse(OperatorList);
        List<Object> output = new ArrayList<>(OperatorList);
        FilterList.stream()
                .map(f -> new Object[] { f.getField(), f.getOperator(), f.getValue() })
                .forEach(output::add);
        return output.toArray();
    }
    // public static Object[] convert(SearchFilter[] filters) {
    // if (filters == null || filters.length == 0) {
    // return new Object[0];
    // }
    //
    // List<Object> tokens = new ArrayList<>();
    // for (int i = 0; i < filters.length; i++) {
    // if (i > 0) {
    // // Determine operator for joining with previous token. Defaults to AND if
    // // null/blank.
    // String join = filters[i - 1].getJoin();
    // if (join == null || join.trim().isEmpty()) {
    // join = SearchFilter.AND;
    // }
    // tokens.add(join.toUpperCase());
    // }
    // tokens.add(filters[i]);
    // }
    //
    // Node root = parse(tokens);
    // List<Object> prefix = new ArrayList<>();
    // toPrefix(root, prefix);
    // return prefix.toArray();
    // }
    //
    // private static Node parse(List<Object> tokens) {
    // ParserState state = new ParserState(tokens);
    // return parseExpr(state);
    // }
    //
    // private static Node parseExpr(ParserState state) {
    // Node node = parseTerm(state);
    // while (state.hasNext() && "OR".equals(state.peek())) {
    // state.next(); // consume OR
    // Node right = parseTerm(state);
    // node = new OperatorNode("OR", node, right);
    // }
    // return node;
    // }
    //
    // private static Node parseTerm(ParserState state) {
    // Node node = parseFactor(state);
    // while (state.hasNext() && "AND".equals(state.peek())) {
    // state.next(); // consume AND
    // Node right = parseFactor(state);
    // node = new OperatorNode("AND", node, right);
    // }
    // return node;
    // }
    //
    // private static Node parseFactor(ParserState state) {
    // if (!state.hasNext())
    // return null;
    // Object token = state.next();
    // if (token instanceof SearchFilter) {
    // return new FilterNode((SearchFilter) token);
    // }
    // return null; // Unexpected token
    // }
    //
    // private static void toPrefix(Node n, List<Object> output) {
    // if (n == null)
    // return;
    //
    // if (n instanceof FilterNode) {
    // SearchFilter f = ((FilterNode) n).f;
    // output.add(new Object[] { f.getField(), f.getOperator(), f.getValue() });
    // } else if (n instanceof OperatorNode) {
    // OperatorNode op = (OperatorNode) n;
    // output.add("OR".equals(op.op) ? "|" : "&");
    // toPrefix(op.left, output);
    // toPrefix(op.right, output);
    // }
    // }
    //
    // private static class ParserState {
    // List<Object> tokens;
    // int index = 0;
    //
    // ParserState(List<Object> tokens) {
    // this.tokens = tokens;
    // }
    //
    // boolean hasNext() {
    // return index < tokens.size();
    // }
    //
    // Object peek() {
    // return tokens.get(index);
    // }
    //
    // Object next() {
    // return tokens.get(index++);
    // }
    // }
    //
    // private interface Node {
    // }
    //
    // private static class FilterNode implements Node {
    // SearchFilter f;
    //
    // FilterNode(SearchFilter f) {
    // this.f = f;
    // }
    // }
    //
    // private static class OperatorNode implements Node {
    // String op;
    // Node left, right;
    //
    // OperatorNode(String op, Node left, Node right) {
    // this.op = op;
    // this.left = left;
    // this.right = right;
    // }
    // }
}
