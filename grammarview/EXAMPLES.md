# Examples of how rules should be drawn

In the drawings, these are the symbol definitions:
The '=' sign indicates a straight horizontal line.
The '|' symbol indicates a straight vertical line.
The '+' symbol indicates where straight lines meet.
The ')' symbol indicates that the presence of a curve.

## Example 1

A normal rule and alternatives that easily fit on a page.

This grammar:

```YACC
   rule : term1 term2 term3
        | term4 term5 term6
        ;
```

Should be drawn like this:

```text
rule ===+=== term1 term2 term3
        |
        +=== term4 term5 term6
```

## Example 2

A rule with a very long name. The alternatives should begin on a new line, closer to the left of the page.

E.g., this YACC grammar:

```YACC
a_really_long_rule_name : term1 term2 term3
                        | term4 term5 term6
```

should be drawn like this.

```text
a_really_long_rule_name ==)
                          |
        )=================)
        |
        +=== term1 term2 term3
        |
        +=== term4 term5 term6
```

## Example 3

A rule with a very long name and a very long alternative.

The alternatives should begin on a new line, closer to the left
of the page.

No alternative should cross the vertical line that leads to the
different alternatives.

This YACC grammar:

```YACC
a_really_long_rule_name : term1 term2 term3 term4 term5 term6
                        | term7 term8 term9
                        ;
```

Should be drawn like this:

```text
a_really_long_rule_name ==)
                          |
        )=================)
        |
        +=== term1 term2 term3 term4 ==)
        |                              |
        |    )=========================)
        |    |
        |    )=== term5 term6
        |
        +=== term7 term8 term9
```

## Example 4

The following grammar is not being drawn correctly. The first and second alternatives are being wrapped correctly, but the second and third alternatives are not connected to the rule. I.e.,
the vertical lines at the beginnings of those alternatives should continue upwards until they connect to the vertical line in the previous alternative.

```YACC
else_if_statement: ELSE_IF LPAREN condition RPAREN LBRACE statements RBRACE
| ELSE_IF LPAREN condition RPAREN LBRACE statements RBRACE ELSE LBRACE statements RBRACE
| ELSE LBRACE statements RBRACE
;
```
