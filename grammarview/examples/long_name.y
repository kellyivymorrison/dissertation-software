// A grammar with a very long rule name to test the new layout.
%token ITEM

%%
a_very_long_rule_name_that_should_wrap_around_the_page_because_it_is_long : ITEM | second_alternative | third_alternative ;

second_alternative : ITEM ITEM ;

third_alternative : ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ITEM ;
%%
