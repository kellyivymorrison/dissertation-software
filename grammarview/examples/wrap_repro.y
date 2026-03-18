// Reproduction for the wrap connection issue.
%token ELSE_IF LPAREN RPAREN LBRACE RBRACE ELSE ITEM

%%

statements : statements ITEM | ITEM ;
condition : ITEM ;

else_if_statement: ELSE_IF LPAREN condition RPAREN LBRACE statements RBRACE
| ELSE_IF LPAREN condition RPAREN LBRACE statements RBRACE ELSE LBRACE statements RBRACE
| ELSE LBRACE statements RBRACE
;
%%
