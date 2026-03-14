%token FORWARD
%token LEFT
%token RIGHT
%token TO
%token THE
%token COMPANY
%token HALT

%%

march
    : proceed steps stop
    ;

proceed
    : begin
    ;

begin
    : FORWARD
    ;

steps
    : step
    | step steps
    ;

step
    : embellishment direction
    ;

direction
    : LEFT
    | RIGHT
    ;

embellishment
    : TO THE
    |
    ;

stop
    : COMPANY HALT
    ;
%%
