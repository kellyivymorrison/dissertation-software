%token PROGRAM
%token ID
%token NUMBER

%%

program
    : PROGRAM ID '{' stmt_list '}'
    ;

stmt_list
    : stmt_list stmt
    | /* empty */
    ;

stmt
    : ID '=' expr ';'
    ;

expr
    : expr '+' term
    | expr '-' term
    | term
    ;

term
    : NUMBER
    | ID
    ;

%%
