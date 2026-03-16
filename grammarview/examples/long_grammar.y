// A very long grammar to test multi-page support and cutoff prevention.
%token T1 T2 T3 T4 T5 T6 T7 T8 T9 T10 T11 T12 T13 T14 T15 T16 T17 T18 T19 T20

%%

start_rule : rule1 rule2 rule3 rule4 rule5 rule6 rule7 rule8 rule9 rule10 ;

rule1 : T1 T2 T3 T4 T5 T6 T7 T8 T9 T10 T11 T12 T13 T14 T15 T16 T17 T18 T19 T20 ;
rule2 : T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 | T12 | T13 | T14 | T15 | T16 | T17 | T18 | T19 | T20 ;
rule3 : rule1 | rule2 ;
rule4 : T1 T2 T3 T4 T5 T6 T7 T8 T9 T10 T11 T12 T13 T14 T15 T16 T17 T18 T19 T20 T1 T2 T3 T4 T5 T6 T7 T8 T9 T10 ;
rule5 : T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 | T12 | T13 | T14 | T15 | T16 | T17 | T18 | T19 | T20 | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 ;
rule6 : T1 ;
rule7 : T2 ;
rule8 : T3 ;
rule9 : T4 ;
rule10 : T5 ;
rule11 : T6 ;
rule12 : T7 ;
rule13 : T8 ;
rule14 : T9 ;
rule15 : T10 ;
rule16 : T11 ;
rule17 : T12 ;
rule18 : T13 ;
rule19 : T14 ;
rule20 : T15 ;
rule21 : T16 ;
rule22 : T17 ;
rule23 : T18 ;
rule24 : T19 ;
rule25 : T20 ;
rule26 : T1 ;
rule27 : T2 ;
rule28 : T3 ;
rule29 : T4 ;
rule30 : T5 ;

%%
